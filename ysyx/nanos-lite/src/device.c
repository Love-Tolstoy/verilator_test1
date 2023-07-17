#include <common.h>
// #include <am.h>
// #include <amdev.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  const char *ch = (char *)buf;
  for (size_t i = 0; i < len; i ++) {
    putch(ch[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  char event[16] = {};
  bool has_kbd  = io_read(AM_INPUT_CONFIG).present;

  if (has_kbd) {
    AM_INPUT_KEYBRD_T ev = io_read(AM_INPUT_KEYBRD);
    if (strcmp(keyname[ev.keycode], "NONE") == 0) {return 0;}
    sprintf(event, "%s %s %d\n", ev.keydown ? "kd" : "ku", keyname[ev.keycode], ev.keycode);
    // printf("%s", event);
    int slen = strlen(event);
    if (slen > len) {
      strncpy(buf, event, len);
      ((char *)buf)[len] = '\0';
      return len;
    }
    strcpy(buf, event);
    return slen;
  }
  return 0;
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  int w = io_read(AM_GPU_CONFIG).width;
  int h = io_read(AM_GPU_CONFIG).height;
  char info[32] = {};

  sprintf(info, "WIDTH:%d\nHEIGHT:%d\n", w, h);
  int slen = strlen(info);
  if (slen > len) {
    strncpy(buf, info, len);
    ((char *)buf)[len] = '\0';
    return len;
  }
  strcpy(buf, info);
  return slen;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  return 0;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
