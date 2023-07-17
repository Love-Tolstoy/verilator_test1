package core.alu

import chisel3._
import chisel3.util._

import core.Tool.sext

class Shift1MUL extends Module {
  val io = IO(new Bundle {
    val mulValid = Input(Bool())
    val mulReady = Output(Bool())
    val flush = Input(Bool())
    val mulw = Input(Bool())

    val multiplier = Input(UInt(64.W))
    val multiplicand = Input(UInt(64.W))
    val mulSigned = Input(UInt(2.W))

    val outValid = Output(Bool())
    val result = Output(new Bundle {
      val hi = UInt(64.W)
      val lo = UInt(64.W)
    })
  })

  val count = RegInit(0.U(log2Ceil(64).W))
  val multiplier = RegInit(0.U(64.W))
  val multiplicand = RegInit(0.U(128.W))
  val result = RegInit(0.U(128.W))

  val fire = io.mulValid && io.mulReady
  val over = count === 0.U || multiplier === 0.U

  val s_none :: s_run :: s_over :: Nil = Enum(3)
  val state = RegInit(s_none)
  state := MuxCase(state, Array(
    io.flush -> s_none,
    (state === s_none && fire) -> s_run,
    (state === s_run && over) -> s_none,
    // (state === s_over) -> s_none,
  ))

  val neg = (io.mulSigned === "b11".U && io.mulw && io.multiplier(31) === 1.U) ||
    (io.mulSigned === "b11".U && !io.mulw && io.multiplier(63) === 1.U)
  val preMultiplier = MuxCase(io.multiplier, Array(
    (io.mulSigned === "b11".U && io.mulw && io.multiplier(31) === 1.U) ->
      (~io.multiplier(31, 0) + 1.U),
    (io.mulSigned === "b11".U && !io.mulw && io.multiplier(63) === 1.U) ->
      (~io.multiplier + 1.U),
    (io.mulSigned =/= "b11".U && io.mulw) -> io.multiplier(31, 0)
  ))
  val preMultiplicand = MuxCase(io.multiplicand, Array(
    (io.mulSigned(1).asBool && !io.mulw) ->
      (Fill(64, io.multiplicand(63)) ## io.multiplicand),
    (io.mulSigned(1).asBool && io.mulw) ->
      (Fill(64+32, io.multiplicand(31)) ## io.multiplicand(31, 0)),
    (!io.mulSigned(1).asBool && io.mulw) -> io.multiplicand(31, 0),
  ))

  count := MuxCase(count, Array(
    fire -> 63.U,
    (state === s_run && !over) -> (count - 1.U),
  ))
  multiplier := MuxCase(multiplier, Array(
    fire -> preMultiplier,
    (state === s_run && !over) -> (multiplier >> 1.U),
  ))
  multiplicand := MuxCase(multiplicand, Array(
    fire -> preMultiplicand,
    (state === s_run && !over) -> (multiplicand << 1.U),
  ))
  result := MuxCase(result, Array(
    fire -> 0.U,
    (state === s_run && !over) ->
      Mux(multiplier(0).asBool, result + multiplicand, result),
  ))

  io.mulReady := state === s_none

  io.outValid := state === s_run && over

  val res = Mux(neg,
    ~Mux(multiplier(0).asBool, result + multiplicand, result)+1.U,
    Mux(multiplier(0).asBool, result + multiplicand, result))
  io.result.hi := res(127, 64)
  io.result.lo := res(63, 0)
}
