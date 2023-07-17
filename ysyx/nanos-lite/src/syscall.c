#include <common.h>
#include "syscall.h"
#include <sys/time.h>
#include <fs.h>
#include <proc.h>

void sys_exit(Context *c) {
  naive_uload(NULL, "/bin/nterm");
  // halt(0);
  c->GPRx = 0;
}
void sys_yield(Context *c) {
  yield();
  c->GPRx = 0;
}
void sys_open(Context *c) {
  char *path = (char *)c->GPR2;
  c->GPRx = fs_open(path, (int)c->GPR3, (int)c->GPR4);
}
void sys_read(Context *c) {
  int fd = c->GPR2;
  char *buf = (char *)c->GPR3;
  size_t count = c->GPR4;

  c->GPRx = fs_read(fd, buf, count);
  // if (fd == 0) {
  //   assert(0);
  //   c->GPRx = c->GPR4;
  // } else if (fd > 2) {
  //   c->GPRx = fs_read(fd, buf, count);
  // } else {
  //   c->GPRx = -1;
  // }
}
void sys_write(Context *c) {
  int fd = c->GPR2;
  const char *buf = (char *)c->GPR3;
  size_t count = c->GPR4;

  c->GPRx = fs_write(fd, buf, count);
  // printf("write %d\n", (int)c->GPR4);
  // if (fd == 1 || fd == 2) {
  //   for (size_t i = 0; i < count; i ++) {
  //     putch(buf[i]);
  //   }
  //   c->GPRx = c->GPR4;
  // } else if (fd > 2) {
  //   c->GPRx = fs_write(fd, buf, count);
  // } else {
  //   c->GPRx = -1;
  // }
}
void sys_close(Context *c) {
  c->GPRx = fs_close((int)c->GPR2);
}
void sys_lseek(Context *c) {
  c->GPRx = fs_lseek((int)c->GPR2, (size_t)c->GPR3, (int)c->GPR4);
}
void sys_brk(Context *c) {
  c->GPRx = 0;
}
void sys_execve(Context *c) {
  c->GPRx = sys_uload((char *)c->GPR2);
}
void sys_gettimeofday(Context *c) {
  struct timeval *tv = (struct timeval *)c->GPR2;
  tv->tv_usec = io_read(AM_TIMER_UPTIME).us / 1000;
  // printf("time: %d", tv->tv_usec);
  tv->tv_sec = io_read(AM_TIMER_UPTIME).us / 1000000;
  c->GPRx = 0;
}
void sys_gpurender(Context *c) {
  uint32_t *pixels = (uint32_t *)c->GPR2;
  int x = (uint32_t)c->GPR3;
  int y = c->GPR3 >> 32;
  int w = (uint32_t)c->GPR4;
  int h = c->GPR4 >> 32;
  io_write(AM_GPU_FBDRAW, x, y, pixels, w, h, true);
  c->GPRx = 0;
}

void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  // printf("strace: system call %d, args:%d,%d,%d\n",
  //   (int)a[0], (int)c->GPR2, (int)c->GPR3, (int)c->GPR4);
  switch (a[0]) {
    case -1: printf("yield\n"); break;
    case 0: sys_exit(c); break;
    case 1: sys_yield(c); break;
    case 2: sys_open(c); break;
    case 3: sys_read(c); break;
    case 4: sys_write(c); break;
    case 7: sys_close(c); break;
    case 8: sys_lseek(c); break;
    case 9: sys_brk(c); break;
    case 13: sys_execve(c); break;
    case 19: sys_gettimeofday(c); break;
    case 20: sys_gpurender(c); break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
