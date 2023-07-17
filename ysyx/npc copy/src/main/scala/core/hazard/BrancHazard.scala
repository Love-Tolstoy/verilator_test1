package core.hazard

import chisel3._
import chisel3.util._

import core.RVOpcode
import core.Tool

class BrancHazard extends Module {
  val io = IO(new Bundle {
    val dnpcIn = Input(UInt(64.W))
    val branch = Input(Bool())  // if inst is branch
    val branchPc = Input(UInt(64.W))
    val dnpcOut = Output(UInt(64.W))

    val bubble = Output(Bool())
  })
  io.dnpcOut := Mux(io.branch, io.branchPc, io.dnpcIn)
  io.bubble := Mux(io.branch, true.B, false.B)
}

class BranchControl extends Module {
  val io = IO(new Bundle {
    val ifuInst = Input(UInt(32.W))
    val ifuPc = Input(UInt(64.W))

    val iduPc = Input(UInt(64.W))
    val exuBranch = Input(Bool())
    val exuJump = Input(Bool())
    val exuJumpPc = Input(UInt(64.W))

    val dnpcOut = Output(UInt(64.W))
    val bubble = Output(Bool())
  })
  val perIdu = Module(new PerIDU)

  // predict state
  val sNotTaken :: wNotTaken :: wTaken :: sTaken :: Nil = Enum(4)
  val predictState = RegInit(0.U(2.W))

  val predictSucc = io.exuBranch && io.iduPc === io.exuJumpPc

  predictState := predictState

  // for ifu
  perIdu.io.pc := io.ifuPc
  perIdu.io.inst := io.ifuInst

  val ifuDnpc = MuxCase(perIdu.io.snpc, Array(
    !perIdu.io.branch -> perIdu.io.snpc,
    (perIdu.io.branch && predictState(1).asBool) -> perIdu.io.dnpc,
  ))

  // for exu
  when(io.exuBranch) {
    predictState := MuxLookup(predictState, 0.U, Array(
      sNotTaken ->  Mux(predictSucc, sNotTaken, wNotTaken),
      wNotTaken ->  Mux(predictSucc, sNotTaken, wTaken),
      wTaken ->     Mux(predictSucc, sTaken, wNotTaken),
      sTaken ->     Mux(predictSucc, sTaken, wTaken),
    ))
  }

  io.bubble := Mux(io.exuJump && !predictSucc, true.B, false.B)
  io.dnpcOut := Mux(io.bubble, io.exuJumpPc, ifuDnpc)
}

class PerIDU extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(64.W))
    val inst = Input(UInt(32.W))

    val branch = Output(Bool())
    val snpc = Output(UInt(64.W))
    val dnpc = Output(UInt(64.W))
  })

  val i = io.inst
  val dest = Tool.sext(i(31,31) ## i(7,7) ##
    i(30,25) ## i(11,8) ## 0.U(1.W), 13)

  io.branch := i(6,0) === RVOpcode.BRANCH.asUInt
  io.snpc := io.pc + 4.U
  io.dnpc := io.pc + dest
}
