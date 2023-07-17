#ifndef __CPU_H__
#define __CPU_H__

#include "common.h"
// #include "VCU.h"
#include "VSIM.h"

typedef struct {
  word_t gpr[32];
  vaddr_t pc;
} CPU_state;

extern const char *regs[];

extern CPU_state cpu;
extern VSIM* top;

void init_cpu();
bool cpu_run(int times);

void step_and_dump_wave();
void difftest_step(vaddr_t pc, vaddr_t npc);
void difftest_skip_ref();
void init_difftest(char *ref_so_file, long img_size, int port);

#endif
