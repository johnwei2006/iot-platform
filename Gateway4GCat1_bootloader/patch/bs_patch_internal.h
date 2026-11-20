#ifndef __BS_PATCH_INTERNAL_H	
#define __BS_PATCH_INTERNAL_H
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <assert.h>
#include "flash_manager.h"
#include "vFile.h"
#include "crc32.h"
#include "lzma_decompress.h"
#include "stm32f4xx.h"
//#include "stm32l151_eval.h"

#ifndef TRUE
#define TRUE  1
#endif

#ifndef FALSE
#define FALSE 0
#endif

#define   BS_ASSERT(exp)     if(!(exp)){ printf("ERROR: ASSERT FAIL!!!, %s [%s]\r\n", #exp, __func__);}while(1); 
                                  
                                 
#endif//!@__BS_PATCH_INTERNAL_H