#include "device.h"

static uint8_t *uart_base = NULL;

// static volatile uint8_t *uart = (uint8_t *)0x10000000;

static int times = 50;

#define REG_QUEUE     0
#define REG_LINESTAT  5
#define REG_STATUS_RX 0x01
#define REG_STATUS_TX 0x20

// void uart_putc(uint8_t ch) {
//   while ((uart_base[REG_LINESTAT] & REG_STATUS_TX) == 0);
//   uart_base[REG_QUEUE] = ch;
//   if (ch == '\n') uart_putc('\r');
// }

// int uart_getc(void) {
//   if (uart_base[REG_LINESTAT] & REG_STATUS_RX) {
//     return uart_base[REG_QUEUE];
//   }
//   return -1;
// }

void uart_init(void){
  uart_base[1] = 0x00;
  uart_base[3] = 0x80;
  uart_base[0] = 0x03;
  uart_base[1] = 0x00;
  uart_base[3] = 0x03;
  uart_base[2] = 0xc7;
  uart_base[5] = 1;
}

static void uart_io_handler(uint32_t offset, int len, bool is_write) {
  // assert(offset == 0 || offset == 4);
  if (times > 0) {
    uart_base[5] = (1 << 5);
    times --;
  } else {
    uart_base[5] = (1 << 5) | 1;
  }
  if (is_write) {
    // printf("%d,%d : %d\n", offset, len, uart_base[0]);
    putchar(uart_base[0]);
  }
  if (!is_write) {
    if (offset == 0) {
      // printf("read..");
      uart_base[0] = getchar();
    }
  }
}

void init_uart() {
  uart_base = (uint8_t *)new_space(8);
  add_mmio_map("uart", UART_MMIO, uart_base, 8, uart_io_handler);
  uart_init();
}
