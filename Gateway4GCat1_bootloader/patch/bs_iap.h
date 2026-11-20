#ifndef __IAP_H
#define __IAP_H
#include <stdint.h>
#include "bs_patch_public.h"
#define IAP_BUFFER_SIZE             4096

#define IMAGE_HEADER_LEN   sizeof(image_header_t)
#define IMAGE_MAX_LEN      (0x80000) 

#define BigtoLittle16(x)    ((((uint16_t)(x) & 0xff00) >> 8) | (((uint16_t)(x) & 0x00ff) << 8))
#define BigtoLittle32(x)    ((((uint32_t)(x) & 0xff000000) >> 24) | \
                            (((uint32_t)(x) & 0x00ff0000) >> 8) | \
                            (((uint32_t)(x) & 0x0000ff00) << 8) | \
                            (((uint32_t)(x) & 0x000000ff) << 24))

int iap_image_check(uint32_t app_backup_address, uint32_t app_run_address, image_header_t *ih_header);
#endif
