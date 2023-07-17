package core.unit

import chisel3._
import chisel3.util._

import core.RVCommand
import core.Param
import core.Tool._

abstract class MeUnit extends RVUnit(
  new EX_MEPipeIO,
  new ME_WBPipeIO
) {

  val memDataOut = WireInit(0.U(64.W))

  regInit := ME_WBPipeIO(0.U, 0.U, Param.PC_INIT.U)

  val comm = pipe.in.comm
  val memAddr = pipe.in.memAddr
  val memData = pipe.in.memData

  regNext.regDest := pipe.in.regDest
  regNext.regData := Mux(pipe.in.memEn,
    MuxLookup(comm.asUInt, pipe.in.regData, Array(
      RVCommand.lb.asUInt     -> sext(memDataOut, 8),
      RVCommand.lh.asUInt     -> sext(memDataOut, 16),
      RVCommand.lw.asUInt     -> sext(memDataOut, 32),
      RVCommand.ld.asUInt     -> memDataOut,
      RVCommand.lbu.asUInt    -> uext(memDataOut, 8),
      RVCommand.lhu.asUInt    -> uext(memDataOut, 16),
      RVCommand.lwu.asUInt    -> uext(memDataOut, 32),
    )), pipe.in.regData
  )
  regNext.pc := pipe.in.pc

  regBubble.pc := pipe.in.pc
}
