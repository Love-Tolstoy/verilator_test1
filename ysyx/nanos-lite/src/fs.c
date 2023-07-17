#include <fs.h>

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB};

static size_t open_offset[100];

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
  [FD_STDIN]  = {"stdin", 0, 0, invalid_read, invalid_write},
  [FD_STDOUT] = {"stdout", 0, 0, invalid_read, serial_write},
  [FD_STDERR] = {"stderr", 0, 0, invalid_read, serial_write},
  {"/dev/events", 0, 0, events_read, invalid_write},
  {"/proc/dispinfo", 0, 0, dispinfo_read, invalid_write},
  {"/dev/fb", 0, 0, invalid_read, fb_write},
#include "files.h"
  {"/dev/null", 0, 0, invalid_read, invalid_write}
};

void init_fs() {
  // TODO: initialize the size of /dev/fb
  int w = io_read(AM_GPU_CONFIG).width;
  int h = io_read(AM_GPU_CONFIG).height;
  file_table[5].size = w*h;
}

int fs_open(const char *pathname, int flags, int mode) {
  int i = 0;
  while (true) {
    Finfo file = file_table[i];
    if (strcmp(pathname, file.name) == 0) {
      break;
    }
    if (strcmp(file.name, "/dev/null") == 0) {
      printf("file not exist: %s\n", pathname);
      return -1;
    }
    i ++;
  }
  // printf("trace: file open: %s\n", pathname);
  open_offset[i] = 0;
  return i;
}
size_t fs_read(int fd, void *buf, size_t len) {
  if (file_table[fd].read != NULL) {
    return file_table[fd].read(buf, 0, len);
  }

  size_t offset = file_table[fd].disk_offset + open_offset[fd];
  if (open_offset[fd] + len > file_table[fd].size) {
    len = file_table[fd].size - open_offset[fd];
    assert(len >= 0);
  }
  open_offset[fd] += len;
  ramdisk_read(buf, offset, len);
  return len;
}
size_t fs_write(int fd, const void *buf, size_t len) {
  if (file_table[fd].write != NULL) {
    return file_table[fd].write(buf, 0, len);
  }

  size_t offset = file_table[fd].disk_offset + open_offset[fd];
  if (open_offset[fd] + len > file_table[fd].size) {
    len = file_table[fd].size - open_offset[fd];
    assert(len >= 0);
  }
  open_offset[fd] += len;
  ramdisk_write(buf, offset, len);
  return len;
}
size_t fs_lseek(int fd, size_t offset, int whence) {
  if (whence == SEEK_SET) {
    open_offset[fd] = offset;
  } else if (whence == SEEK_CUR) {
    open_offset[fd] += offset;
  } else if (whence == SEEK_END) {
    open_offset[fd] = file_table[fd].size + offset;
  } else {
    assert(0);
  }
  assert(open_offset[fd] <= file_table[fd].size);
  return open_offset[fd];
}
int fs_close(int fd) {
  return 0;
}
