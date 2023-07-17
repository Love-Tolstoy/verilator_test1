package core.alu

import chisel3._
import chisel3.util._

class Radix2DIV extends Module {
  val io = IO(new Bundle {
    val divValid = Input(Bool())
    val divReady = Output(Bool())
    val flush = Input(Bool())
    val divw = Input(Bool())

    val dividend = Input(UInt(64.W))
    val divisor = Input(UInt(64.W))
    val divSigned = Input(Bool())

    val outValid = Output(Bool())
    val quotient = Output(UInt(64.W))
    val remainder = Output(UInt(64.W))
  })

  def surplusZeros(num: UInt, bit: Int): UInt = {
    if(bit == 0) {63.U}
    else {
      Mux(num(bit).asBool, (63-bit).U, surplusZeros(num, bit-1))
    }
  }

  val count = RegInit(0.U(log2Ceil(64).W))
  val dividend = RegInit(0.U(128.W))
  val divisor = WireInit(0.U(64.W))

  val quotient = RegInit(0.U(64.W))

  val fire = Wire(Bool())
  val over = Wire(Bool())
  fire := io.divValid && io.divReady
  over := count === 0.U || dividend === 0.U

  val s_none :: s_run :: s_over :: Nil = Enum(3)
  val state = RegInit(s_none)
  state := MuxCase(state, Array(
    io.flush -> s_none,
    (state === s_none && fire) -> s_run,
    (state === s_run && over) -> s_over,
    (state === s_over) -> s_none,
  ))

  val preDividend = MuxCase(io.dividend, Array(
    (io.divw && io.divSigned && io.dividend(31) === 1.U) -> (~io.dividend(31, 0) + 1.U),
    (!io.divw && io.divSigned && io.dividend(63) === 1.U) -> (~io.dividend + 1.U),
    (io.divw && !io.divSigned) -> io.dividend(31, 0),
  ))
  val preDivisor = MuxCase(io.divisor, Array(
    (io.divw && io.divSigned && io.divisor(31) === 1.U) -> (~io.divisor(31, 0) + 1.U),
    (!io.divw && io.divSigned && io.divisor(63) === 1.U) -> (~io.divisor + 1.U),
    (io.divw && !io.divSigned) -> io.divisor(31, 0),
  ))
  divisor := preDivisor
  val surplus = surplusZeros(preDividend, 63)

  val subAns = Wire(UInt(64.W))
  val subAnsPos = Wire(Bool())
  subAns := dividend(127, 63) - divisor
  subAnsPos := dividend(127, 63) >= divisor
  count := MuxCase(count, Array(
    fire -> (63.U - surplus),
    (state === s_run && !over) -> (count - 1.U),
  ))
  dividend := MuxCase(dividend, Array(
    fire -> (preDividend << surplus),
    (state === s_run && !over) -> (Mux(subAnsPos, subAns ## dividend(62, 0), dividend) << 1.U),
    (state === s_run && over) -> (Mux(subAnsPos, subAns ## dividend(62, 0), dividend) << count),
  ))
  quotient := MuxCase(quotient, Array(
    fire -> 0.U,
    (state === s_run && !over) -> ((quotient(63, 1) ## subAnsPos.asUInt) << 1.U),
    (state === s_run && over) -> ((quotient(63, 1) ## subAnsPos.asUInt) << count),
  ))

  io.divReady := state === s_none

  io.outValid := state === s_over

  val dividendPos = Mux(io.divw, io.dividend(31), io.dividend(63)) === 0.U
  val divisorPos = Mux(io.divw, io.divisor(31), io.divisor(63)) === 0.U
  io.quotient := MuxCase(quotient, Array(
    !io.divSigned -> quotient,
    (dividendPos && divisorPos) -> quotient,
    (dividendPos && !divisorPos) -> (~quotient + 1.U),
    (!dividendPos && divisorPos) -> (~quotient + 1.U),
    (!dividendPos && !divisorPos) -> quotient,
  ))
  io.remainder := MuxCase(dividend(126, 63), Array(
    !io.divSigned -> dividend(126, 63),
    (dividendPos && divisorPos) -> dividend(126, 63),
    (dividendPos && !divisorPos) -> dividend(126, 63),
    (!dividendPos && divisorPos) -> (~dividend(126, 63) + 1.U),
    (!dividendPos && !divisorPos) -> (~dividend(126, 63) + 1.U),
  ))
}
