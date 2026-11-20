#include "upgrade.h"
#include "flash_manager.h"
#include <string.h>
#include "upgrade_flag_manager.h"
#include <stdio.h>
#include <stdarg.h>
#include "boot_logger.h"
#include "bs_patch_public.h"
#include "bs_iap.h"

void full_upgrade(upgrade_flag_t *upgrade_flag){
    Boot_Printf("CRC verification passed, copying B -> A\n");
            
    Boot_Printf("Erasing Partition A (0x%08X)...\n", APP_BASE_ADDRESS);
    {
        uint32_t erase_sectors[] = {
         FLASH_Sector_4,
         FLASH_Sector_5,
         FLASH_Sector_6,
        };
        uint8_t i;
        FLASH_Status status;
                
        FLASH_Unlock();
        FLASH_ClearFlag(FLASH_FLAG_EOP | FLASH_FLAG_OPERR | FLASH_FLAG_WRPERR | 
                                FLASH_FLAG_PGAERR | FLASH_FLAG_PGPERR | FLASH_FLAG_PGSERR);
                
        for (i = 0; i < 3; i++)
        {
           Boot_Printf("  Erasing sector %d...\n", erase_sectors[i]);
           status = FLASH_EraseSector(erase_sectors[i], VoltageRange_3);
           if (status != FLASH_COMPLETE)
           {
              Boot_Printf("  Erase sector %d failed!\n", erase_sectors[i]);
              FLASH_Lock();
              upgrade_flag->upgrade_status = UPGRADE_STATUS_FAILED;
              upgrade_flag_write(upgrade_flag);
              return;
           }
        }
        FLASH_Lock();
        Boot_Printf("  Partition A erased.\n");
    }
            
    Boot_Printf("Copying firmware B -> A (size=%d bytes)...\n", upgrade_flag->firmware_size);
    {
        FLASH_StatusTypeDef copy_ret;
        copy_ret = flash_copy_data(BACKUP_APP_BASE_ADDRESS, APP_BASE_ADDRESS, upgrade_flag->firmware_size);
        if (copy_ret != FLASH_OK)
        {
          Boot_Printf("  Copy firmware failed!\n");
          upgrade_flag->upgrade_status = UPGRADE_STATUS_FAILED;
          upgrade_flag_write(upgrade_flag);
          return;
        }
     }
            
    Boot_Printf("Clearing upgrade flag...\n");
    upgrade_flag->upgrade_status = UPGRADE_STATUS_READY;
    upgrade_flag_write(upgrade_flag);
            
    Boot_Printf("Upgrade completed!\n");
}
void diff_upgrade(upgrade_flag_t *upgrade_flag){
    image_header_t ih_header;
    uint32_t application_image_length;
    uint32_t application_backup_address;
    uint32_t application_run_address;

    application_backup_address = BACKUP_APP_BASE_ADDRESS;
    application_run_address = APP_BASE_ADDRESS;

    if(BS_IMAGE_CHECK_INVALID == iap_image_check(application_backup_address, application_run_address, &ih_header))
    {
        Boot_Printf("Diff upgrade failed, image check failed!\n");
        upgrade_flag->upgrade_status = UPGRADE_STATUS_FAILED;
        upgrade_flag_write(upgrade_flag);
        return;
    }
      
    Boot_Printf("1. Diff upgrade passed, erasing diff partition...\n");
    flash_erase_range(BACKUP_APP_BASE_ADDRESS + IAP_DIFF_OFFSET_ADDRESS, FLASH_APPBACKUP_SIZE - IAP_DIFF_OFFSET_ADDRESS);
        
    uint32_t patch_size  = BigtoLittle32(ih_header.ih_header_size);    
    Boot_Printf("patch size %d \n", patch_size);        

    Boot_Printf("2. Diff upgrade passed, patching diff partition...\n");
    application_image_length = bs_iap_patch((uint8_t *)application_run_address, BigtoLittle32(ih_header.ih_header_size), (uint8_t *)application_backup_address + IMAGE_HEADER_LEN, patch_size, IAP_DIFF_OFFSET_ADDRESS);              
      
    if(application_image_length == 0 || application_image_length > FLASH_APP_SIZE)
    {
        Boot_Printf("Diff upgrade failed, patching diff partition failed!\n");
        upgrade_flag->upgrade_status = UPGRADE_STATUS_FAILED;
        upgrade_flag_write(upgrade_flag);
        return;
    }
    
    Boot_Printf("3. Diff upgrade passed, patching diff partition completed.\n");
    uint32_t image_crc  = BigtoLittle32(ih_header.ih_header_dcrc);
    uint32_t flash_backup_image_crc = flash_calculate_crc(BACKUP_APP_BASE_ADDRESS + IAP_DIFF_OFFSET_ADDRESS, application_image_length);
    Boot_Printf("image_crc = %08x, flash_backup_image_crc = %08x\n", image_crc, flash_backup_image_crc);

    if(flash_backup_image_crc != image_crc)
    {
        Boot_Printf("Diff upgrade failed, patching diff partition failed!\n");
        upgrade_flag->upgrade_status = UPGRADE_STATUS_FAILED;
        upgrade_flag_write(upgrade_flag);
        return;
    }

    Boot_Printf("4. Diff upgrade passed, patching diff partition completed.\n");
    flash_erase_range(APP_BASE_ADDRESS, FLASH_APP_SIZE); 
    Boot_Printf("5. Diff upgrade passed, erasing app partition...\n");
    flash_write_data(APP_BASE_ADDRESS, (uint8_t *)application_backup_address + IAP_DIFF_OFFSET_ADDRESS, application_image_length);
    Boot_Printf("6. Diff upgrade passed, app partition copied.\n");
    flash_erase_range(BACKUP_APP_BASE_ADDRESS, FLASH_APPBACKUP_SIZE);   
    
    upgrade_flag->upgrade_status = UPGRADE_STATUS_READY;
    Boot_Printf("7. Diff upgrade passed, upgrade status set to ready.\n");
    upgrade_flag_write(upgrade_flag);
    
    Boot_Printf("8. Diff upgrade completed!\n");
}