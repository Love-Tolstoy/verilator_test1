package core.cache

import chisel3._
import chisel3.util._

import device._
// import core

class UNCache extends AXI4M_Master {
  val io = IO(new Bundle {
    val en = Input(Bool())
    val wen = Input(Bool())
    val addr = Input(UInt(64.W))

    val strb = Input(UInt(8.W)) // write
    val dataIn = Input(UInt(64.W))

    val size = Input(UInt(2.W))

    val over = Output(Bool())
    val dataOut = Output(UInt(64.W))
  })

  val offset = io.addr(2, 0)
  val dataIn = MuxLookup(offset, io.dataIn, Array(
    0.U -> io.dataIn(63,0),
    1.U -> io.dataIn(55,0) ## 0.U(8.W),
    2.U -> io.dataIn(47,0) ## 0.U(16.W),
    3.U -> io.dataIn(39,0) ## 0.U(24.W),
    4.U -> io.dataIn(31,0) ## 0.U(32.W),
    5.U -> io.dataIn(23,0) ## 0.U(40.W),
    6.U -> io.dataIn(15,0) ## 0.U(48.W),
    7.U -> io.dataIn(7,0) ## 0.U(56.W),
  ))

  bus_id := 2.U
  bus_addr := io.addr
  bus_data := dataIn
  bus_strb := io.strb << offset
  bus_size := io.size

  bus_read := io.en && !io.wen
  bus_write := io.en && io.wen

  io.over := bus_read_over || bus_write_over
  // io.dataOut := bus.r.bits.data// >> offset
  io.dataOut := MuxLookup(offset, bus.r.bits.data, Array(
    0.U -> bus.r.bits.data(63, 0),
    1.U -> bus.r.bits.data(63, 8),
    2.U -> bus.r.bits.data(63, 16),
    3.U -> bus.r.bits.data(63, 24),
    4.U -> bus.r.bits.data(63, 32),
    5.U -> bus.r.bits.data(63, 40),
    6.U -> bus.r.bits.data(63, 48),
    7.U -> bus.r.bits.data(63, 56),
  ))
}
