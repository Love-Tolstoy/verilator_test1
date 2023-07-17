package core.stages

import chisel3._
import chisel3.util._

import core.unit._
import core._

class IDU extends IdUnit {
  val io = IO(new Bundle {
    val regsRead = Flipped(new RegsReadIO)
  })

  io.regsRead.rs1 := Mux(
    typ === RVType.I ||
    typ === RVType.S ||
    typ === RVType.B ||
    typ === RVType.R ||
    typ === RVType.C, rs1, 0.U)
  io.regsRead.rs2 := Mux(
    typ === RVType.S ||
    typ === RVType.B ||
    typ === RVType.R, rs2, 0.U)

  rs1Data := io.regsRead.rs1Data
  rs2Data := io.regsRead.rs2Data
}
