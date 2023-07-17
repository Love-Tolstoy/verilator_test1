#include "common.h"
#include "sim.h"

#include "axi4.hpp"
#include "axi4_slave.hpp"
#include "axi4_mem.hpp"

#include <iostream>
using std::cout;
using std::endl;

#define OVER 0x87ffff01
#define NOIMPL 0x87ffff02
#define DIFFAIL 0x87ffff03

extern VerilatedContext* contextp;
static VerilatedVcdC* tfp = NULL;

extern VSIM* top;

CData dont_care_in = 0;
CData dont_care_out = 0;
CData len = 3;

void dump_axi_read(axi4_ref<64,64,4> &axi) {
    cout << "arid\t" << (unsigned long)axi.arid << endl;
    cout << "araddr\t" << (unsigned long)axi.araddr << endl;
    cout << "arlen\t" << (unsigned long)axi.arlen << endl;
    cout << "arsize\t" << (unsigned long)axi.arsize << endl;
    cout << "arburst\t" << (unsigned long)axi.arburst << endl;
    cout << "arvalid\t" << (unsigned long)axi.arvalid << endl;
    cout << "arready\t" << (unsigned long)axi.arready << endl;
    cout << "rid\t" << (unsigned long)axi.rid << endl;
    cout << "rdata\t" << (unsigned long)axi.rdata << endl;
    cout << "rresp\t" << (unsigned long)axi.rresp << endl;
    cout << "rlast\t" << (unsigned long)axi.rlast << endl;
    cout << "rvalid\t" << (unsigned long)axi.rvalid << endl;
    cout << "rready\t" << (unsigned long)axi.rready << endl;
}

void connect_wire(axi4_ptr <64,64,4> &mem_ptr, VSIM *top) {
    // aw
    mem_ptr.awaddr  = &(top->io_bus_wa_bits_addr);
    mem_ptr.awburst = &(top->io_bus_wa_bits_burst);
    mem_ptr.awid    = &(top->io_bus_wa_bits_id);
    mem_ptr.awlen   = &(top->io_bus_wa_bits_len);
    mem_ptr.awready = &(top->io_bus_wa_ready);
    mem_ptr.awsize  = &(top->io_bus_wa_bits_size);
    mem_ptr.awvalid = &(top->io_bus_wa_valid);
    // w
    mem_ptr.wdata   = &(top->io_bus_w_bits_data);
    mem_ptr.wlast   = &(top->io_bus_w_bits_wlast);
    mem_ptr.wready  = &(top->io_bus_w_ready);
    mem_ptr.wstrb   = &(top->io_bus_w_bits_strb);
    mem_ptr.wvalid  = &(top->io_bus_w_valid);
    // b
    mem_ptr.bid     = &(top->io_bus_b_bits_bid);
    mem_ptr.bready  = &(top->io_bus_b_ready);
    mem_ptr.bresp   = &(top->io_bus_b_bits_resp);
    mem_ptr.bvalid  = &(top->io_bus_b_valid);
    // ar
    mem_ptr.araddr  = &(top->io_bus_ra_bits_addr);
    mem_ptr.arburst = &(top->io_bus_ra_bits_burst);
    mem_ptr.arid    = &(top->io_bus_ra_bits_id);
    mem_ptr.arlen   = &(top->io_bus_ra_bits_len);
    mem_ptr.arready = &(top->io_bus_ra_ready);
    mem_ptr.arsize  = &(top->io_bus_ra_bits_size);
    mem_ptr.arvalid = &(top->io_bus_ra_valid);
    // r
    mem_ptr.rdata   = &(top->io_bus_r_bits_data);
    mem_ptr.rid     = &(top->io_bus_r_bits_rid);
    mem_ptr.rlast   = &(top->io_bus_r_bits_rlast);
    mem_ptr.rready  = &(top->io_bus_r_ready);
    mem_ptr.rresp   = &(dont_care_out);
    mem_ptr.rvalid  = &(top->io_bus_r_valid);
}

static void display() {
    printf("dontcare: %d,%d,%d\n",dont_care_in,dont_care_out,len);
}

void soc_sim_main(char *img_file) {
    contextp = new VerilatedContext;
    top = new VSIM;

    printf("start soc sim\n");
    axi4_mem<64,64,4> mem(4096l*1024*1024);
    mem.load_binary(img_file, CONFIG_MBASE);

    // printf("start2\n");
    axi4_ptr<64,64,4> mem_ptr;

    // printf("start3\n");
    connect_wire(mem_ptr, top);
    // assert(mem_ptr.check());

    // printf("start4\n");
    axi4_ref<64,64,4> mem_ref(mem_ptr);
    axi4<64,64,4> mem_sigs;
    axi4_ref<64,64,4> mem_sigs_ref(mem_sigs);

    // printf("init over\n");
    // dump_axi_read(mem_ref);

    tfp = new VerilatedVcdC;
    contextp->traceEverOn(true);
    top->trace(tfp, 0);
    tfp->open("out/soc.vcd");

    // reset
    top->reset = 1;
    top->clock = top->clock == 1?0:1;
    top->eval();
    top->clock = top->clock == 1?0:1;
    top->eval();
    top->clock = top->clock == 1?0:1;
    top->eval();
    top->reset = 0;
    top->clock = 0;

    contextp->timeInc(1);
    tfp->dump(contextp->time());

    int i = 10000;
    while(top->ioSim_pc < 0x87ffff00 && i-- > 0) {
        // display();
        top->clock = top->clock == 1?0:1;
        mem_sigs.update_input(mem_ref);
        top->eval();

        contextp->timeInc(1);
        tfp->dump(contextp->time());

        mem.beat(mem_sigs_ref);
        mem_sigs.update_output(mem_ref);
    }
    tfp->close();
    
    if (top->ioSim_pc == OVER) {
        if (top->ioSim_regs_10 == 0) {
            printf("--------- good trap -----------\n");
            exit(0);
        } else {
            printf("--------------- bad trap ------------------\n");
        }
    } else {
        printf("error\n");
    }
    exit(123);
}

