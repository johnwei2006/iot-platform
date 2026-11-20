#ifndef __JUMP_TO_APP_H
#define __JUMP_TO_APP_H

#include "stm32f4xx.h"

/* ============== 地址配置（根据你的实际分区修改） ============== */
/* STM32F407 Flash 起始地址 */
#define FLASH_BASE_ADDR         ((uint32_t)0x08000000)

/* Bootloader 占用大小：  64KB = 0x10000 */
#define BOOTLOADER_SIZE         ((uint32_t)(64 * 1024))

/* 应用程序（APP）起始地址 */
#define APPLICATION_ADDR        (FLASH_BASE_ADDR + BOOTLOADER_SIZE)

/* APP 栈顶地址的有效范围校验（防止跳转到非法地址） */
#define SRAM_BASE_ADDR          ((uint32_t)0x20000000)
#define SRAM_SIZE               ((uint32_t)(192 * 1024))   /* F407 共 192KB SRAM */
#define SRAM_END_ADDR           (SRAM_BASE_ADDR + SRAM_SIZE)

/* ============== 函数声明 ============== */
void Bootloader_DebugPrintAppHeader(uint32_t app_addr);
void Bootloader_JumpToApp(uint32_t app_addr);
uint8_t Bootloader_CheckAppValid(uint32_t app_addr);
void Bootloader_SystemReset(uint32_t app_addr);
void Bootloader_CheckResetAndJump(uint32_t app_addr);

#endif /* __JUMP_TO_APP_H */