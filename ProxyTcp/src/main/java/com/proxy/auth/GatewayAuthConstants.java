package com.proxy.auth;

/**
 * 网关认证协议常量（见 docs/protocol-gateway-auth.md）。
 * 命令字取协议未用值，与固件 transport_udp_message.h 保持一致。
 */
public class GatewayAuthConstants {

	/** 上行：网关认证请求（gateway_id + 预共享密钥，token 字段全 0），在 ProxyTcp 终结 */
	public static final int SERVICE_TYPE_GATEWAY_AUTH_REQUEST 	= 105;	// 0x69

	/** 下行：平台认证响应（result + token） */
	public static final int SERVICE_TYPE_GATEWAY_AUTH_RESPONSE 	= 106;	// 0x6A

	/** 认证结果码（认证响应 serviceData 首字节） */
	public static final int AUTH_RESULT_SUCCESS 				= 0;
	public static final int AUTH_RESULT_UNKNOWN_DEVICE 			= 1;
	public static final int AUTH_RESULT_INVALID_SECRET 			= 2;
	public static final int AUTH_RESULT_ERROR 					= 3;

	/** 预共享密钥最大长度（字节） */
	public static final int AUTH_SECRET_MAX_LENGTH 				= 32;

	/** 错误 ACK（serviceType=0，serviceData 1 字节错误码）错误码 */
	public static final int NAK_CODE_TOKEN_INVALID 				= 1;
	public static final int NAK_CODE_NOT_AUTHENTICATED 			= 2;

	/** 重连记录类型 */
	public static final String CONNECT_TYPE_FIRST 				= "FIRST";
	public static final String CONNECT_TYPE_RECONNECT 			= "RECONNECT";
}
