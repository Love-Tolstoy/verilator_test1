package core.unit

import chisel3._
import chisel3.util._

import core.RVCommand
import core.RVCommand._
import core.Tool._
import core.Param.PC_INIT
import core.alu._

abstract class ExUnit extends RVUnit(
  new ID_EXPipeIO,
  new EX_MEPipeIO
) {

  regInit := EX_MEPipeIO(false.B, true.B, addi, 0.U, 0.U, 0.U, 0.U, PC_INIT.U)

  val csrt = WireInit(0.U(64.W))

  val comm = pipe.in.comm
  val src1 = pipe.in.src1
  val src2 = pipe.in.src2
  val dest = pipe.in.dest
  val pc = pipe.in.pc

  val aluIn = Wire(new ALUIO)
  aluIn := MuxLookup(comm.asUInt, ALUIO(0.U, 0.U, ALUOp.add), Array(
    auipc.asUInt  -> ALUIO(pc, src1, ALUOp.add),
    lui.asUInt    -> ALUIO(src1, 0.U, ALUOp.add),
    jal.asUInt    -> ALUIO(pc, 4.U, ALUOp.add),
    jalr.asUInt   -> ALUIO(pc, 4.U, ALUOp.add),

    addi.asUInt   -> ALUIO(src1, src2, ALUOp.add),
    slti.asUInt   -> ALUIO(src1, src2, ALUOp.lt, true.B),
    sltiu.asUInt  -> ALUIO(src1, src2, ALUOp.lt),
    xori.asUInt   -> ALUIO(src1, src2, ALUOp.xor),
    ori.asUInt    -> ALUIO(src1, src2, ALUOp.or),
    andi.asUInt   -> ALUIO(src1, src2, ALUOp.and),
    slli.asUInt   -> ALUIO(src1, src2(5,0), ALUOp.sl),
    srli.asUInt   -> ALUIO(src1, src2(5,0), ALUOp.sr),
    srai.asUInt   -> ALUIO(src1, src2(5,0), ALUOp.sr, true.B),

    addiw.asUInt  -> ALUIO(src1, src2, ALUOp.add, false.B, true.B),
    slliw.asUInt  -> ALUIO(src1(31,0), src2(4,0), ALUOp.sl, false.B, true.B),
    srliw.asUInt  -> ALUIO(src1(31,0), src2(4,0), ALUOp.sr, false.B, true.B),
    sraiw.asUInt  -> ALUIO(src1(31,0), src2(4,0), ALUOp.sr, true.B, true.B),

    add.asUInt    -> ALUIO(src1, src2, ALUOp.add),
    sub.asUInt    -> ALUIO(src1, src2, ALUOp.sub),
    slt.asUInt    -> ALUIO(src1, src2, ALUOp.lt, true.B),
    sltu.asUInt   -> ALUIO(src1, src2, ALUOp.lt),
    sll.asUInt    -> ALUIO(src1, src2(5,0), ALUOp.sl),
    srl.asUInt    -> ALUIO(src1, src2(5,0), ALUOp.sr),
    sra.asUInt    -> ALUIO(src1, src2(5,0), ALUOp.sr, true.B),
    xor.asUInt    -> ALUIO(src1, src2, ALUOp.xor),
    or.asUInt     -> ALUIO(src1, src2, ALUOp.or),
    and.asUInt    -> ALUIO(src1, src2, ALUOp.and),
    mul.asUInt    -> ALUIO(src1, src2, ALUOp.mul),
    div.asUInt    -> ALUIO(src1, src2, ALUOp.div, true.B),
    divu.asUInt   -> ALUIO(src1, src2, ALUOp.div),
    rem.asUInt    -> ALUIO(src1, src2, ALUOp.rem, true.B),
    remu.asUInt   -> ALUIO(src1, src2, ALUOp.rem),

    addw.asUInt   -> ALUIO(src1, src2, ALUOp.add, false.B, true.B),
    subw.asUInt   -> ALUIO(src1, src2, ALUOp.sub, false.B, true.B),
    sllw.asUInt   -> ALUIO(src1(31,0), src2(4,0), ALUOp.sl, false.B, true.B),
    srlw.asUInt   -> ALUIO(src1(31,0), src2(4,0), ALUOp.sr, false.B, true.B),
    sraw.asUInt   -> ALUIO(src1(31,0), src2(4,0), ALUOp.sr, true.B, true.B),
    mulw.asUInt   -> ALUIO(src1(31,0), src2(31,0), ALUOp.mul, false.B, true.B),
    divw.asUInt   -> ALUIO(src1(31,0), src2(31,0), ALUOp.div, true.B, true.B),
    divuw.asUInt  -> ALUIO(src1(31,0), src2(31,0), ALUOp.div, false.B, true.B),
    remw.asUInt   -> ALUIO(src1(31,0), src2(31,0), ALUOp.rem, true.B, true.B),
    remuw.asUInt  -> ALUIO(src1(31,0), src2(31,0), ALUOp.rem, false.B, true.B),

    csrrs.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
    csrrw.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
    csrrc.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
    csrrwi.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
    csrrsi.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
    csrrci.asUInt  -> ALUIO(csrt, 0.U, ALUOp.none),
  ))
  val alu = ALU(aluIn)

  regNext.memEn := comm.isOneOf(lb,lh,lw,ld,lbu,lhu,lwu,sb,sh,sw,sd)
  regNext.memRw := comm.isOneOf(lb,lh,lw,ld,lbu,lhu,lwu)
  regNext.comm := comm
  regNext.memAddr := Mux(regNext.memRw, src1 + src2, src1 + dest)
  regNext.memData := src2
  regNext.regDest := Mux(!pipe.in.regWen, 0.U, dest(4,0))
  regNext.regData := alu.io.ans
  regNext.pc := pc
  // regNext.isBubble := pipe.in.isBubble

  regBubble.pc := pc
  // regBubble.isBubble := true.B
}
