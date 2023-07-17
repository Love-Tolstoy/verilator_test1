package core.unit

import chisel3._
import chisel3.util._

import core._

abstract class IfUnit extends RVUnit(
  new PC_IFPipeIO,
  new IF_IDPipeIO
) {

  val memDataOut = WireInit(0.U(64.W))

  regInit := IF_IDPipeIO(19.U, Param.PC_INIT.U)
  // regNext := regInit
  // regBubble := regInit

  regNext.inst := memDataOut(31,0)
  regNext.pc := pipe.in.pc

  regBubble.pc := pipe.in.pc
  regBubble.isBubble := true.B
}