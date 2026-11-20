#include <stdbool.h>
#include "utility.h"

void utl_delayms(uint32_t ms)
{
  for(volatile uint32_t cnt = 0;cnt < ms;cnt ++)
  {
    uint16_t i = 4670;
    while(i --){
      ;
    }
  }
}

int utl_memcmp_byte(uint8_t *p1, uint8_t value, int size)
{
    while(size--)
    {
        if(*(p1++) != value) return false;
    }
    return true;
}

/************************ (C) COPYRIGHT 2023 SHANDONG KEBAI.LTD *******************END OF FILE******/
