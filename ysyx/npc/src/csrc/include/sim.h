#ifndef __SIM_H__
#define __SIM_H__

#include "verilated.h"
#include "verilated_vcd_c.h"
#include "VSIM.h"

void init_sim();
bool sim();

void sim_exit(int ret);

void soc_sim_main(char *img_file);

#endif
