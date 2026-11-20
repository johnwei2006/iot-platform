#ifndef UPGRADE_FLAG_MANAGER_H
#define UPGRADE_FLAG_MANAGER_H

#include <stm32f4xx.h>
#include <stm32f4xx_flash.h>
#include <stddef.h>

// 升级状态枚举
typedef enum {
    UPGRADE_STATUS_IDLE       = 0x00,    // 空闲（无升级）
    UPGRADE_STATUS_DOWNLOADED = 0x01,    // 固件下载已完成，等待升级
    UPGRADE_STATUS_READY      = 0x02,    // bootloader已升级完成，准备重启应用
    UPGRADE_STATUS_FAILED     = 0x03     // 升级失败
} UpgradeStatus;

typedef enum {
    OTA_TYPE_FULL       = 0x00,    // 全量升级，默认升级类型
    OTA_TYPE_DIFF       = 0x01,    // 差分升级
} upgrade_type;

typedef enum {
    APP_TYPE_BOOTLOADER       = 0x00,    // bootloader
    APP_TYPE_GATEWAY          = 0x01,    // gateway
} app_type;

// 升级参数结构体（参数区存储）
#pragma pack(1)
typedef struct {
    uint8_t upgrade_status;       // 升级状态（UpgradeStatus）
    uint8_t app_type;             // 应用类型: 0 - bootloader,   1 - gateway
    uint8_t upgrade_type;         // 升级类型: 0 - 全量升级,      1 - 差分升级
    uint8_t upgrade_magic_number; // 升级魔数
    uint32_t firmware_size;       // 固件大小
    uint32_t firmware_crc;        // 固件CRC校验值
    uint8_t  reserved[16];        // 预留空间
} upgrade_flag_t;

#pragma pack()

// 参数区操作函数声明
void upgrade_flag_read(upgrade_flag_t *flag);
void upgrade_flag_write(upgrade_flag_t *flag);
void upgrade_flag_erase(void);

#endif // UPGRADE_FLAG_MANAGER_H
