#ifndef FLASH_MANAGER_H
#define FLASH_MANAGER_H

#include <stm32f4xx.h>
#include <stm32f4xx_flash.h>
#include <stddef.h>

//FLASH 扇区的起始地址
#define ADDR_FLASH_SECTOR_0     ((uint32_t)0x08000000) 	//扇区0起始地址, 16 Kbytes
#define ADDR_FLASH_SECTOR_1     ((uint32_t)0x08004000) 	//扇区1起始地址, 16 Kbytes
#define ADDR_FLASH_SECTOR_2     ((uint32_t)0x08008000) 	//扇区2起始地址, 16 Kbytes
#define ADDR_FLASH_SECTOR_3     ((uint32_t)0x0800C000) 	//扇区3起始地址, 16 Kbytes
#define ADDR_FLASH_SECTOR_4     ((uint32_t)0x08010000) 	//扇区4起始地址, 64 Kbytes
#define ADDR_FLASH_SECTOR_5     ((uint32_t)0x08020000) 	//扇区5起始地址, 128 Kbytes

#define ADDR_FLASH_SECTOR_6     ((uint32_t)0x08040000) 	//扇区6起始地址, 128 Kbytes
#define ADDR_FLASH_SECTOR_7     ((uint32_t)0x08060000) 	//扇区7起始地址, 128 Kbytes

#define ADDR_FLASH_SECTOR_8     ((uint32_t)0x08080000) 	//扇区8起始地址, 128 Kbytes

#define ADDR_FLASH_SECTOR_9     ((uint32_t)0x080A0000) 	//扇区9起始地址, 128 Kbytes
#define ADDR_FLASH_SECTOR_10    ((uint32_t)0x080C0000) 	//扇区10起始地址,128 Kbytes

#define ADDR_FLASH_SECTOR_11    ((uint32_t)0x080E0000) 	//扇区11起始地址,128 Kbytes

// Bootloader占用 0x08000000-0x0800FFFF (Sector 0-1, 32KB)
#define BOOTLOADER_ADDRESS         ADDR_FLASH_SECTOR_0
// Bootloader备份存储地址: 0x08008000 (Sector 2-3, 32KB)
#define BACKUP_BOOTLOADER_ADDRESS  ADDR_FLASH_SECTOR_2
// 应用运行地址: 0x08010000 (Sector 4-6, 256KB)
#define APP_BASE_ADDRESS           ADDR_FLASH_SECTOR_4

// 升级包备份存储地址: 0x08060000 (Sector 7-8, 256KB)
#define BACKUP_APP_BASE_ADDRESS    ADDR_FLASH_SECTOR_7

// LoRa节点固件存储地址: 0x080A0000 (Sector 9-10, 256KB)
#define LORA_APP_BASE_ADDRESS      ADDR_FLASH_SECTOR_9
                          
// 配置参数存储地址: 0x080E0000 (Sector 11)
#define CONFIG_PARAM_ADDRESS       ADDR_FLASH_SECTOR_11
                         
// 升级标志存储地址: 0x080FF000 (Sector 11 末尾 4KB)
#define UPGRADE_FLAG_ADDRESS       ((uint32_t)0x080FF000)

#define FLASH_APP_SIZE (BACKUP_APP_BASE_ADDRESS - APP_BASE_ADDRESS - 4)

#define FLASH_APPBACKUP_SIZE (CONFIG_PARAM_ADDRESS - BACKUP_APP_BASE_ADDRESS - 4)

#define IAP_DIFF_OFFSET_ADDRESS (20 * 1024)   //0x5000

// 自定义状态枚举，替代HAL_StatusTypeDef
typedef enum {
    FLASH_OK       = 0x00U,  // 操作成功
    FLASH_ERROR    = 0x01U,  // 操作失败
    //FLASH_BUSY     = 0x02U,  // 总线忙
    FLASH_TIMEOUT  = 0x03U   // 超时
} FLASH_StatusTypeDef;

// Flash操作函数声明（返回值替换为自定义的FLASH_StatusTypeDef）
uint32_t flash_calculate_crc(uint32_t start_addr, uint32_t length);
FLASH_StatusTypeDef flash_erase_sector(uint32_t addr);
FLASH_StatusTypeDef flash_write_data(uint32_t addr, uint8_t *data, uint32_t len);
FLASH_StatusTypeDef flash_read_data(uint32_t addr, uint8_t *data, uint32_t len);
FLASH_StatusTypeDef flash_copy_data(uint32_t src_addr, uint32_t dst_addr, uint32_t len);
FLASH_StatusTypeDef flash_erase_range(uint32_t start_addr, uint32_t size);
#endif