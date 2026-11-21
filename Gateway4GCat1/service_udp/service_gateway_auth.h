#ifndef SERVICE_GATEWAY_AUTH_H_
#define SERVICE_GATEWAY_AUTH_H_

#include <rtthread.h>
#include "../service_manager/abstract_service.h"
#include "../transport_udp/transport_udp_service_message.h"

typedef struct
{
    service_t base;
} service_gateway_auth_t;

/** 认证请求服务（上行，gateway_id + 预共享密钥，token 全 0） */
service_gateway_auth_t* service_gateway_auth_request_create(void);
rt_err_t service_gateway_auth_request_execute(void* p_data, void* business_data);

/** 认证响应服务（下行，result + token） */
service_gateway_auth_t* service_gateway_auth_response_create(void);
rt_err_t service_gateway_auth_response_execute(void* p_data, void* business_data);

/** 重置认证状态（断线重连/连接建立时调用），随后应发起认证请求 */
void service_gateway_auth_invalidate(void);

/** 是否已通过认证 */
rt_bool_t service_gateway_auth_is_authenticated(void);

/** 将当前有效 token 填入报文；未认证时填全 0 */
void service_gateway_auth_fill_token(uint8_t token[GATEWAY_AUTH_TOKEN_LENGTH]);

/** 校验下行报文 token（认证响应报文不校验，由平台/网关协商放行） */
rt_bool_t service_gateway_auth_validate_downlink_token(const uint8_t token[GATEWAY_AUTH_TOKEN_LENGTH]);

/** 启动认证重试线程（INIT_APP_EXPORT 自动调用） */
int service_gateway_auth_start(void);

#endif /*SERVICE_GATEWAY_AUTH_H_*/
