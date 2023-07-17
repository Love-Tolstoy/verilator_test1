#include <unistd.h>
#include <stdio.h>
#include "fixedptc.h"

int main() {
  fixedpt a = fixedpt_rconst(1.2);
  fixedpt b = fixedpt_fromint(10);
  int c = 0;
  if (b > fixedpt_rconst(7.9)) {
    c = fixedpt_toint(fixedpt_div(fixedpt_mul(a + FIXEDPT_ONE, b), fixedpt_rconst(2.01)));
    printf("(1.2 + 1) * 10 / 2.01 = %d\n", c);
  }
  printf("floor 1.2 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(1.2))));
  printf("floor -1.2 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(-1.2))));
  printf("floor -1 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_fromint(-1))));
  printf("floor 0 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_fromint(0))));
  printf("ceil -1 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_fromint(-1))));
  printf("ceil 0 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_fromint(0))));
  printf("ceil 2.001 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_rconst(2.3))));
  printf("ceil 8,388,600.1 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_rconst(8388600.1))));
  printf("floor 8,388,600.1 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(8388600.1))));
  printf("ceil -8,388,600.1 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_rconst(-8388600.1))));
  printf("floor -8,388,600.1 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(-8388600.1))));
  printf("ceil 8,388,606.1 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_rconst(8388606.1))));
  printf("floor 8,388,607.1 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(8388607.1))));
  printf("ceil -8,388,607.1 = %d\n", fixedpt_toint(fixedpt_ceil(fixedpt_rconst(-8388607.1))));
  printf("floor -8,388,607.1 = %d\n", fixedpt_toint(fixedpt_floor(fixedpt_rconst(-8388607.1))));
  return 0;
}
