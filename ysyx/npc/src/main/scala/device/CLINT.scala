package device

import chisel3._
import chisel3.util._

class CLINT extends Module {
  val io = IO(new Bundle {
    val mtie = Input(Bool())

    val rwio = new CLINTRWIO

    val raiseIntr = Output(Bool())
  })

  val mtime = RegInit(0.U(64.W))
  val mtimecmp = RegInit(0.U(64.W))

  mtime := MuxCase(mtime, Array(
    io.rwio.mtimeWen -> io.rwio.mtimeIn,
    io.mtie -> (mtime + 1.U),
  ))

  mtimecmp := MuxCase(mtimecmp, Array(
    io.rwio.mtimecmpWen -> io.rwio.mtimecmpIn,
  ))

  io.rwio.mtimeOut := mtime
  io.rwio.mtimecmpOut := mtimecmp
  io.raiseIntr := io.mtie && (mtime >= mtimecmp)
}
object CLINT {
  val ecode = "x8000000000000007".U(64.W)
}

class CLINTRWIO extends Bundle {
  val mtimeWen = Input(Bool())
  val mtimeIn = Input(UInt(64.W))
  val mtimeOut = Output(UInt(64.W))

  val mtimecmpWen = Input(Bool())
  val mtimecmpIn = Input(UInt(64.W))
  val mtimecmpOut = Output(UInt(64.W))
}
