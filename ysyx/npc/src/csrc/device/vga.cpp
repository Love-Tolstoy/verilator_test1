#include "device.h"

// #define SCREEN_W 800
// #define SCREEN_H 600
#define SCREEN_W 400
#define SCREEN_H 300

static uint32_t screen_width() {
  // return MUXDEF(CONFIG_TARGET_AM, io_read(AM_GPU_CONFIG).width, SCREEN_W);
  return SCREEN_W;
}

static uint32_t screen_height() {
  // return MUXDEF(CONFIG_TARGET_AM, io_read(AM_GPU_CONFIG).height, SCREEN_H);
  return SCREEN_H;
}

static uint32_t screen_size() {
  return screen_width() * screen_height() * sizeof(uint32_t);
}

static void *vmem = NULL;
static uint32_t *vgactl_port_base = NULL;

// #ifdef CONFIG_VGA_SHOW_SCREEN
// #ifndef CONFIG_TARGET_AM
#include <SDL2/SDL.h>

static SDL_Renderer *renderer = NULL;
static SDL_Texture *texture = NULL;

static void init_screen() {
  SDL_Window *window = NULL;
  char title[128];
  sprintf(title, "%s-NPC", "riscv-64");
  SDL_Init(SDL_INIT_VIDEO);
  SDL_CreateWindowAndRenderer(
      SCREEN_W * 2,
      SCREEN_H * 2,
      0, &window, &renderer);
  SDL_SetWindowTitle(window, title);
  texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888,
      SDL_TEXTUREACCESS_STATIC, SCREEN_W, SCREEN_H);
}

static inline void update_screen() {
  SDL_UpdateTexture(texture, NULL, vmem, SCREEN_W * sizeof(uint32_t));
  SDL_RenderClear(renderer);
  SDL_RenderCopy(renderer, texture, NULL, NULL);
  SDL_RenderPresent(renderer);
}
// static void init_screen() {}

// static inline void update_screen() {
//   ioe_write(AM_GPU_FBDRAW, 0, 0, vmem, screen_width(), screen_height(), true);
// }

void vga_update_screen() {
  // TODO: call `update_screen()` when the sync register is non-zero,
  // then zero out the sync register
  uint32_t sync = vgactl_port_base[1];
  if (sync == 1) {
    update_screen();
    vgactl_port_base[1] = 0;
  }
}

void init_vga() {
  vgactl_port_base = (uint32_t *)new_space(8);
  vgactl_port_base[0] = (screen_width() << 16) | screen_height();

  add_mmio_map("vgactl", VGACTL_MMIO, vgactl_port_base, 8, NULL);

  vmem = new_space(screen_size());
  add_mmio_map("vmem", FB_ADDR, vmem, screen_size(), NULL);
  init_screen();
  memset(vmem, 0, screen_size());
}
