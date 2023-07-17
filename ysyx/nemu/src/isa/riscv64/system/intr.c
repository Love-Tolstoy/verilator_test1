#include <isa.h>

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */
  // printf("intr : %ld, 0x%lx\n", NO, epc);
  cpu.sr[2] = epc;
  if (NO == -1) {
    cpu.sr[3] = -1;
  } else {
    cpu.sr[3] = 11;
  }
  return cpu.sr[1];
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
