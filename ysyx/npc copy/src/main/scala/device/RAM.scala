package device

import chisel3._
import chisel3.util._

class RAM_T[T <: Data](depth: Int, dataTyp: T) extends Module {
  val io = IO(new Bundle {
    val cen = Input(Bool())
    val wen = Input(Bool())
    val addr = Input(UInt(log2Ceil(depth).W))
    val dataIn = Input(dataTyp)
    val dataOut = Output(dataTyp)
  })
  val mem = SyncReadMem(depth, dataTyp)
  io.dataOut := DontCare
  when(io.cen) {
    val rdwrPort = mem(io.addr)
    when (io.wen) { rdwrPort := io.dataIn }
    .otherwise    { io.dataOut := rdwrPort }
  }
}
object RAM_T {
  def apply[T <: Data](depth: Int, dataTyp: T): RAM_T[T] = {
    Module(new RAM_T(depth, dataTyp))
  }
}

class RAM(depth: Int, width: Int) extends Module {
  val io = IO(new Bundle {
    val cen = Input(Bool())
    val wen = Input(Bool())
    val addr = Input(UInt(log2Ceil(depth).W))
    val mask = Input(UInt((width/8).W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
  })
  val mem = SyncReadMem(depth, Vec(width/8, UInt(8.W)))
  io.dataOut := DontCare
  when(io.cen) {
    val rdwrPort = mem(io.addr)
    when (io.wen) {
      for(i <- 0 until width/8) {
        when (io.mask.asBools(i)) {
          rdwrPort(i) := io.dataIn((i+1)*8-1,i*8)
        }
      }
    }.otherwise { io.dataOut := rdwrPort.asUInt }
  }

  def init() = {
    io.cen := false.B
    io.wen := false.B
    io.addr := 0.U
    io.mask := 0.U
    io.dataIn := 0.U
  }
}
object RAM {
  def apply(depth: Int, width: Int): RAM = {
    Module(new RAM(depth, width))
  }
}
