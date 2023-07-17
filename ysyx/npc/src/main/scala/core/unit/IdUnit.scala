package core.unit

import chisel3._
import chisel3.util._

import core.RVCommand
import core.Param._
import core.Tool._
import core._
import core.RVCommand._

abstract class IdUnit extends RVUnit(
  new IF_IDPipeIO,
  new ID_EXPipeIO
) {

  regInit := ID_EXPipeIO(addi, 0.U, 0.U, 0.U, false.B, PC_INIT.U)

  val rs1Data = WireInit(0.U(64.W))
  val rs2Data = WireInit(0.U(64.W))

  def immI(i: UInt) = { sext(i(31,20), 12) }
  def immU(i: UInt) = { (sext(i(31,12), 20) << 12) }
  def immS(i: UInt) = { sext(i(31,25) ## i(11,7), 12) }
  def immJ(i: UInt) = {
    sext(i(31,31) ## i(19,12) ##
      i(20,20) ## i(30,21) ##
      0.U(1.W), 21)
  }
  def immB(i: UInt) = {
    sext(i(31,31) ## i(7,7) ##
      i(30,25) ## i(11,8) ##
      0.U(1.W), 13)
  }

  val inst = pipe.in.inst

  val typ = MuxLookup(inst(6,0), RVType.N, Array(
    RVOpcode.LOAD.asUInt ->       RVType.I,
    RVOpcode.MISC_MEM.asUInt ->   RVType.I,
    RVOpcode.OP_IMM.asUInt ->     RVType.I,
    RVOpcode.AUIPC.asUInt ->	    RVType.U,
    RVOpcode.OP_IMM_32.asUInt ->  RVType.I,
    RVOpcode.STORE.asUInt ->	    RVType.S,
    RVOpcode.OP.asUInt ->	        RVType.R,
    RVOpcode.LUI.asUInt -> 	      RVType.U,
    RVOpcode.OP_32.asUInt ->      RVType.R,
    RVOpcode.BRANCH.asUInt ->     RVType.B,
    RVOpcode.JALR.asUInt ->       RVType.I,
    RVOpcode.JAL.asUInt ->        RVType.J,
    RVOpcode.SYSTEM.asUInt ->     RVType.C,
      // Mux(inst(14,12) > 0.U, RVType.I, RVType.N),
  ))

  val funct3 = inst(14,12)
  val funct7 = inst(31,25)
  val funct12 = inst(31,20)
  regNext.comm := MuxLookup(inst(6,0), inv, Array(
    RVOpcode.OP_IMM.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> addi,
      "b010".U -> slti,
      "b011".U -> sltiu,
      "b100".U -> xori,
      "b110".U -> ori,
      "b111".U -> andi,
      "b001".U -> Mux(inst(31,26) === "b000000".U, slli, inv),
      "b101".U -> MuxLookup(inst(31,26), inv, Array(
        "b000000".U -> srli,
        "b010000".U -> srai,
      )),
    )),
    RVOpcode.OP.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> add,
        "b0100000".U -> sub,
        "b0000001".U -> mul,
      )),
      "b001".U -> Mux(funct7 === "b0000000".U, sll, inv),
      "b010".U -> Mux(funct7 === "b0000000".U, slt, inv),
      "b011".U -> Mux(funct7 === "b0000000".U, sltu, inv),
      "b100".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> xor,
        "b0000001".U -> div,
      )),
      "b101".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> srl,
        "b0100000".U -> sra,
        "b0000001".U -> divu,
      )),
      "b110".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> or,
        "b0000001".U -> rem,
      )),
      "b111".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> and,
        "b0000001".U -> remu,
      )),
    )),
    RVOpcode.BRANCH.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> beq,
      "b001".U -> bne,
      "b100".U -> blt,
      "b101".U -> bge,
      "b110".U -> bltu,
      "b111".U -> bgeu,
    )),
    RVOpcode.LOAD.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> lb,
      "b001".U -> lh,
      "b010".U -> lw,
      "b011".U -> ld,
      "b100".U -> lbu,
      "b101".U -> lhu,
      "b110".U -> lwu,
    )),
    RVOpcode.STORE.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> sb,
      "b001".U -> sh,
      "b010".U -> sw,
      "b011".U -> sd,
    )),
    RVOpcode.OP_IMM_32.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> addiw,
      "b001".U -> Mux(funct7 === "b0000000".U, slliw, inv),
      "b101".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> srliw,
        "b0100000".U -> sraiw,
      )),
    )),
    RVOpcode.OP_32.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> addw,
        "b0100000".U -> subw,
        "b0000001".U -> mulw,
      )),
      "b001".U -> Mux(funct7 === "b0000000".U, sllw, inv),
      "b100".U -> Mux(funct7 === "b0000001".U, divw, inv),
      "b101".U -> MuxLookup(funct7, inv, Array(
        "b0000000".U -> srlw,
        "b0100000".U -> sraw,
        "b0000001".U -> divuw,
      )),
      "b110".U -> Mux(funct7 === "b0000001".U, remw, inv),
      "b111".U -> Mux(funct7 === "b0000001".U, remuw, inv),
    )),
    RVOpcode.LUI.asUInt -> lui,
    RVOpcode.AUIPC.asUInt -> auipc,
    RVOpcode.JAL.asUInt -> jal,
    RVOpcode.JALR.asUInt -> jalr,
    RVOpcode.SYSTEM.asUInt -> MuxLookup(funct3, inv, Array(
      "b000".U -> MuxLookup(funct12, inv, Array(
        "b000000000000".U -> ecall,
        "b000000000001".U -> ebreak,
        "b001100000010".U -> mret,
      )),
      "b001".U -> csrrw,
      "b010".U -> csrrs,
      "b011".U -> csrrc,
      "b101".U -> csrrwi,
      "b110".U -> csrrsi,
      "b111".U -> csrrci,
    )),
  RVOpcode.MISC_MEM.asUInt -> fencei,
  ))
  regNext.dest := MuxLookup(typ.asUInt, inst(11,7), Array(
    RVType.S.asUInt -> immS(inst),
    RVType.B.asUInt -> immB(inst),
  ))
  val rs1 = inst(19,15)
  regNext.src1 := MuxLookup(typ.asUInt, rs1, Array(
    RVType.I.asUInt -> rs1Data,
    RVType.U.asUInt -> immU(inst),
    RVType.S.asUInt -> rs1Data,
    RVType.J.asUInt -> immJ(inst),
    RVType.B.asUInt -> rs1Data,
    RVType.R.asUInt -> rs1Data,
    RVType.C.asUInt -> Mux(regNext.comm.isOneOf(csrrs,csrrw,csrrc),
      rs1Data, rs1),
  ))
  val rs2 = inst(24,20)
  regNext.src2 := MuxLookup(typ.asUInt, rs2, Array(
    RVType.I.asUInt -> immI(inst),
    RVType.S.asUInt -> rs2Data,
    RVType.B.asUInt -> rs2Data,
    RVType.R.asUInt -> rs2Data,
    RVType.C.asUInt -> inst(31, 20)
  ))
  regNext.regWen := !typ.isOneOf(RVType.S,RVType.B,RVType.N)
  regNext.pc := pipe.in.pc
  regNext.isBubble := pipe.in.isBubble

  regBubble.pc := pipe.in.pc
  regBubble.isBubble := true.B
}
