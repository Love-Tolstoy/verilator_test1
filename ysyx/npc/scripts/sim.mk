# TOP = CU
TOP = SIM

CPP_PATH = $(NPC_HOME)/src/csrc
INC_PATH = $(NPC_HOME)/src/csrc/include
SOC_PATH = $(NPC_HOME)/src/csrc/include/axi4

# CONFIG_SOC_TEST = y

# CONFIG_VCD_RECORD = y
# CONFIG_ITRACE = y
# CONFIG_MTRACE = y
# CONFIG_ETRACE = y
# CONFIG_DIFFTEST = y
CONFIG_DEVICE = y

CFLAGS = -I$(INC_PATH) -I$(SOC_PATH)
ifdef CONFIG_SOC_TEST
	CFLAGS += -DCONFIG_SOC_TEST
endif
ifdef CONFIG_VCD_RECORD
	CFLAGS += -DCONFIG_VCD_RECORD
endif
ifdef CONFIG_ITRACE
	CFLAGS += -DCONFIG_ITRACE
endif
ifdef CONFIG_MTRACE
	CFLAGS += -DCONFIG_MTRACE
endif
ifdef CONFIG_ETRACE
	CFLAGS += -DCONFIG_ETRACE
endif
ifdef CONFIG_DIFFTEST
	CFLAGS += -DCONFIG_DIFFTEST
endif
ifdef CONFIG_DEVICE
	CFLAGS += -DCONFIG_DEVICE
endif

LDFLAGS = -lpthread -lSDL2 -fsanitize=address -ldl

SIMFLAGS =
ifdef CONFIG_DIFFTEST
	SIMFLAGS += -d $(NEMU_HOME)/build/riscv64-nemu-interpreter-so
endif

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc \
				-O3 --x-assign fast --x-initial fast --noassert	\
				-CFLAGS "$(CFLAGS)" -LDFLAGS "$(LDFLAGS)"


CHISEL_SRCS = $(shell find $(CHISEL_PATH) -name "*.scala")

# VERILOG_TARGET = $(notdir$(shell find $(abspath ./out/bulid) -name "*.v"))
VSRCS = $(shell find $(abspath ./build) -name "*.v")
CSRCS = $(shell find $(CPP_PATH) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
DPICS = $(shell find $(abspath ./src/vsrc) -name "*.v")


dpi-c:
	$(VERILATOR) -cc $(DPICS) --Mdir $(OBJ_DIR)

$(BUILD_DIR)/$(TOP).v: $(CHISEL_SRCS)
	make verilog
	# @echo "replace ALU.v"
	# @sed -i '7s/64/63/g' build/ALU.v

$(OBJ_DIR)/V$(TOP): $(BUILD_DIR)/$(TOP).v dpi-c
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOP) \
		$(CSRCS) $(VSRCS) \
		--Mdir $(OBJ_DIR) --exe --trace

sim: $(OBJ_DIR)/V$(TOP)
	./build/obj_dir/V$(TOP) $(SIMFLAGS)
	@# $(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "sim over"

run: $(OBJ_DIR)/V$(TOP)
	./build/obj_dir/V$(TOP) $(SIMFLAGS) $(ARGS) $(IMG)

clean-obj:
	-rm -rf $(OBJ_DIR)

.PHONY: dpi-c sim clean-obj
