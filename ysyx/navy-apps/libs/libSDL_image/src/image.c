#define SDL_malloc  malloc
#define SDL_free    free
#define SDL_realloc realloc

#define SDL_STBIMAGE_IMPLEMENTATION
#include "SDL_stbimage.h"

SDL_Surface* IMG_Load_RW(SDL_RWops *src, int freesrc) {
  assert(src->type == RW_TYPE_MEM);
  assert(freesrc == 0);
  return NULL;
}

SDL_Surface* IMG_Load(const char *filename) {
  printf("img load %s\n", filename);
  FILE *fp = fopen(filename, "r");
  if (!fp) return NULL;

  fseek(fp, SEEK_SET, SEEK_END);
  int size = ftell(fp);

  uint8_t *buf = SDL_malloc(size);
  fseek(fp, SEEK_SET, 0);
  fread(buf, 1, size, fp);

  SDL_Surface* ret = STBIMG_LoadFromMemory(buf, size);
  SDL_free(buf);
  fclose(fp);
  return ret;
}

int IMG_isPNG(SDL_RWops *src) {
  return 0;
}

SDL_Surface* IMG_LoadJPG_RW(SDL_RWops *src) {
  return IMG_Load_RW(src, 0);
}

char *IMG_GetError() {
  return "Navy does not support IMG_GetError()";
}
