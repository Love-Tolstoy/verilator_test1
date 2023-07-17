#include <proc.h>
#include <elf.h>
#include <fs.h>

#if defined(__ISA_AM_NATIVE__)
# define EXPECT_TYPE EM_X86_64
#elif defined(__ISA_RISCV32__) || defined(__ISA_RISCV64__)
# define EXPECT_TYPE EM_RISCV
#else
# error Unsupported ISA
#endif

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

static uintptr_t loader(PCB *pcb, const char *filename) {
  Elf_Ehdr elf;
  Elf_Phdr phdr;

  int fd = fs_open(filename, 0, 0);
  fs_read(fd, &elf, sizeof(Elf_Ehdr));

  assert(*(uint32_t *)elf.e_ident == 0x464c457f);

  for(int i = 0; i < elf.e_phnum; i ++) {
    fs_lseek(fd, elf.e_phoff + i*sizeof(Elf_Phdr), SEEK_SET);
    fs_read(fd, &phdr, sizeof(Elf_Phdr));
    printf("%x,%x,%x\n", phdr.p_vaddr, phdr.p_offset, phdr.p_memsz);
    fs_lseek(fd, phdr.p_offset, SEEK_SET);
    fs_read(fd, (void *)phdr.p_vaddr, phdr.p_memsz);
    memset((void *)(phdr.p_vaddr + phdr.p_filesz), 0,
      phdr.p_memsz == 0 ? 0 : phdr.p_memsz-phdr.p_filesz);
  }
  fs_close(fd);

  return elf.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", (void *)entry);
  ((void(*)())entry) ();
}

int sys_uload(const char *filename) {
  int fd = fs_open(filename, 0, 0);
  if (fd == -1) { return -1; }
  naive_uload(NULL, filename);
  return 0;
}