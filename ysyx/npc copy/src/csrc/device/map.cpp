#include "device.h"
#include <stdlib.h>

#define PAGE_SHIFT        12
#define PAGE_SIZE         (1ul << PAGE_SHIFT)
#define PAGE_MASK         (PAGE_SIZE - 1)
#define IO_SPACE_MAX (2 * 1024 * 1024)

static uint8_t *io_space = NULL;
static uint8_t *p_space = NULL;

uint8_t* new_space(int size) {
  uint8_t *p = p_space;
  // page aligned;
  size = (size + (PAGE_SIZE - 1)) & ~PAGE_MASK; // malloc
  p_space += size;
  assert(p_space - io_space < IO_SPACE_MAX);
  return p;
}

void init_map() {
  io_space = (uint8_t *)malloc(IO_SPACE_MAX);
  assert(io_space);
  p_space = io_space;
}

// word_t map_read(paddr_t addr, int len, IOMap *map) {
//   assert(len >= 1 && len <= 8);
//   check_bound(map, addr);
//   paddr_t offset = addr - map->low;
//   invoke_callback(map->callback, offset, len, false); // prepare data to read
//   word_t ret = host_read(map->space + offset, len);
//   return ret;
// }

// void map_write(paddr_t addr, int len, word_t data, IOMap *map) {
//   assert(len >= 1 && len <= 8);
//   check_bound(map, addr);
//   paddr_t offset = addr - map->low;
//   host_write(map->space + offset, len, data);
//   invoke_callback(map->callback, offset, len, true);
// }