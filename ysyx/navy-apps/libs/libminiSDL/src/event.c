#include <NDL.h>
#include <SDL.h>

#define keyname(k) #k,

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

static Uint8  SDL_KeyState[88] = {0};

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  if (!ev) {return 0;}
  char buf[64];
  char du[3];
  char name[1];
  int num;
  if (NDL_PollEvent(buf, sizeof(buf))) {
    sscanf(buf, "%s %s %d\n", du, name, &num);
    ev->type = du[1] == 'd' ? SDL_KEYDOWN : SDL_KEYUP;
    ev->key.type = ev->type;
    ev->key.keysym.sym = num;

    SDL_KeyState[num] = ev->type == SDL_KEYDOWN;

    return 1;
  }

  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  char buf[64];
  char du[3];
  char name[1];
  int num;
  while (!NDL_PollEvent(buf, sizeof(buf)));

  sscanf(buf, "%s %s %d\n", du, name, &num);
  event->type = du[1] == 'd' ? SDL_KEYDOWN : SDL_KEYUP;
  event->key.keysym.sym = num;
  printf("receive event: %s [%d, %d]\n", buf, event->type, event->key.keysym.sym);

  SDL_KeyState[num] = event->type == SDL_KEYDOWN;

  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return SDL_KeyState;
}
