#include "service_gateway_auth.h"
#include "../transport/transport_message_queue.h"
#include "../transport_udp/transport_message_cache.h"
#include "../config/config_manager.h"
#include "../service_manager/service_manager_udp.h"
#include "../transport_udp/transport_udp_message.h"
#include "../lte/lte_app.h"
#include "../utils/utils.h"
#include <rtthread.h>
#include <string.h>

/**
 * 网关认证服务：
 * 1. 连接建立后发送认证请求（gateway_id + 预共享密钥，token 全 0）
 * 2. 收到认证响应后保存 10 字节 token，认证成功才触发时间同步等后续服务
 * 3. 未认证或认证失败时由重试线程定期重发；断线重连必须重新认证
 */

#define AUTH_RETRY_INTERVAL_MS      10000   // 重试间隔
#define AUTH_MAX_RETRY              3       // 单次连接内最大重试次数

static struct
{
    uint8_t     token[GATEWAY_AUTH_TOKEN_LENGTH];
    rt_bool_t   authenticated;
    uint8_t     retry_count;
} auth_state = {
    .authenticated = RT_FALSE,
    .retry_count = 0,
};

static rt_mutex_t auth_state_mutex = RT_NULL;
static rt_timer_t auth_retry_timer = RT_NULL;

static void auth_state_lock(void);
static void auth_state_unlock(void);

static void auth_state_lock(void)
{
    if (auth_state_mutex != RT_NULL) {
        rt_mutex_take(auth_state_mutex, RT_WAITING_FOREVER);
    }
}

static void auth_state_unlock(void)
{
    if (auth_state_mutex != RT_NULL) {
        rt_mutex_release(auth_state_mutex);
    }
}

/**
 * 认证请求服务：读取 flash 中的预共享密钥，构造认证请求报文并入上行队列。
 */
rt_err_t service_gateway_auth_request_execute(void* p_data, void* business_data)
{
    config_gateway_serial_t gateway_config;
    flash_status status = read_config_gateway_serial(&gateway_config);
    if (status != FLASH_OK) {
        rt_kprintf("Error: gateway auth request failed to read config, status: %d\n", status);
        return -RT_ERROR;
    }
    size_t secret_len = rt_strlen((const char *)gateway_config.auth_secret);
    if (secret_len == 0) {
        rt_kprintf("Error: gateway auth secret is empty, use console command #ST GATEWAY_SECRET <secret> to set it\n");
        return -RT_ERROR;
    }
    if (secret_len > AUTH_SECRET_MAX_LENGTH) {
        rt_kprintf("Error: gateway auth secret too long: %d\n", secret_len);
        return -RT_ERROR;
    }

    transport_udp_message_t* gateway_uplink_message = (transport_udp_message_t*)rt_malloc(sizeof(transport_udp_message_t));
    if (gateway_uplink_message == RT_NULL) {
        rt_kprintf("Error: memory allocate gateway auth request message failed\n");
        return RT_ENOMEM;
    }
    rt_memset(gateway_uplink_message, 0, sizeof(transport_udp_message_t));

    gateway_uplink_message->transport_udp_header    = (transport_udp_header_t*)rt_malloc(sizeof(transport_udp_header_t));
    gateway_uplink_message->transport_udp_data      = (transport_udp_data_t*)rt_malloc(sizeof(transport_udp_data_t));
    gateway_uplink_message->service_data            = (service_data_t*)rt_malloc(sizeof(service_data_t));
    if (gateway_uplink_message->transport_udp_header == RT_NULL ||
        gateway_uplink_message->transport_udp_data == RT_NULL ||
        gateway_uplink_message->service_data == RT_NULL) {
        rt_kprintf("Error: memory allocate gateway auth request fields failed\n");
        free_transport_udp_message(gateway_uplink_message);
        return RT_ENOMEM;
    }
    rt_memset(gateway_uplink_message->transport_udp_header, 0, sizeof(transport_udp_header_t));
    rt_memset(gateway_uplink_message->transport_udp_data, 0, sizeof(transport_udp_data_t));
    rt_memset(gateway_uplink_message->service_data, 0, sizeof(service_data_t));

    gateway_uplink_message->transport_udp_data->event_id                = generate_rand_short();
    gateway_uplink_message->transport_udp_data->service_type            = SERVICE_TYPE_GATEWAY_AUTH_REQUEST;
    gateway_uplink_message->transport_udp_data->service_version         = SERVICE_VERSION_1;
    gateway_uplink_message->transport_udp_data->service_data_encoding   = SERVICE_DATA_ENCODING_BINARY;
    // token 保持全 0，认证成功后由 cloud_uplink_process 填充有效 token

    // business_data_payload = 预共享密钥（ASCII）
    gateway_uplink_message->service_data->business_data_payload_length = (uint16_t)secret_len;
    gateway_uplink_message->service_data->business_data_payload = (uint8_t*)rt_malloc(secret_len);
    if (gateway_uplink_message->service_data->business_data_payload == RT_NULL) {
        rt_kprintf("Error: memory allocate gateway auth secret payload failed\n");
        free_transport_udp_message(gateway_uplink_message);
        return RT_ENOMEM;
    }
    rt_memcpy(gateway_uplink_message->service_data->business_data_payload, gateway_config.auth_secret, secret_len);
    gateway_uplink_message->service_data->business_data = RT_NULL;

    rt_kprintf("Gateway auth request sent, event_id: %d, secret_len: %d\n",
        gateway_uplink_message->transport_udp_data->event_id, secret_len);
    rt_err_t result = put_gateway_uplink_message(&gateway_uplink_message);
    if (result != RT_EOK) {
        rt_kprintf("Warning: put_gateway_uplink_message failed, free memory\n");
        free_transport_udp_message(gateway_uplink_message);
        return result;
    }
    return RT_EOK;
}

/**
 * 认证响应服务：保存 token，成功后触发时间同步等后续服务。
 */
rt_err_t service_gateway_auth_response_execute(void* p_data, void* business_data)
{
    service_data_gateway_auth_response_t* auth_response = (service_data_gateway_auth_response_t*)business_data;
    if (auth_response == RT_NULL) {
        rt_kprintf("Error: gateway auth response business_data is NULL\n");
        return -RT_ERROR;
    }

    if (auth_response->result == 0) { // AUTH_RESULT_SUCCESS
        auth_state_lock();
        rt_memcpy(auth_state.token, auth_response->token, GATEWAY_AUTH_TOKEN_LENGTH);
        auth_state.authenticated = RT_TRUE;
        auth_state.retry_count = 0;
        auth_state_unlock();
        rt_kprintf("Gateway auth success, token received\n");

        // 认证通过后触发时间同步
        service_t* service = query_service_by_type(SERVICE_TYPE_TIME_SYNC_REQUEST);
        if (service != RT_NULL) {
            service->service_execute(RT_NULL, RT_NULL);
        }
    } else if (auth_response->result == 1) {
        rt_kprintf("Error: gateway auth failed, device not registered (UNKNOWN_DEVICE)\n");
    } else if (auth_response->result == 2) {
        rt_kprintf("Error: gateway auth failed, invalid secret (INVALID_SECRET)\n");
    } else {
        rt_kprintf("Error: gateway auth failed, result code: %d\n", auth_response->result);
    }
    return RT_EOK;
}

/** 重置认证状态（断线重连/连接建立时调用） */
void service_gateway_auth_invalidate(void)
{
    auth_state_lock();
    rt_memset(auth_state.token, 0, GATEWAY_AUTH_TOKEN_LENGTH);
    auth_state.authenticated = RT_FALSE;
    auth_state.retry_count = 0;
    auth_state_unlock();
    rt_kprintf("Gateway auth state invalidated, re-auth required\n");
}

rt_bool_t service_gateway_auth_is_authenticated(void)
{
    return auth_state.authenticated;
}

/** 将当前有效 token 填入报文；未认证时填全 0 */
void service_gateway_auth_fill_token(uint8_t token[GATEWAY_AUTH_TOKEN_LENGTH])
{
    auth_state_lock();
    if (auth_state.authenticated) {
        rt_memcpy(token, auth_state.token, GATEWAY_AUTH_TOKEN_LENGTH);
    } else {
        rt_memset(token, 0, GATEWAY_AUTH_TOKEN_LENGTH);
    }
    auth_state_unlock();
}

/** 校验下行报文 token */
rt_bool_t service_gateway_auth_validate_downlink_token(const uint8_t token[GATEWAY_AUTH_TOKEN_LENGTH])
{
    auth_state_lock();
    rt_bool_t valid = auth_state.authenticated &&
        (rt_memcmp(token, auth_state.token, GATEWAY_AUTH_TOKEN_LENGTH) == 0);
    auth_state_unlock();
    return valid;
}

/**
 * 认证重试定时器：连接后未认证时周期性重发认证请求。
 */
static void service_gateway_auth_retry(void* parameter)
{
    lte_info_t lte_info = {0};
    get_lte_info(&lte_info);
    if (lte_info.lte_state != UDP_STATE_CONNECTED) {
        return;
    }

    auth_state_lock();
    if (auth_state.authenticated) {
        auth_state.retry_count = 0;
        auth_state_unlock();
        return;
    }
    if (auth_state.retry_count >= AUTH_MAX_RETRY) {
        // 超过重试次数，等待下次断线重连后由 connect_callback 重置
        auth_state_unlock();
        return;
    }
    auth_state.retry_count++;
    auth_state_unlock();

    rt_kprintf("Info: gateway auth retry #%d\n", auth_state.retry_count);
    service_gateway_auth_request_execute(RT_NULL, RT_NULL);
}

service_gateway_auth_t* service_gateway_auth_request_create(void)
{
    service_gateway_auth_t* service = (service_gateway_auth_t*)rt_malloc(sizeof(service_gateway_auth_t));
    if (service == RT_NULL) {
        rt_kprintf("Error: Failed to allocate memory for gateway auth request service\n");
        return RT_NULL;
    }
    rt_memset(service, 0, sizeof(service_gateway_auth_t));
    service->base.service_type = SERVICE_TYPE_GATEWAY_AUTH_REQUEST;
    service->base.service_execute = service_gateway_auth_request_execute;
    return service;
}

service_gateway_auth_t* service_gateway_auth_response_create(void)
{
    service_gateway_auth_t* service = (service_gateway_auth_t*)rt_malloc(sizeof(service_gateway_auth_t));
    if (service == RT_NULL) {
        rt_kprintf("Error: Failed to allocate memory for gateway auth response service\n");
        return RT_NULL;
    }
    rt_memset(service, 0, sizeof(service_gateway_auth_t));
    service->base.service_type = SERVICE_TYPE_GATEWAY_AUTH_RESPONSE;
    service->base.service_execute = service_gateway_auth_response_execute;
    return service;
}

int service_gateway_auth_start(void)
{
    auth_state_mutex = rt_mutex_create("auth_state", RT_IPC_FLAG_FIFO);
    if (auth_state_mutex == RT_NULL) {
        rt_kprintf("Error: create auth_state mutex failed\n");
        return -RT_ERROR;
    }
    auth_retry_timer = rt_timer_create("auth_retry", service_gateway_auth_retry,
        RT_NULL, rt_tick_from_millisecond(AUTH_RETRY_INTERVAL_MS),
        RT_TIMER_FLAG_PERIODIC | RT_TIMER_FLAG_SOFT_TIMER);
    if (auth_retry_timer == RT_NULL) {
        rt_kprintf("Error: create auth retry timer failed\n");
        return -RT_ERROR;
    }
    rt_timer_start(auth_retry_timer);
    rt_kprintf("Gateway auth retry timer started, interval: %d ms\n", AUTH_RETRY_INTERVAL_MS);
    return RT_EOK;
}
INIT_APP_EXPORT(service_gateway_auth_start);
