#include "boot_logger.h"
#include "stm32f4xx.h"
#include <string.h>
#include <stdio.h>
#include "stm32f4xx_usart.h"
#include "stm32f4xx_gpio.h"
#include <stdarg.h>

void USART1_Init(void)
{
    // 使能时钟
    RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOA, ENABLE);
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1, ENABLE);
    
    // 配置PA9为USART1_TX
    GPIO_InitTypeDef GPIO_InitStructure;
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_9;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF;
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_UP;
    GPIO_Init(GPIOA, &GPIO_InitStructure);
    
    GPIO_PinAFConfig(GPIOA, GPIO_PinSource9, GPIO_AF_USART1);
    
    // 配置USART1
    USART_InitTypeDef USART_InitStructure;
    USART_InitStructure.USART_BaudRate = 115200;
    USART_InitStructure.USART_WordLength = USART_WordLength_8b;
    USART_InitStructure.USART_StopBits = USART_StopBits_1;
    USART_InitStructure.USART_Parity = USART_Parity_No;
    USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
    USART_InitStructure.USART_Mode = USART_Mode_Tx | USART_Mode_Rx;
    USART_Init(USART1, &USART_InitStructure);
    
    USART_Cmd(USART1, ENABLE);
}
// 串口发送单个字符
static void UART_PutChar(char ch)
{
    // 等待发送完成
    while (USART_GetFlagStatus(USART1, USART_FLAG_TXE) == RESET);
    USART_SendData(USART1, (uint16_t)ch);
}

// 串口发送字符串
static void UART_PutString(const char *str)
{
    while (*str)
    {
        UART_PutChar(*str++);
    }
}

// 格式化打印函数
void Boot_Printf(char *fmt, ...)
{
    va_list ap;
    char buf[256];
    int i, len;
    
    va_start(ap, fmt);
    len = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    
    // 发送格式化后的字符串
    for (i = 0; i < len; i++)
    {
        UART_PutChar(buf[i]);
    }
}

// 等待串口传输完全完成
void USART1_WaitTxComplete(void)
{
    while (USART_GetFlagStatus(USART1, USART_FLAG_TC) == RESET);
}