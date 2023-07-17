#include <stdio.h>

#include <NDL.h>

int main() {
  int i = 20;
  long now = 0;
  long t = 0;

  NDL_Init(0);
  while (i--) {
    t = NDL_GetTicks();
    while (now < t+500) {
      now = NDL_GetTicks();
    }
    printf("timer test %dst: now is %ld\n", 20-i, now);
  }
  return 0;
}
