#ifndef __RISCV64_REG_H__
#define __RISCV64_REG_H__

#include <common.h>

static inline int check_reg_idx(int idx) {
  IFDEF(CONFIG_RT_CHECK, assert(idx >= 0 && idx < 32));
  return idx;
}

static inline int check_csr_idx(int idx) {
  switch (idx) {
    case 0x300: return 0;
    case 0x305: return 1;
    case 0x341: return 2;
    case 0x342: return 3;
    default: assert(0);
  }
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])
#define sr(idx) (cpu.sr[check_csr_idx(idx)])

static inline const char* reg_name(int idx, int width) {
  extern const char* regs[];
  return regs[check_reg_idx(idx)];
}

#endif
