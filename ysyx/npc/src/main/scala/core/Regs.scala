package core

import chisel3._
import chisel3.util._

class RegsReadIO extends Bundle {
  val rs1 = Input(UInt(5.W))
  val rs2 = Input(UInt(5.W))
  val rs1Data = Output(UInt(64.W))
  val rs2Data = Output(UInt(64.W))
}

// two read, one write once
class Regs extends Module {
  val io = IO(new Bundle {
    val wen = Input(Bool())
    // read
    val regsRead = new RegsReadIO
    // write
    val regDest = Input(UInt(5.W))
    val regData = Input(UInt(64.W))

    val regsOut = Output(Vec(32, UInt(64.W)))
  })
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  io.regsRead.rs1Data := regs(io.regsRead.rs1)
  io.regsRead.rs2Data := regs(io.regsRead.rs2)

  regs(io.regDest) := Mux(io.wen, io.regData, regs(io.regDest))
  regs(0) := 0.U

  io.regsOut := regs
}
