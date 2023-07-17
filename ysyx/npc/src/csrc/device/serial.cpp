#include "device.h"

#include <iostream>

static uint8_t *serial_base = NULL;

static void serial_io_handler(uint32_t offset, int len, bool is_write) {
  assert(len == 1);
  if (offset == 0 && is_write) {
    std::cout.put((char)*serial_base);
    // putchar((char)*serial_base);
  }
}

void init_serial() {
  serial_base = new_space(8);
  add_mmio_map("serial", SERIAL_MMIO, serial_base, 8, serial_io_handler);
}
