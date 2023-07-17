#include <time.h>
#include "device.h"

static uint32_t *rtc_port_base = NULL;

static uint64_t boot_time = 0;

static uint64_t get_time_internal() {
  struct timespec now;
  clock_gettime(CLOCK_MONOTONIC_COARSE, &now);
  uint64_t us = now.tv_sec * 1000000 + now.tv_nsec / 1000;
  return us;
}

uint64_t get_time() {
  if (boot_time == 0) boot_time = get_time_internal();
  uint64_t now = get_time_internal();
  return now - boot_time;
}

static void rtc_io_handler(uint32_t offset, int len, bool is_write) {
  assert(offset == 0 || offset == 4);
  if (!is_write && offset == 4) {
    uint64_t us = get_time();
    rtc_port_base[0] = (uint32_t)us;
    rtc_port_base[1] = us >> 32;
  }
}

// #ifndef CONFIG_TARGET_AM
// static void timer_intr() {
//   if (nemu_state.state == NEMU_RUNNING) {
//     extern void dev_raise_intr();
//     dev_raise_intr();
//   }
// }
// #endif

void init_timer() {
  rtc_port_base = (uint32_t *)new_space(8);
  add_mmio_map("rtc", RTC_MMIO, rtc_port_base, 8, rtc_io_handler);
}
