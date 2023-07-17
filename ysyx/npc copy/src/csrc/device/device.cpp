#include "device.h"
#include "mem.h"
#include <SDL2/SDL.h>

// typedef void(*io_callback_t)(uint32_t, int, bool);

typedef struct {
  const char *name;
  // we treat ioaddr_t as paddr_t here
  paddr_t low;
  paddr_t high;
  void *space;
  io_callback_t callback;
} IOMap;

static IOMap maps[16];
static int maps_len = 0;

/* device interface */
void add_mmio_map(const char *name, paddr_t addr, void *space, uint32_t len, io_callback_t callback) {
  assert(maps_len < 16);
  maps[maps_len] = (IOMap){ .name = name, .low = addr, .high = addr + len - 1,
    .space = space, .callback = callback };
  printf("Add mmio map '%s' at [ %08x ,  %08x ]\n",
      maps[maps_len].name, maps[maps_len].low, maps[maps_len].high);

  maps_len ++;
}

void init_device() {
#ifdef CONFIG_DEVICE
  printf("init device\n");
  init_map();
  init_serial();
  init_timer();
  init_vga();
  init_uart();
#endif
}

// true: quit
bool device_update() {
  static uint64_t last = 0;
  uint64_t now = get_time();
  if (now - last < 1000000 / 60) {
    return false;
  }
  last = now;

  vga_update_screen();

  SDL_Event event;
  while (SDL_PollEvent(&event)) {
    switch (event.type) {
      case SDL_QUIT:
        return true;
        break;
#ifdef CONFIG_HAS_KEYBOARD
      // If a key was pressed
      case SDL_KEYDOWN:
      case SDL_KEYUP: {
        uint8_t k = event.key.keysym.scancode;
        bool is_keydown = (event.key.type == SDL_KEYDOWN);
        send_key(k, is_keydown);
        break;
      }
#endif
      default: break;
    }
  }
  return false;
}

static int get_map(paddr_t addr, int len) {
  for (int i = 0; i < maps_len; i ++) {
    if (addr >= maps[i].low && addr <= maps[i].high) {
      return i;
    }
  }
  printf("mem %08x error\n", addr);
  return -1;
}

static void invoke_callback(io_callback_t c, paddr_t offset, int len, bool is_write) {
  if (c != NULL) { c(offset, len, is_write); }
}

word_t mmio_read(paddr_t addr, int len) {
  int map_index = get_map(addr, len);
  if (map_index != -1) {
    paddr_t offset = addr - maps[map_index].low;
    invoke_callback(maps[map_index].callback, offset, len, false);
    return host_read(maps[map_index].space + offset, len);
  }
  assert(0);
}

void mmio_write(paddr_t addr, int len, word_t data) {
  int map_index = get_map(addr, len);
  if (map_index != -1) {
    paddr_t offset = addr - maps[map_index].low;
    host_write(maps[map_index].space + offset, len, data);
    invoke_callback(maps[map_index].callback, offset, len, true);
    return;
  }
  assert(0);
}


