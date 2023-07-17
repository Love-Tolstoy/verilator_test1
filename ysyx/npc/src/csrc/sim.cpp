#include "sim.h"
#include "cpu.h"

VerilatedContext* contextp = NULL;
static VerilatedVcdC* tfp = NULL;

VSIM* top;

void init_sim() {
  contextp = new VerilatedContext;
  top = new VSIM;
#ifdef CONFIG_VCD_RECORD
  tfp = new VerilatedVcdC;
  contextp->traceEverOn(true);
  top->trace(tfp, 0);
  tfp->open("out/cpu.vcd");
#endif
}

void step_and_dump_wave() {
  top->clock = top->clock == 1?0:1;
  top->eval();
#ifdef CONFIG_VCD_RECORD
  contextp->timeInc(1);
  tfp->dump(contextp->time());
#endif
#ifdef CONFIG_ITRACE
  printf("time: %ld\n", contextp->time());
#endif
}

void sim_exit(int ret) {
  step_and_dump_wave();
#ifdef CONFIG_VCD_RECORD
  tfp->close();
#endif
  exit(ret);
}

bool sim() {
  init_cpu();
  bool ans = cpu_run(-1);
  sim_exit(!ans);
  return ans;
}
