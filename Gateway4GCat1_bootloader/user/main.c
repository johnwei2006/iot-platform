#include "main.h"
#include "stm32f4xx.h"
#include "stm32f4xx_rcc.h"
#include "stm32f4xx_syscfg.h"
#include "misc.h"
#include "flash_manager.h"
#include <string.h>
#include "upgrade_flag_manager.h"
#include <stdio.h>
#include "stm32f4xx_usart.h"
#include "stm32f4xx_gpio.h"
#include <stdarg.h>
#include "boot_logger.h"
#include "jump_to_app.h"
#include "upgrade.h"

static uint8_t StdPeriph_Init(void);
static void SystemClock_Config(void);

int main(void)
{
    USART1_Init();
    StdPeriph_Init();
    Boot_Printf("\r\n==== Bootloader Start ====\r\n");
    
    //1. read upgrade_flag
    upgrade_flag_t upgrade_flag;
    upgrade_flag_read(&upgrade_flag);
    Boot_Printf("reading upgrade_flag****************************************************************\r\n");
    Boot_Printf("upgrade type:  %d\r\n", upgrade_flag.upgrade_type);
    Boot_Printf("app type:      %d\r\n", upgrade_flag.app_type);
    Boot_Printf("firmware size: %d\r\n", upgrade_flag.firmware_size);
    Boot_Printf("firmware crc:  0x%08X\r\n", upgrade_flag.firmware_crc);
    Boot_Printf("upgrade status: %d\r\n", upgrade_flag.upgrade_status);

    if(upgrade_flag.app_type == APP_TYPE_GATEWAY)
    {
        Boot_Printf("upgrade app type: gateway\r\n");
    }
    else if(upgrade_flag.app_type == APP_TYPE_BOOTLOADER)
    {
        Boot_Printf("upgrade app type: bootloader, not support now\r\n");
        goto jump_app;
    }
    else
    {
        Boot_Printf("upgrade app type: unknown %d\r\n", upgrade_flag.app_type);
        goto jump_app;
    }

    if (upgrade_flag.upgrade_status == UPGRADE_STATUS_DOWNLOADED)
    {
        //2. verify firmware crc in Partition B
        uint32_t calc_crc = 0;       
        Boot_Printf("Upgrade detected, verifying firmware in Partition B (0x%08X)\n", BACKUP_APP_BASE_ADDRESS);        
        calc_crc = flash_calculate_crc(BACKUP_APP_BASE_ADDRESS, upgrade_flag.firmware_size);
        if (calc_crc == upgrade_flag.firmware_crc)// && Bootloader_CheckAppValid(BACKUP_APP_BASE_ADDRESS))
        {
            Boot_Printf("Firmware CRC: 0x%08X, Expected CRC: 0x%08X\n", calc_crc, upgrade_flag.firmware_crc);

            if(upgrade_flag.firmware_size > FLASH_APP_SIZE)
            {
                Boot_Printf("Firmware size exceeds Partition A size, upgrade failed!\n");
                upgrade_flag.upgrade_status = UPGRADE_STATUS_FAILED;
                upgrade_flag_write(&upgrade_flag);
                goto jump_app;
            }
            //3. perform upgrade based on upgrade_type
            if(upgrade_flag.upgrade_type == OTA_TYPE_FULL)
            {
                //3.1. full upgrade
                Boot_Printf("Start full upgrade from 0x%08X to 0x%08X\n", BACKUP_APP_BASE_ADDRESS, APP_BASE_ADDRESS);
                if(upgrade_flag.firmware_size <= 64 * 1024)
                {
                    Boot_Printf("Firmware size is less than 64KB, ignore upgrade...\n");
                    goto jump_app;
                }
                full_upgrade(&upgrade_flag);
            }
            else if(upgrade_flag.upgrade_type == OTA_TYPE_DIFF)
            {
                //3.2. differential upgrade
                Boot_Printf("Start differential upgrade from 0x%08X to 0x%08X\n", BACKUP_APP_BASE_ADDRESS, APP_BASE_ADDRESS);
                if(upgrade_flag.firmware_size > 100 * 1024)
                {
                    Boot_Printf("Firmware size is greater than 100KB, ignore upgrade...\n");
                    goto jump_app;
                }                
                diff_upgrade(&upgrade_flag);    
            }
            else
            {
                Boot_Printf("Unknown upgrade type: %d\n", upgrade_flag.upgrade_type);
                upgrade_flag.upgrade_status = UPGRADE_STATUS_FAILED;
                upgrade_flag_write(&upgrade_flag);
                goto jump_app;
            }
        } else {
            // CRC verification failed
            Boot_Printf("CRC verification failed!\n");
            Boot_Printf("Firmware CRC: 0x%08X, Expected CRC: 0x%08X\n", calc_crc, upgrade_flag.firmware_crc);
            upgrade_flag.upgrade_status = UPGRADE_STATUS_FAILED;
            upgrade_flag_write(&upgrade_flag);
        }
    } 
jump_app:
    //4. jump to Partition A
    Boot_Printf("Jump to Partition A: 0x%08X\r\n", APP_BASE_ADDRESS);   
    Bootloader_DebugPrintAppHeader(APP_BASE_ADDRESS);
    Bootloader_JumpToApp(APP_BASE_ADDRESS);
    return 0;
}

static uint8_t StdPeriph_Init(void)
{
    SystemClock_Config();
    NVIC_PriorityGroupConfig(NVIC_PriorityGroup_4);
    SysTick_Config(SystemCoreClock / 1000);
    USART1_Init();
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_PWR, ENABLE);
    return 0;
}

static void SystemClock_Config(void)
{
    ErrorStatus HSEStartUpStatus;
    
    RCC_DeInit();

    RCC_HSEConfig(RCC_HSE_ON);
    HSEStartUpStatus = RCC_WaitForHSEStartUp();
    if (HSEStartUpStatus == ERROR)
    {
        return;
    }

    RCC_HCLKConfig(RCC_SYSCLK_Div1);
    RCC_PCLK1Config(RCC_HCLK_Div4);
    RCC_PCLK2Config(RCC_HCLK_Div2);

    RCC_PLLConfig(RCC_PLLSource_HSE, 8, 336, 2, 7);

    RCC_PLLCmd(ENABLE);
    while (RCC_GetFlagStatus(RCC_FLAG_PLLRDY) == RESET);

    FLASH_Unlock();
    FLASH_SetLatency(FLASH_Latency_5);
    FLASH_Lock();

    RCC_SYSCLKConfig(RCC_SYSCLKSource_PLLCLK);
    while (RCC_GetSYSCLKSource() != 0x08);

    SystemCoreClockUpdate();
}