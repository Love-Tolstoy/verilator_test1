package device

import chisel3._
import chisel3.util._
import core.Tool.saveInit

/**
  * 64bit bus
  * unsuport: unaligned transfers
  *
  */
class AXI4M_Slave extends Module {
  val bus = IO(new AXI4M_SlaveIO)

  bus.init()

  val bus_ready :: bus_r :: bus_w :: bus_b :: Nil = Enum(4)
  val bus_state = RegInit(bus_ready)
  bus_state := MuxCase(bus_state, Array(
    (bus_state === bus_ready && bus.ra.fire) -> bus_r,
    (bus_state === bus_r && bus.r.fire) -> bus_ready,
    (bus_state === bus_ready && bus.wa.fire) -> bus_w,
    (bus_state === bus_w && bus.w.fire) -> bus_b,
    (bus_state === bus_b && bus.b.fire) -> bus_ready,
  ))

  // need set
  val bus_dataIn = saveInit(0.U(64.W))
  val bus_resp = saveInit(0.U(2.W))
  val r_valid = saveInit(false.B)
  val b_valid = saveInit(false.B)
  // output
  val bus_dataOut = WireInit(0.U(64.W))

  val bus_count = RegInit(0.U(8.W))
  bus_count := MuxCase(bus_count, Array(
    bus.ra.fire -> bus.ra.bits.len,
    bus.wa.fire -> bus.wa.bits.len,
    (bus_count =/= 0.U) -> (bus_count - 1.U),
  ))

  val bus_id = saveInit(0.U(4.W))
  bus_id := MuxCase(bus_id, Array(
    bus.ra.fire -> bus.ra.bits.id,
    bus.wa.fire -> bus.wa.bits.id,
  ))
  val bus_last = bus_count === 0.U

  // connect
  bus.ra.ready := bus_state === bus_ready
  bus.r.valid := r_valid
  bus.wa.ready := bus_state === bus_ready
  bus.w.ready := bus_state === bus_w
  bus.b.valid := b_valid

  bus.init(bus_id, bus_resp, bus_last)

  val offset = bus.ra.bits.addr(2, 0)
  bus.r.bits.data := MuxLookup(offset, bus_dataIn, Array(
    0.U -> bus_dataIn(63,0),
    1.U -> bus_dataIn(55,0) ## 0.U(8.W),
    2.U -> bus_dataIn(47,0) ## 0.U(16.W),
    3.U -> bus_dataIn(39,0) ## 0.U(24.W),
    4.U -> bus_dataIn(31,0) ## 0.U(32.W),
    5.U -> bus_dataIn(23,0) ## 0.U(40.W),
    6.U -> bus_dataIn(15,0) ## 0.U(48.W),
    7.U -> bus_dataIn(7,0) ## 0.U(56.W),
  ))
  bus_dataOut := MuxLookup(offset, bus.w.bits.data, Array(
    0.U -> bus.w.bits.data(63, 0),
    1.U -> bus.w.bits.data(63, 8),
    2.U -> bus.w.bits.data(63, 16),
    3.U -> bus.w.bits.data(63, 24),
    4.U -> bus.w.bits.data(63, 32),
    5.U -> bus.w.bits.data(63, 40),
    6.U -> bus.w.bits.data(63, 48),
    7.U -> bus.w.bits.data(63, 56),
  ))
}