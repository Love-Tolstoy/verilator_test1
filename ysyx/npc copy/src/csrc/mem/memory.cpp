#include "mem.h"
#include "cpu.h"
#include "device.h"

#include "sim.h"

static uint8_t pmem[CONFIG_MSIZE] = {};

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

static inline bool in_pmem(paddr_t addr) {
  return (addr >= CONFIG_MBASE) && (addr < (paddr_t)CONFIG_MBASE + CONFIG_MSIZE);
}

static void out_of_bound(paddr_t addr) {
  printf("out of bound addr: 0x%x\n", addr);
  sim_exit(1);
}

word_t pmem_read(paddr_t addr, int len) {
#ifdef CONFIG_MTRACE    // MTRACE
  // if (addr != cpu.pc)   // filter pc read
    printf("READ  MEM 0x%08x: ", addr);
#endif
  if (in_pmem(addr) == false) {
#ifdef CONFIG_DEVICE
    return mmio_read(addr, len);
#endif
    out_of_bound(addr); return 0;
  }
  word_t ret = host_read(guest_to_host(addr), len);
#ifdef CONFIG_MTRACE
  // if (addr != cpu.pc)   // filter pc read
    printf("0x%08lx [len:%d]\n", ret, len);
#endif
  return ret;
}

void pmem_write(paddr_t addr, int len, word_t data) {
#ifdef CONFIG_MTRACE
  printf("WRITE MEM 0x%08x: 0x%08lx [len:%d]\n", addr, data, len);
#endif
  if (in_pmem(addr) == false) {
#ifdef CONFIG_DEVICE
    mmio_write(addr, len, data); return;
#endif
    out_of_bound(addr); return;
  }
  host_write(guest_to_host(addr), len, data);
}

// word_t vmem_read(paddr_t addr, int len) {
//   word_t ret = host_read(guest_to_host(addr), len);
//   return ret;
// }

// void vmem_write(paddr_t addr, int len, word_t data) {
//   host_write(guest_to_host(addr), len, data);
// }




