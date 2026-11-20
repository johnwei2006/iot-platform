#include "upgrade_flag_manager.h"
#include <stddef.h>
#include <string.h>
#include "flash_manager.h"

// 升级标志区位于sector11末尾0x080FF000处，与网关配置区共享sector11（0x080E0000-0x08100000）
// 写入升级标志时必须先备份网关配置区内容，以保护网关配置数据不被破坏
#define SECTOR11_BASE             0x080E0000
#define GATEWAY_CONFIG_SIZE       (16 * 1024)

// 网关配置区备份缓冲区，用于写入升级标志时保护网关配置区数据
static uint8_t gateway_config_backup[GATEWAY_CONFIG_SIZE];

// 读取升级标志区数据
void upgrade_flag_read(upgrade_flag_t *flag)
{
    if (flag == NULL) return;
    // 从升级标志存储区读取升级参数结构体
    flash_read_data(UPGRADE_FLAG_ADDRESS, (uint8_t*)flag, sizeof(upgrade_flag_t));
}

// 写入升级标志区数据（备份网关配置区后擦除扇区再写入，保护网关配置数据）
void upgrade_flag_write(upgrade_flag_t *flag)
{
    if (flag == NULL) return;
    
    FLASH_Status status;
    uint32_t i;
    
    // 1. 读取网关配置区全部内容到备份缓冲区
    Boot_Printf("Reading gateway config from sector 11...\n");
    uint32_t *src = (uint32_t*)SECTOR11_BASE;
    for (i = 0; i < GATEWAY_CONFIG_SIZE / 4; i++)
    {
        gateway_config_backup[i * 4] = ((uint8_t*)&src[i])[0];
        gateway_config_backup[i * 4 + 1] = ((uint8_t*)&src[i])[1];
        gateway_config_backup[i * 4 + 2] = ((uint8_t*)&src[i])[2];
        gateway_config_backup[i * 4 + 3] = ((uint8_t*)&src[i])[3];
    }
    
    // 2. 擦除整个sector11（网关配置区+升级标志区）
    Boot_Printf("Erasing sector 11...\n");
    FLASH_Unlock();
    FLASH_ClearFlag(FLASH_FLAG_EOP | FLASH_FLAG_OPERR | FLASH_FLAG_WRPERR | 
                    FLASH_FLAG_PGAERR | FLASH_FLAG_PGPERR | FLASH_FLAG_PGSERR);
    status = FLASH_EraseSector(FLASH_Sector_11, VoltageRange_3);
    if (status != FLASH_COMPLETE)
    {
        FLASH_Lock();
        return;
    }
    
    // 3. 将网关配置数据写回（备份时保存了全部数据，因此全部写回）
    Boot_Printf("Writing gateway config to sector 11...\n");
    for (i = 0; i < GATEWAY_CONFIG_SIZE / 4; i++)
    {
        uint32_t word = gateway_config_backup[i * 4] | 
                       (gateway_config_backup[i * 4 + 1] << 8) |
                       (gateway_config_backup[i * 4 + 2] << 16) |
                       (gateway_config_backup[i * 4 + 3] << 24);
        status = FLASH_ProgramWord(SECTOR11_BASE + i * 4, word);
        if (status != FLASH_COMPLETE)
        {
            FLASH_Lock();
            return;
        }
    }
    
    // 4. 写入升级标志
    Boot_Printf("Writing upgrade flag to sector 11...\n");
    uint8_t *p = (uint8_t*)flag;
    for (i = 0; i < sizeof(upgrade_flag_t); i++)
    {
        status = FLASH_ProgramByte(UPGRADE_FLAG_ADDRESS + i, p[i]);
        if (status != FLASH_COMPLETE)
        {
            FLASH_Lock();
            return;
        }
    }
    
    FLASH_Lock();
}
// 擦除升级标志区
void upgrade_flag_erase(void)
{
    // 擦除升级标志区时同样需要保护网关配置区数据
    upgrade_flag_t empty_flag;
    memset(&empty_flag, 0xFF, sizeof(upgrade_flag_t));
    upgrade_flag_write(&empty_flag);
}