package core

import chisel3._
import chisel3.util._

class PC extends Module {
  val io = IO(new Bundle {
    val wen = Input(Bool())
    val pcNext = Input(UInt(64.W))
    val pcOut = Output(UInt(64.W))
  })
  val pcReg = RegInit(Param.PC_INIT.U(64.W))
  pcReg := MuxCase(pcReg, Array(
    io.wen -> io.pcNext,
  ))
  io.pcOut := pcReg
}


class PCU extends Module {
  val io = IO(new Bundle {
    val con = new Control
    val valid = Output(Bool())

    val pcIn = Input(UInt(64.W))
    val pcOut = Output(UInt(64.W))

    val branch = Input(Bool())
    val branchPc = Input(UInt(64.W))
    val bubble = Output(Bool())

    val flowOut = new PC_IF
  })
  io.valid := true.B

  val dnpc = Wire(UInt(64.W))
  // PC Control
  dnpc := MuxCase(io.pcIn, Array(
    (io.branch && io.con.fire) -> io.branchPc,
    io.con.fire -> (io.pcIn + 4.U),
  ))
  io.pcOut := dnpc

  io.flowOut.pc := io.pcIn

  io.bubble := io.branch
}
