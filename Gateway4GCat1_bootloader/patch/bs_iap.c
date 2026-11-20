/*
 * Copyright (c) 2023-2024, KeBai Development Team
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Change Logs:
 * Date          Author     Notes
 * 2023-12-2     xhc       the first version
 * 2023-12-8     xhc       update iap patch use for image merge
 * 2024-3-8      xhc       modify flash rw pointer operation bug
 * 2024-3-11     xhc       clean code
 */
#include "bs_iap.h"
#include "bs_patch.h"
#include "bs_patch_internal.h"
#include "bs_patch_public.h"
#include "upgrade_flag_manager.h"

typedef  void (*pFunction)(void);

static pFunction jump_To_application;

static uint8_t s_buffer[IAP_BUFFER_SIZE] = {0};

static int application_check(uint32_t address)
{
    int res = -1;
    if (((*(__IO uint32_t*)address) & 0x2FFE0000 ) == 0x20000000)  //Check Stack Top Legality
    { 
        res = 0;
    }
    else
    {
        Boot_Printf("iap %08x error\r\n",address);
    }
    
    return res;
}

static void jump_app_address(uint32_t address)
{
    uint32_t jump_address;
    
    if(application_check(address) != 0)
    {
        Boot_Printf("iap jump 0x%08x error\r\n",address);
        return ;
    }
    __disable_irq();
   
    Boot_Printf("IAP jump to 0x%08x\r\n",address);
    jump_address = *(__IO uint32_t*) (address + 4);
    jump_To_application = (pFunction) jump_address;
    __set_MSP(*(__IO uint32_t*) address);
    jump_To_application();
}

void bs_iap_jump_to_bootloader(void)
{
    Boot_Printf("jump bootloader:%08x\r\n",BOOTLOADER_ADDRESS);
    jump_app_address(BOOTLOADER_ADDRESS);
}

void bs_iap_jump_to_application()
{
    uint32_t app_entry_addr = 0;
    upgrade_flag_t flag;
    upgrade_flag_read(&flag);

    if(flag.upgrade_type == OTA_TYPE_DIFF) {

        app_entry_addr = APP_BASE_ADDRESS;
        
    }else{

        app_entry_addr = APP_BASE_ADDRESS;
    }

    Boot_Printf("jump app:%08x\r\n",app_entry_addr);

    jump_app_address(app_entry_addr);
}


int iap_image_check(uint32_t app_backup_address, uint32_t app_run_address, image_header_t *ih_header)
{
    memcpy(ih_header, (void *)app_backup_address, IMAGE_HEADER_LEN);

    uint32_t magic = BigtoLittle32(ih_header->ih_header_magic);
    
    if(magic == IH_HEADER_MAGIC)  /*header magmic check */
    {
        uint32_t crc  = BigtoLittle32(ih_header->ih_header_ocrc);
        uint32_t old_image_crc = crc32((uint8_t *)app_run_address, BigtoLittle32(ih_header->ih_header_size));
        Boot_Printf("ImageCheck:old_image_crc = %08x, crc = %08x\r\n",old_image_crc, crc);
        
        if (old_image_crc != crc) {
            
            Boot_Printf("application image crc calculate error!!!\r\n");

            return BS_IMAGE_CHECK_INVALID;
        }
            
        return BS_IMAGE_CHECK_VALID;
	}

    return BS_IMAGE_CHECK_INVALID;
}

int bs_iap_image_undiff(uint32_t size)
{
    uint16_t nowSize = 0;
    /*packet size calculate*/
    uint16_t pack = size / IAP_BUFFER_SIZE + ((size % IAP_BUFFER_SIZE) ? 1 : 0);
  
    if(size > BACKUP_APP_BASE_ADDRESS-APP_BASE_ADDRESS)
    {
        Boot_Printf("iap app size error!\r\n");
        return BS_IMAGE_LENGTH_ERROR;
    }
  
    if(application_check(BACKUP_APP_BASE_ADDRESS) != 0)
    {
        Boot_Printf("iap backup app error!\r\n");
        return BS_IMAGE_ADDRESS_ERROR;
    }
  
    Boot_Printf("iap start copy app\r\n");
    
    flash_erase_range(APP_BASE_ADDRESS, size);
    
    for(uint16_t i = 0;i < pack;i ++)
    {
        if(size / IAP_BUFFER_SIZE > 0) nowSize = IAP_BUFFER_SIZE;
        else nowSize = size % IAP_BUFFER_SIZE;
        size -= IAP_BUFFER_SIZE;
        Boot_Printf("iap cp %04u B,progress:%03u\r\n",nowSize,(i + 1) * 100 / pack);
        
        flash_read_data(BACKUP_APP_BASE_ADDRESS + i * IAP_BUFFER_SIZE, s_buffer, nowSize);  
       
        //EVAL_LED_RUN_OFF;
    
        flash_write_data(APP_BASE_ADDRESS + i * IAP_BUFFER_SIZE, s_buffer, nowSize);
      
        //EVAL_LED_RUN_ON;
    
    }
    
    if(application_check(APP_BASE_ADDRESS) != 0)
    {
        Boot_Printf(" iap app address error!!\r\n");
        return BS_IMAGE_ADDRESS_ERROR;
    }
    
    return BS_IAMGE_MERGE_SUCCESS;
}

int bs_iap_image_diff(void) {

    image_header_t ih_header;
    uint32_t application_image_length;
    uint32_t application_backup_address;
    uint32_t application_run_address;

    application_backup_address = BACKUP_APP_BASE_ADDRESS;
    application_run_address = APP_BASE_ADDRESS;

    if(BS_IMAGE_CHECK_INVALID == iap_image_check(application_backup_address, application_run_address, &ih_header)) return BS_IMAGE_CHECK_INVALID;
      
    flash_erase_range(BACKUP_APP_BASE_ADDRESS + IAP_DIFF_OFFSET_ADDRESS, FLASH_APPBACKUP_SIZE - IAP_DIFF_OFFSET_ADDRESS);
        
    uint32_t patch_size  = BigtoLittle32(ih_header.ih_header_size);    
    Boot_Printf("patch size %d \r\n", patch_size);        
      
    application_image_length = bs_iap_patch((uint8_t *)application_run_address, BigtoLittle32(ih_header.ih_header_size), (uint8_t *)application_backup_address + IMAGE_HEADER_LEN, patch_size, IAP_DIFF_OFFSET_ADDRESS);              
      
    if(application_image_length == 0 || application_image_length > FLASH_APP_SIZE) return BS_IMAGE_LENGTH_ERROR;
    
    uint32_t image_crc  = BigtoLittle32(ih_header.ih_header_dcrc);
    uint32_t flash_backup_image_crc = flash_calculate_crc(BACKUP_APP_BASE_ADDRESS + IAP_DIFF_OFFSET_ADDRESS, application_image_length);
    Boot_Printf("image_crc = %08x, flash_backup_image_crc = %08x\r\n", image_crc, flash_backup_image_crc);

    if(flash_backup_image_crc != image_crc) return BS_IMAGE_CRC_ERROR;

    flash_erase_range(APP_BASE_ADDRESS, FLASH_APP_SIZE); 
    flash_write_data(APP_BASE_ADDRESS, (uint8_t *)application_backup_address + IAP_DIFF_OFFSET_ADDRESS, application_image_length);
    flash_erase_range(BACKUP_APP_BASE_ADDRESS, FLASH_APPBACKUP_SIZE);   
    
    return BS_IAMGE_MERGE_SUCCESS;
}
/************************ (C) COPYRIGHT 2024 SHANDONG KEBAI.LTD *******************END OF FILE******/