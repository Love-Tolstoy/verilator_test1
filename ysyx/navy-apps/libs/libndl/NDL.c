#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <fcntl.h>
#include "syscall.h"

int _gpurender(size_t *pixels, size_t pos, size_t rect);

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;

uint32_t NDL_GetTicks() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_usec;
}

int NDL_PollEvent(char *buf, int len) {
  int fd = open("/dev/events", 1, 0);
  int ret = read(fd, buf, len);
  return buf != NULL && ret > 0;
}

void NDL_OpenCanvas(int *w, int *h) {
  printf("dispinfo: %d*%d\n", *w, *h);
  if (*w == 0 && *h == 0) {
    *w = screen_w;
    *h = screen_h;
  }
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  }
}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  if (x + w > screen_w || y + h > screen_h) {
    printf("GPU render overload: %d*%d,%d*%d\n",x,y,w,h);
  }
  if (x == 0 && y == 0 && w == 0 && h == 0) {
    w = screen_w;
    h = screen_h;
  }
  size_t pos = y;
  pos = (pos << 32) | x;
  size_t rect = h;
  rect = (rect << 32) | w;

  _gpurender((size_t *)pixels, pos, rect);
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  char dispinfo[32] = {};
  int fd = open("/proc/dispinfo", 1, 0);
  read(fd, dispinfo, 31);
  sscanf(dispinfo, "WIDTH:%d\nHEIGHT:%d\n", &screen_w, &screen_h);

  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  return 0;
}

void NDL_Quit() {
}
