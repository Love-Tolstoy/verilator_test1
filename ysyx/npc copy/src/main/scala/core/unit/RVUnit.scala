package core.unit

import chisel3._
import core.Control
import chisel3.util.MuxCase

abstract class RVUnit[T <: Bundle, U <: Bundle](
    pipeIn: T,
    pipeOut: U
  ) extends Module {
  val pipe = IO(new Bundle {
    val con = new Control
    val ready = Output(Bool())
    val valid = Output(Bool())

    val in = Input(pipeIn)
    val out = Output(pipeOut)

    val fire = Output(Bool())
  })

  val regInit = Wire(pipeOut)

  val regNext = WireInit(regInit)
  val regBubble = WireInit(regInit)
  val taskOver = WireInit(true.B)

  pipe.fire := taskOver && pipe.con.fire

  val regOut = RegInit(regInit)
  pipe.out := regOut
  regOut := MuxCase(regOut, Array(
    pipe.con.block -> regOut,
    (pipe.con.bubble && pipe.fire) -> regBubble,
    (pipe.fire) -> regNext,
  ))

  pipe.valid := taskOver && pipe.con.prevValid
  pipe.ready := taskOver && pipe.con.nextReady
}


