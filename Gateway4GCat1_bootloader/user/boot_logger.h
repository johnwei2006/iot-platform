#ifndef __BOOT_LOGGER_H
#define __BOOT_LOGGER_H

/* Includes ------------------------------------------------------------------*/
void Boot_Printf(char *fmt, ...);
void USART1_Init(void);
void USART1_WaitTxComplete(void);
#endif /* __BOOT_LOGGER_H */