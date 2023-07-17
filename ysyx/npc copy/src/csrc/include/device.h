#ifndef __DEVICE_H__
#define __DEVICE_H__

#include "common.h"

#define SERIAL_MMIO   0xa00003f8
#define KBD_MMIO      0xa0000060
#define RTC_MMIO      0xa0000048
#define VGACTL_MMIO   0xa0000100
#define AUDIO_MMIO    0xa0000200
#define DISK_MMIO     0xa0000300
#define FB_ADDR       0xa1000000
#define SB_ADDR       0xa1200000
#define UART_MMIO     0x10000000

typedef void(*io_callback_t)(uint32_t, int, bool);

void init_device();
bool device_update();

uint64_t get_time();

word_t mmio_read(paddr_t addr, int len);
void mmio_write(paddr_t addr, int len, word_t data);

uint8_t* new_space(int size);
void add_mmio_map(const char *name, paddr_t addr, void *space, uint32_t len, io_callback_t callback);

void init_map();
void init_serial();
void init_timer();
void init_vga();
void init_uart();

void vga_update_screen();

#endif
