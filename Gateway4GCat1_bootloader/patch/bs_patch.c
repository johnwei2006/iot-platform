/*
 * Copyright (c) 2023-2024, KeBai Development Team
 *
 * SPDX-License-Identifier: Apache-2.0
 * Change Logs:
 * Date           Author    Notes
 * 2023-12-2      xhc       the first version
 * 2024-3-4       xhc       translate virtual address to physical address
 * 2024-3-11      xhc       modify dynamic memery cache stack allocation
 */
#include "bs_patch_internal.h"
#include "bs_patch.h"

#define WRITE_BLOCK_SIZE    1024
#define DCOMPRESS_BUFFER_SIZE   1024

#ifdef __DYNAMIC_MEMERY__
static uint8_t *s_diff_data_buff = NULL;
#else 
static uint8_t s_diff_data_buff[DCOMPRESS_BUFFER_SIZE];
#endif
static int s_diff_data_len;
static int s_diff_data_fp;
static int s_new_data_fp;

static int64_t offtin(uint8_t *buf)
{
    int64_t y;

    y = buf[7] & 0x7F;
    y = y * 256;
    y += buf[6];
    y = y * 256;
    y += buf[5];
    y = y * 256;
    y += buf[4];
    y = y * 256;
    y += buf[3];
    y = y * 256;
    y += buf[2];
    y = y * 256;
    y += buf[1];
    y = y * 256;
    y += buf[0];

    if (buf[7] & 0x80) y = -y;

    return y;
}

static int bspatch(const uint8_t *old, int64_t oldsize, int64_t newsize, struct bspatch_stream *stream)
{
     uint8_t buf[8];
    uint8_t *buf_data;
    int64_t oldpos, newpos, len;
    int64_t ctrl[3];
    int64_t i;

    buf_data = (uint8_t *)malloc(WRITE_BLOCK_SIZE + 1);
    if (buf_data == NULL)return -1;

    oldpos = 0;
    newpos = 0;
    while (newpos < newsize)
    {
        /* Read control data */
        for (i = 0; i <= 2; i++)
        {
            if (stream->read(stream, buf, 8))
            {
                return -1;
            }
            ctrl[i] = offtin(buf);
        };

        /* Sanity-check */
        if (ctrl[0] < 0 || ctrl[0] > INT_MAX ||
                ctrl[1] < 0 || ctrl[1] > INT_MAX ||
                newpos + ctrl[0] > newsize)
        {
         
            return -1;
        }

        /* Read diff string */
        while (ctrl[0] > 0)
        {
            if (ctrl[0] > WRITE_BLOCK_SIZE)len = WRITE_BLOCK_SIZE;
            else len = ctrl[0];
            if (stream->read(stream, buf_data, len))return -1;
            for (i = 0; i < len; i++)
            {
                if ((oldpos + i >= 0) && (oldpos + i < oldsize))
                {
                    buf_data[i] += old[oldpos + i];
                }
            }
            stream->write(stream, buf_data, len);
            ctrl[0] -= len;
            oldpos += len;
            newpos += len;
        }

        /* Sanity-check */
        if (newpos + ctrl[1] > newsize)
        {
         
            return -1;
        }

        /* Read extra string */
        while (ctrl[1] > 0)
        {
            if (ctrl[1] > WRITE_BLOCK_SIZE)len = WRITE_BLOCK_SIZE;
            else len = ctrl[1];
            if (stream->read(stream, buf_data, len))return -1;
            stream->write(stream, buf_data, len);
            ctrl[1] -= len;
            newpos += len;
        }

        /* Adjust pointers */
        oldpos += ctrl[2];
    };

    if (buf_data != NULL) free(buf_data);

    return 0;
}

static int patch_data_read(const struct bspatch_stream *stream, void *buffer, int length)
{
    uint8_t *dp = (uint8_t *)buffer;
    vFile *pf;

    pf = (vFile *) stream->opaque_r;

    for (int i = 0; i < length; i++)
    {
        if (s_diff_data_len == 0)
        {
               memset(s_diff_data_buff, 0, DCOMPRESS_BUFFER_SIZE);
            s_diff_data_len = lzma_decompress_read(pf, s_diff_data_buff, DCOMPRESS_BUFFER_SIZE);
            
            if (s_diff_data_len > 0)
            {
                s_diff_data_fp = 0;
            }else
            {
                return -1;
            }
        }
        if (s_diff_data_len > 0)
        {
            *(dp++) = s_diff_data_buff[s_diff_data_fp++];
            s_diff_data_len--;
        }
    }

    return 0;
}

static int new_data_write(const struct bspatch_stream *stream, void *buffer, int length)
{
    uint32_t file_addr;

    file_addr = *((uint32_t *)stream->opaque_w);

    printf("Data:");

    for(int i = 0; i < length; i++) {  
        
        printf(" %02x", ((uint8_t *)buffer)[i]);
    }
    
    printf("\r\n");
    printf("\r\n");
   
    uint32_t py_addr =  file_addr + s_new_data_fp + BACKUP_APP_BASE_ADDRESS;

    printf("flash write address = %08x, length = %d\r\n", py_addr, length);
   
    flash_write_data(BACKUP_APP_BASE_ADDRESS + file_addr + s_new_data_fp, buffer, length);

    s_new_data_fp += length;
  
    return (TRUE);
}

static void patch_data_read_finish(void)
{
    lzma_decompress_finish();
}

int bs_iap_patch(const uint8_t *old, uint32_t oldsize, const uint8_t *patch, uint32_t patchsize, uint32_t newfile)
{
    vFile *patch_fp;
    uint8_t header[24];
    int64_t newsize;

    s_diff_data_len = 0;
    s_diff_data_fp = 0;
    s_new_data_fp = 0;
   
    /*virtual file interface*/
    patch_fp = vfopen(patch, patchsize);

    if (patch_fp == NULL)
    {
        return (0);
    }

    vfread(patch_fp, header, sizeof(header));

    if (memcmp(header, "ENDSLEY/BSDIFF43", 16) != 0)
    {
        return (0);
    }

    newsize = offtin(header + 16);
    if (newsize < 0)
    {
        return (0);
    }
#ifdef __DYNAMIC_MEMERY__
    s_diff_data_buff = malloc(DCOMPRESS_BUFFER_SIZE);

    if (s_diff_data_buff == NULL)
    {
        return (0);
    }
    
    memset(s_diff_data_buff, 0, DCOMPRESS_BUFFER_SIZE);
#endif    
    struct bspatch_stream stream;
    
    stream.read = patch_data_read;
    stream.opaque_r = (void *)patch_fp;
    stream.write = new_data_write;
    stream.opaque_w = &newfile;  /*IAP_DIFF_OFFSET_ADDRESS = 0x2800*/

    int res = bspatch(old, oldsize, newsize, &stream);
    
    patch_data_read_finish();
#ifdef __DYNAMIC_MEMERY__
    free(s_diff_data_buff);
#else 
    memset(s_diff_data_buff, 0, DCOMPRESS_BUFFER_SIZE);
#endif
    vfclose(patch_fp);

    return ((int)newsize);
}
/************************ (C) COPYRIGHT 2023 SHANDONG KEBAI.LTD *******************END OF FILE******/