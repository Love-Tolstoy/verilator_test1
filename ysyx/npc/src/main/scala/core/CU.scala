package core

import chisel3._
import chisel3.util._

import core.hazard._
import core.stages._
import core.except._
import device._

class CU extends Module {
  val io = IO(new Bundle {
    val en = Input(Bool())

    val bus = new AXI4M_MasterIO
  })
  val regs = Module(new Regs)
  val pc = Module(new PC)
  val csrs = Module(new CSRS(10))

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val meu = Module(new MEU)

  val clint = Module(new CLINT)

  // connect flow
  PipeLine.init(ifu, idu, exu, meu)

  // pipe input
  ifu.pipe.in.pc := pc.io.pcOut
  // idu.io.regsRead <> regs.io.regsRead

  // pipe output
  val regsWen = RegNext(meu.pipe.fire, false.B)
  regs.io.wen := regsWen
  regs.io.regDest := meu.pipe.out.regDest
  regs.io.regData := meu.pipe.out.regData

  // branch control
  val bc = Module(new BranchControl)
  bc.io.ifuInst := ifu.io.inst
  bc.io.ifuPc := ifu.io.pc
  bc.io.iduPc := idu.pipe.in.pc
  bc.io.exuBranch := exu.io.branch && exu.pipe.fire
  bc.io.exuJump := exu.io.jump
  bc.io.exuJumpPc := exu.io.jumpPc

  // data conflict control
  val dh = Module(new DataHazard)
  dh.io.regsRead <> regs.io.regsRead
  dh.io.exRegDest := exu.io.regDest
  dh.io.exRegData := exu.io.regData
  dh.io.exLoad := exu.io.exLoad
  dh.io.meRegDest := meu.io.regDest
  dh.io.meRegData := meu.io.regData
  dh.io.wbRegDest := meu.pipe.out.regDest
  dh.io.wbRegData := meu.pipe.out.regData

  idu.io.regsRead <> dh.io.idRegsRead

  // exception
  val exp = Module(new Exceptions)
  exp.io.en := exu.io.raiseIntr || clint.io.raiseIntr
  exp.io.ecode := Mux(clint.io.raiseIntr, CLINT.ecode, 11.U)
  exp.io.epc := Mux(exu.pipe.in.isBubble, exu.io.jumpPc, exu.pipe.in.pc)  // fuck CLINT
  // exp.io.dnpc := Mux(bh.io.branch, bh.io.dnpcOut, idu.pipe.in.pc) // tmd
  exp.io.mret := exu.io.mret
  exp.io.csrsIn := csrs.io.regsOut

  csrs.io.regsWen := exu.pipe.fire
  csrs.io.regsIn := exp.io.csrsOut

  exu.io.csrio <> csrs.io.rwio

  // pc
  pc.io.wen := ifu.pipe.fire
  pc.io.pcNext := Mux(exp.io.raiseIntr, exp.io.exceptPc, bc.io.dnpcOut)

  // pipe line control
  when(exp.io.raiseIntr) {
    ifu.pipe.con.bubble := true.B
    idu.pipe.con.bubble := true.B
    exu.pipe.con.bubble := true.B
  }.elsewhen(meu.io.fencei) {
    ifu.pipe.con.bubble := true.B
    idu.pipe.con.bubble := true.B
    exu.pipe.con.bubble := true.B
    pc.io.pcNext := meu.pipe.in.pc + 4.U
  }.elsewhen(dh.io.loadConflict) {
    ifu.pipe.con.block := true.B
    idu.pipe.con.bubble := true.B
  }.elsewhen(bc.io.bubble) {
    ifu.pipe.con.bubble := true.B
    idu.pipe.con.bubble := true.B
  }
  // ifu.pipe.con.block := dh.io.loadConflict
  // ifu.pipe.con.bubble := bh.io.bubble || exp.io.raiseIntr
  // idu.pipe.con.bubble := dh.io.loadConflict || bh.io.bubble || exp.io.raiseIntr

  // bus
  val arbiter = Module(new AXI4_Crossbar)
  // val mem = Module(new AXI4MEM)
  arbiter.io.in(0) <> ifu.io.bus
  arbiter.io.in(1) <> meu.io.bus
  arbiter.io.out <> io.bus

  // device
  clint.io.mtie := csrs.io.regsOut(CSRS.mapIndex(CSRSNumber.Mie))(7)
  clint.io.rwio <> meu.io.clintio
}
