#include <stdlib.h>
#include <string.h>
#include "vFile.h"

vFile *vfopen(const uint8_t *dp, uint32_t size)
{
    vFile *fp = NULL;

    fp = malloc(sizeof(vFile));
    if (fp != NULL)
    {
        fp->curptr = (uint8_t *)dp;
        fp->offset = 0;
        fp->size = size;
    }

    return (fp);
}

int vfread(vFile *fp, uint8_t *buff, int len)
{
    if (fp != NULL)
    {
        if ((fp->offset + len) > fp->size)
        {
            len = fp->size - fp->offset;
        }
        memcpy(buff, fp->curptr + fp->offset, len);
        fp->offset += len;

        return (len);
    }

    return (0);
}

uint8_t *vfgetpos(vFile *fp, uint32_t *position)
{
    if (fp != NULL)
    {
        *position = fp->offset;

        return (fp->curptr + fp->offset);
    }

    return (NULL);
}

int vfsetpos(vFile *fp, uint32_t position)
{
    if (fp != NULL)
    {
        fp->offset = position;
        return (fp->offset);
    }
    return -1;
}

int vfclose(vFile *fp)
{
    if (fp != NULL)
    {
        free(fp);
    }

    return (0);
}

uint32_t vfgetlen(vFile *fp)
{
    return (fp->size);
}
/************************ (C) COPYRIGHT 2023 SHANDONG KEBAI.LTD *******************END OF FILE******/