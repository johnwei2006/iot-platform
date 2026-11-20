#ifndef __BS_PATCH_PUBLIC
#define __BS_PATCH_PUBLIC

#define   BS_IAMGE_MERGE_SUCCESS     0x00
#define   BS_IMAGE_CHECK_INVALID     0x01
#define   BS_IMAGE_CHECK_VALID       0x02
#define   BS_IMAGE_LENGTH_ERROR      0x03
#define   BS_IMAGE_CRC_ERROR         0x04
#define   BS_IMAGE_ADDRESS_ERROR     0x06


#define IH_HEADER_MAGIC   0x27051956   /* Image Magic Number       */
#define IH_HEADER_NAME_LENGTH   (32 - 4)     /* Image Name Length        */

typedef struct image_header 
{
    uint32_t  ih_header_magic;/* Image Header Magic Number     */
    uint32_t ih_header_hcrc; /* Image Header CRC Checksum     */
    uint32_t ih_header_time; /* Image Creation Timestamp */
    uint32_t ih_header_size; /* Image Data Size     */
    uint32_t ih_header_load; /* Data Load Address         */
    uint32_t ih_header_ep;   /* Entry Point Address      */
    uint32_t ih_header_dcrc; /* Image Data CRC Checksum */
    uint8_t  ih_header_os;   /* Operating System         */
    uint8_t  ih_header_arch; /* CPU architecture         */
    uint8_t  ih_header_type; /* Image Type          */
    uint8_t  ih_header_comp; /* Compression Type         */
    uint8_t  ih_header_name[IH_HEADER_NAME_LENGTH]; /* Image Name      */
    uint32_t ih_header_ocrc; /* Old Image Data CRC Checksum */ 
} image_header_t;


#define IMAGE_HEADER_LEN   sizeof(image_header_t)
#define IMAGE_MAX_LEN      (0x80000) 

int  bs_iap_image_diff(void);
int  bs_iap_image_undiff(unsigned int size);
void bs_iap_jump_to_bootloader(void);
void bs_iap_jump_to_application(void);
#endif//!@__BS_PATCH_PUBLIC