#include "jump_to_app.h"
#include "boot_logger.h"
#include "stm32f4xx_rcc.h"

/**
 * @brief  关闭所有中断并清除挂起位，跳转前必须调用
 * @note   确保跳转后 APP 不会被 Bootloader 的残留中断打断
 */
static void Bootloader_DisableAllInterrupts(void)
{
    /* 1. 关闭全局中断 */
    __disable_irq();

    /* 2. 关闭所有外设中断（NVIC）
     *    STM32F407 共 82 个可屏蔽中断，需要 3 个寄存器（ICER0~ICER2）
     */
    NVIC->ICER[0] = 0xFFFFFFFF;
    NVIC->ICER[1] = 0xFFFFFFFF;
    NVIC->ICER[2] = 0xFFFFFFFF;   /* F407 用不到 3 个全 1，写 0xFFFFFFFF 也无害 */

    /* 3. 清除所有挂起的中断标志 */
    NVIC->ICPR[0] = 0xFFFFFFFF;
    NVIC->ICPR[1] = 0xFFFFFFFF;
    NVIC->ICPR[2] = 0xFFFFFFFF;

    /* 4. 关闭 SysTick 定时器（如果 Bootloader 用了 SysTick） */
    SysTick->CTRL = 0;
    SysTick->LOAD = 0;
    SysTick->VAL  = 0;

    /* 5. 清除所有 PendSV / SVC 等系统异常挂起位 */
    SCB->ICSR = SCB_ICSR_PENDSVCLR_Msk | SCB_ICSR_PENDSTCLR_Msk;
}

/**
 * @brief  检查应用程序是否有效
 * @param  app_addr : 应用程序起始地址
 * @retval 1 = 有效，0 = 无效
 */
uint8_t Bootloader_CheckAppValid(uint32_t app_addr)
{
    uint32_t stack_top = *((__IO uint32_t *)app_addr);

    /* 校验栈顶地址是否落在合法的 SRAM 范围内 */
    if ((stack_top >= SRAM_BASE_ADDR) && (stack_top <= SRAM_END_ADDR))
    {
        return 1;
    }
    return 0;
}

/**
 * @brief  跳转到应用程序入口执行
 * @param  app_addr : 应用程序起始地址
 * @note   此函数成功执行后不会返回
 */
void Bootloader_JumpToApp(uint32_t app_addr)
{
    typedef void (*pFunction)(void);
    pFunction app_reset_handler;
    uint32_t reset_handler_addr;

    Boot_Printf("[JUMP] Checking app validity...\r\n");
    if (Bootloader_CheckAppValid(app_addr) == 0)
    {
        Boot_Printf("[JUMP] App invalid!\r\n");
        return;
    }
    Boot_Printf("[JUMP] App valid\r\n");

    app_reset_handler = (pFunction)(*((__IO uint32_t *)(app_addr + 4)));
    reset_handler_addr = (uint32_t)app_reset_handler;

    Boot_Printf("[JUMP] VTOR=0x%08X MSP=0x%08X PC=0x%08X\r\n",
                (unsigned int)app_addr,
                (unsigned int)*((__IO uint32_t *)app_addr),
                (unsigned int)reset_handler_addr);
    USART1_WaitTxComplete();

    Bootloader_DisableAllInterrupts();

    /* 配置 FLASH：关闭缓存，设置等待周期 */
    FLASH->ACR = FLASH_ACR_LATENCY_5WS | FLASH_ACR_PRFTEN;

    /* 设置向量表和栈指针 */
    SCB->VTOR = app_addr;
    __set_MSP(*((__IO uint32_t *)app_addr));
    __set_CONTROL(0);
    __DSB();
    __ISB();

    app_reset_handler();

    while (1);
}

void Bootloader_DebugPrintAppHeader(uint32_t app_addr)
{
    uint32_t i;
    Boot_Printf("=== APP Header @ 0x%08X ===\r\n", (unsigned int)app_addr);
    // for (i = 0; i < 8; i++)
    // {
    //     uint32_t val = *((__IO uint32_t *)(app_addr + i * 4));
    //     Boot_Printf("  0x%08X: 0x%08X\r\n", (unsigned int)(app_addr + i * 4), (unsigned int)val);
    // }
    
    /* 关键检查 */
    uint32_t stack_top = *((__IO uint32_t *)app_addr);
    uint32_t reset_vec = *((__IO uint32_t *)(app_addr + 4));
    Boot_Printf("Stack Top: 0x%08X (should be in 0x20000000 ~ 0x20030000)\r\n", (unsigned int)stack_top);
    Boot_Printf("Reset Vec: 0x%08X (should be in 0x08010000 ~ 0x080FFFFF)\r\n", (unsigned int)reset_vec);
}

static void Bootloader_JumpMinimal(uint32_t app_addr)
{
    __disable_irq();

    /* 完整复位 RCC 到 HSI 默认状态 */
    RCC_DeInit();

    /* 关闭所有外设时钟 */
    RCC->AHB1ENR = 0;
    RCC->AHB2ENR = 0;
    RCC->AHB3ENR = 0;
    RCC->APB1ENR = 0;
    RCC->APB2ENR = 0;

    /* 关闭 SysTick */
    SysTick->CTRL = 0;
    SysTick->LOAD = 0;
    SysTick->VAL = 0;

    /* 清除所有 NVIC 中断 */
    for (int i = 0; i < 8; i++) {
        NVIC->ICER[i] = 0xFFFFFFFF;
        NVIC->ICPR[i] = 0xFFFFFFFF;
    }

    /* 复位 FLASH 配置 */
    FLASH->ACR = 0;

    /* 禁用 MPU */
    MPU->CTRL = 0;

    /* 设置向量表 */
    SCB->VTOR = app_addr;

    /* 设置 MSP */
    __set_MSP(*((__IO uint32_t *)app_addr));
    __set_CONTROL(0);

    __DSB();
    __ISB();

    /* 使用函数指针跳转 */
    ((void (*)(void))(*((__IO uint32_t *)(app_addr + 4))))();

    while (1);
}

void Bootloader_SystemReset(uint32_t app_addr)
{
    Boot_Printf("SP=0x%08X, PC=0x%08X\r\n", 
                (unsigned int)*((__IO uint32_t *)app_addr),
                (unsigned int)*((__IO uint32_t *)(app_addr + 4)));
    Boot_Printf("Performing watchdog reset...\r\n");

    __disable_irq();

    /* 确保 LSI 已开启 */
    RCC->CSR |= RCC_CSR_LSION;
    while ((RCC->CSR & RCC_CSR_LSIRDY) == 0);

    /* 使能 IWDG 写访问 */
    IWDG->KR = 0x5555;
    /* 预分频器 = 4, LSI=32kHz → IWDG时钟=8kHz */
    IWDG->PR = 0;
    /* 重载值 = 1 → 复位时间 ≈ 125μs */
    IWDG->RLR = 1;
    /* 等待寄存器更新完成 */
    while (IWDG->SR & IWDG_SR_PVU);
    while (IWDG->SR & IWDG_SR_RVU);
    /* 启动看门狗 */
    IWDG->KR = 0xCCCC;

    /* 等待复位 */
    while (1);
}

void Bootloader_CheckResetAndJump(uint32_t app_addr)
{
    /* 检查是否为软件复位或看门狗复位 */
    if ((RCC_GetFlagStatus(RCC_FLAG_SFTRST) != RESET) ||
        (RCC_GetFlagStatus(RCC_FLAG_IWDGRST) != RESET))
    {
        /* 清除所有复位标志 */
        RCC->CSR |= RCC_CSR_RMVF;
        if (Bootloader_CheckAppValid(app_addr))
        {
            Bootloader_JumpMinimal(app_addr);
        }
    }
}