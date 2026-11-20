#include "flash_manager.h"
#include <stm32f4xx_flash.h>

// 获取Flash扇区号（标准库版本，逻辑不变）
static uint32_t flash_get_sector(uint32_t addr)
{
    uint32_t sector = 0;
    
    if ((addr >= 0x08000000) && (addr < 0x08004000)) sector = FLASH_Sector_0;
    else if ((addr >= 0x08004000) && (addr < 0x08008000)) sector = FLASH_Sector_1;
    else if ((addr >= 0x08008000) && (addr < 0x0800C000)) sector = FLASH_Sector_2;
    else if ((addr >= 0x0800C000) && (addr < 0x08010000)) sector = FLASH_Sector_3;
    else if ((addr >= 0x08010000) && (addr < 0x08020000)) sector = FLASH_Sector_4;
    else if ((addr >= 0x08020000) && (addr < 0x08040000)) sector = FLASH_Sector_5;
    else if ((addr >= 0x08040000) && (addr < 0x08060000)) sector = FLASH_Sector_6;
    else if ((addr >= 0x08060000) && (addr < 0x08080000)) sector = FLASH_Sector_7;
    else if ((addr >= 0x08080000) && (addr < 0x080A0000)) sector = FLASH_Sector_8;
    else if ((addr >= 0x080A0000) && (addr < 0x080C0000)) sector = FLASH_Sector_9;
    else if ((addr >= 0x080C0000) && (addr < 0x080E0000)) sector = FLASH_Sector_10;
    else if ((addr >= 0x080E0000) && (addr < 0x08100000)) sector = FLASH_Sector_11;
    
    return sector;
}

// 计算指定地址范围的CRC32（逻辑不变）
uint32_t flash_calculate_crc(uint32_t start_addr, uint32_t length)
{
    uint32_t crc = 0xFFFFFFFF;
    uint32_t i;
    uint8_t *p = (uint8_t*)start_addr;
    
    for (i = 0; i < length; i++)
    {
        crc ^= (uint32_t)*p++;
        for (uint8_t j = 0; j < 8; j++)
        {
            if (crc & 0x01)
                crc = (crc >> 1) ^ 0xEDB88320;
            else
                crc = crc >> 1;
        }
    }
    return ~crc;
}

// 擦除指定地址所在的扇区（标准库实现）
FLASH_StatusTypeDef flash_erase_sector(uint32_t addr)
{
    FLASH_Status status = FLASH_COMPLETE;
    uint32_t sector = flash_get_sector(addr);
    
    // 解锁Flash（标准库解锁方式）
    FLASH_Unlock();
    // 清除所有Flash标志位
    FLASH_ClearFlag(FLASH_FLAG_EOP | FLASH_FLAG_OPERR | FLASH_FLAG_WRPERR | 
                    FLASH_FLAG_PGAERR | FLASH_FLAG_PGPERR | FLASH_FLAG_PGSERR);
    
    // 擦除扇区（标准库擦除函数）
    status = FLASH_EraseSector(sector, VoltageRange_3); // 3.0-3.6V
    
    // 锁定Flash
    FLASH_Lock();
    
    // 转换标准库状态码为自定义状态
    if (status == FLASH_COMPLETE)
        return FLASH_OK;
    else
        return FLASH_ERROR;
}

// 写入数据到Flash（标准库实现，优先按字写入以提升速度）
FLASH_StatusTypeDef flash_write_data(uint32_t addr, uint8_t *data, uint32_t len)
{
    FLASH_Status status = FLASH_COMPLETE;
    uint32_t i = 0;
    
    // 解锁Flash
    FLASH_Unlock();
    FLASH_ClearFlag(FLASH_FLAG_EOP | FLASH_FLAG_OPERR | FLASH_FLAG_WRPERR | 
                    FLASH_FLAG_PGAERR | FLASH_FLAG_PGPERR | FLASH_FLAG_PGSERR);
    
    // 先按32位字写入（对齐部分），大幅提升大块数据写入速度
    while ((i + 4) <= len && status == FLASH_COMPLETE)
    {
        uint32_t word = *(uint32_t*)(data + i);
        status = FLASH_ProgramWord(addr + i, word);
        i += 4;
    }
    
    // 剩余不足4字节的部分按字节写入
    while (i < len && status == FLASH_COMPLETE)
    {
        status = FLASH_ProgramByte(addr + i, data[i]);
        i++;
    }
    
    // 锁定Flash
    FLASH_Lock();
    
    // 转换状态码
    if (status == FLASH_COMPLETE)
        return FLASH_OK;
    else
        return FLASH_ERROR;
}

// 从Flash读取数据
FLASH_StatusTypeDef flash_read_data(uint32_t addr, uint8_t *data, uint32_t len)
{
    uint32_t i = 0;
    uint8_t *p = (uint8_t*)addr;
    
    while (i < len)
    {
        data[i++] = *p++;
    }
    return FLASH_OK;
}
FLASH_StatusTypeDef flash_copy_data(uint32_t src_addr, uint32_t dst_addr, uint32_t len)
{
    uint8_t buffer[4096];  // 4KB 缓冲区
    uint32_t copied = 0;
    
    while (copied < len)
    {
        uint32_t chunk_size = (len - copied > 4096) ? 4096 : (len - copied);
        
        // 读取源数据
        flash_read_data(src_addr + copied, buffer, chunk_size);
        
        // 写入目标地址
        if (flash_write_data(dst_addr + copied, buffer, chunk_size) != FLASH_OK)
        {
            return FLASH_ERROR;
        }
        
        copied += chunk_size;
    }
    
    return FLASH_OK;
}

FLASH_StatusTypeDef flash_erase_range(uint32_t start_addr, uint32_t size)
{
    uint32_t end_addr = start_addr + size;
    uint32_t sector_size;

    while (start_addr < end_addr)
    {
        uint32_t sector = flash_get_sector(start_addr);

        if (flash_erase_sector(start_addr) != FLASH_OK)
            return FLASH_ERROR;

        if (sector <= FLASH_Sector_3)
            sector_size = 16 * 1024;
        else if (sector == FLASH_Sector_4)
            sector_size = 64 * 1024;
        else
            sector_size = 128 * 1024;

        start_addr += sector_size;
    }

    return FLASH_OK;
}