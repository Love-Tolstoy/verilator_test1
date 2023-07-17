#include "cpu.h"
#include "device.h"
// #include "state.h"

extern VerilatedContext* contextp;

CPU_state cpu;

#define OVER 0x87ffff01
#define NOIMPL 0x87ffff02
#define DIFFAIL 0x87ffff03

int ifTimes = 0;
int iCacheHitTimes = 0;

bool diff_skips[5] = {false};
int s = 0;

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void display() {
  printf("pc : 0x%lx \t pcOver : 0x%lx", top->ioSim_pc, top->ioSim_pcOver);
  if (top->ioSim_branch) printf("[branch]");
  if (top->ioSim_loadConflict) printf("[load conflict]");
  if (top->ioSim_dataForward) printf("[data forward]");
  printf("\n");
  word_t *p_regs = &top->ioSim_regs_0;
  for (int i = 0; i < 32; i += 2) {
    printf("%s : 0x%08lx \t %s : 0x%08lx\n", regs[i], p_regs[i], regs[i+1], p_regs[i+1]);
  }
  printf("\n");
}

static void cpu_exec_once() {
  // display();
  step_and_dump_wave();
  step_and_dump_wave();
#ifdef CONFIG_ETRACE
  if (top->ioSim_raiseIntr) {
    // printf("time:%ld ;csrs:0x%lx,0x%lx,0x%lx,0x%lx,0x%lx,0x%lx,0x%lx\n",
    //   contextp->time(),
    //   top->ioSim_csrs_0, top->ioSim_csrs_1, top->ioSim_csrs_2, top->ioSim_csrs_3,
    //   top->ioSim_csrs_4, top->ioSim_csrs_5, top->ioSim_csrs_6);
    printf("[trap]pc: 0x%lx --> 0x%lx\n", top->ioSim_csrs_3, top->ioSim_csrs_2);
  }
  if (top->ioSim_mret) {
    printf("[mret]pc: 0x%lx <-- 0x%lx\n", top->ioSim_csrs_3, top->ioSim_pcOver);
  }
#endif
  if (top->ioSim_regsChange) {
#ifdef CONFIG_ITRACE
    display();
#endif
    word_t last_pcOver = cpu.pc;
    cpu.pc = top->ioSim_pcOver;
    word_t *p_regs = &top->ioSim_regs_0;
      for (int i = 0; i < 32; i ++) {
      cpu.gpr[i] = p_regs[i];
    }
#ifdef CONFIG_DIFFTEST
    if (diff_skips[s]) {
      difftest_skip_ref();
    } else {
      // printf("[debug]%lx, %lx\n", last_pcOver, top->ioSim_pcOver);
      difftest_step(top->ioSim_pcOver, top->ioSim_pcOver);
    }
    diff_skips[s] = false;
    s = (s+1)%5;
#endif
  }
}

void init_cpu() {
  top->reset = 1;
  step_and_dump_wave();
  step_and_dump_wave();
  step_and_dump_wave();
  top->reset = 0;
  cpu.pc = top->ioSim_pcOver;

  // skip 4 clk
  for (int i = 0; i < 4; i++)
    diff_skips[(s+i)%5] = true;
  printf("init cpu over\n");
}

bool cpu_exit() {
  if (top->ioSim_pc == NOIMPL) {
    printf("------- not impl --------\n");
  }
  else if (top->ioSim_pc == DIFFAIL) {
    printf("-------- difftest fail --------\n");
    display();
  }
  else if (top->ioSim_pc == OVER) {
    for (int i = 0; i < 2; i++)   // not true over
      cpu_exec_once();

    if (cpu.gpr[10] == 0) {
      printf("--------- good trap -----------\n");
      return true;
    } else {
      printf("--------------- bad trap ------------------\n");
    }
  }
  return false;
}

bool cpu_run(int times) {
  while (top->ioSim_pc < 0x87ffff00 && times--) {
    if (top->ioSim_loadConflict) {
      diff_skips[(s+2)%5] = true;
      // diff_skips[(s+3)%5] = true;
    } else if (top->ioSim_branch) {
      diff_skips[(s+2)%5] = true;
      diff_skips[(s+3)%5] = true;
      // diff_skips[(s+4)%5] = true;
    }
    cpu_exec_once();
#ifdef CONFIG_DEVICE
    if(device_update()) {top->ioSim_pc = OVER;};
#endif
  }
  return cpu_exit();
}
