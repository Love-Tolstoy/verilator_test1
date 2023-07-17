package core.stages

import chisel3._
import chisel3.util._

import core.unit._
import core.RVCommand._
import core._

class EXU extends ExUnit {
  val io = IO(new Bundle {
    val branch = Output(Bool())
    val jump = Output(Bool())
    val jumpPc = Output(UInt(64.W))

    // exception
    val csrio = Flipped(new ExpectRWOneIO)
    val mret = Output(Bool())

    val raiseIntr = Output(Bool())
    // val ecode = Output(UInt(4.W))

    // forward
    val regDest = Output(UInt(5.W))
    val regData = Output(UInt(64.W))
    val exLoad = Output(Bool())
  })

  val s_alu :: s_waitCon :: Nil = Enum(2)
  val state = RegInit(s_alu)
  state := MuxCase(state, Array(
    (state === s_alu && alu.io.outValid && pipe.con.fire) -> s_alu,
    (state === s_alu && alu.io.outValid && !pipe.con.fire) -> s_waitCon,
    (state === s_waitCon && pipe.con.fire) -> s_alu,
  ))

  alu.io.flush := false.B
  alu.io.aluIn.valid := state === s_alu && alu.io.aluIn.ready
  taskOver := alu.io.outValid || state === s_waitCon

  val dnpc = Wire(UInt(64.W))
  val snpc = Wire(UInt(64.W))
  snpc := pc + 4.U
  dnpc := MuxLookup(comm.asUInt, snpc, Array(
    jal.asUInt    -> (pc + src1),
    jalr.asUInt   -> ((src1 + src2) & (~1.U(64.W))),

    beq.asUInt    -> Mux(src1 === src2, pc + dest, snpc),
    bne.asUInt    -> Mux(src1 =/= src2, pc + dest, snpc),
    blt.asUInt    -> Mux(src1.asSInt < src2.asSInt, pc + dest, snpc),
    bge.asUInt    -> Mux(src1.asSInt >= src2.asSInt, pc + dest, snpc),
    bltu.asUInt   -> Mux(src1 < src2, pc + dest, snpc),
    bgeu.asUInt   -> Mux(src1 >= src2, pc + dest, snpc),

    ebreak.asUInt -> Param.PC_OVER.U,               // over
    inv.asUInt    -> Param.PC_NOIMPL.U,               // not imp

    // ecall.asUInt  -> io.mtvec,
    mret.asUInt   -> io.csrio.dataOut,
  ))

  // expect
  csrt := io.csrio.dataOut

  io.csrio.readIndex := MuxCase(0.U, Array(
    comm.isOneOf(csrrs,csrrw,csrrc,csrrsi,csrrwi,csrrci) -> src2,
    (comm === mret) -> "x341".U,
  ))
  io.csrio.wen := comm.isOneOf(csrrs,csrrw,csrrc,csrrsi,csrrwi,csrrci) && pipe.fire
  io.csrio.writeIndex := src2
  io.csrio.dataIn := MuxCase(0.U, Array(
    comm.isOneOf(csrrs, csrrsi)  -> (csrt | src1),
    comm.isOneOf(csrrw, csrrwi)  -> src1,
    comm.isOneOf(csrrc, csrrci)  -> (csrt & ~src1),
  ))
  io.raiseIntr := comm === ecall

  io.mret := comm === mret

  // branch
  io.branch := comm.isOneOf(beq,bne,blt,bge,bltu,bgeu)
  io.jump := comm.isOneOf(jal,jalr,beq,bne,blt,bge,bltu,bgeu,ebreak,inv,mret)
  val branchPcReg = RegNext(io.jumpPc, 0.U(64.W))  // record
  io.jumpPc := Mux(io.jump, dnpc, branchPcReg)

  // data forward
  io.regDest := regNext.regDest
  io.regData := regNext.regData
  io.exLoad := regNext.memEn && regNext.memRw
}
