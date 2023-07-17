package device

import chisel3._
import chisel3.util._

/**
  * size == 3 -> 64bit
  * master dont use last
  * TODO: burst
  * TODO: unaligned transfer or dont support
  */
class AXI4M_Master extends Module {
  val bus = IO(new AXI4M_MasterIO)

  bus.init()

  // setting
  val bus_size = WireInit(3.U(3.W))
  val bus_len = WireInit(0.U(8.W))

  // args
  val bus_id = WireInit(0.U(3.W))
  val bus_addr = WireInit(0.U(64.W))
  val bus_data = WireInit(0.U(64.W))
  val bus_strb = WireInit(0.U(8.W))

  val bus_burst = WireInit(1.U(2.W))

  // transaction
  val bus_write = WireInit(false.B)
  val bus_read = WireInit(false.B)

  // bus_ready = bus_xa
  val bus_reset :: bus_ready :: bus_rd :: bus_wd :: bus_br :: Nil = Enum(5)
  val bus_state = RegInit(bus_reset)
  bus_state := MuxCase(bus_state, Array(
    (bus_state === bus_reset) -> bus_ready,
    (bus_state === bus_ready && bus_write && bus.wa.fire && !bus.w.fire) -> bus_wd,
    (bus_state === bus_ready && bus_read && bus.ra.fire) -> bus_rd,
    (bus_state === bus_rd && bus.r.fire) -> bus_ready,
    (bus_state === bus_wd && bus.w.fire) -> bus_br,
    (bus_state === bus_ready && bus_write && bus.wa.fire && bus.w.fire) -> bus_br,  // aw,w over at same time
    (bus_state === bus_br && bus.b.fire) -> bus_ready,
  ))

  val bus_count = RegInit(0.U(8.W))
  bus_count := MuxCase(bus_count, Array(
    bus.wa.fire -> bus_len,
    bus.ra.fire -> bus_len,
    // (bus_count =/= 0.U) -> (bus_count - 1.U),
  ))

  // connect
  // val bus_burst = Mux(bus_len === 0.U, 0.U, 1.U)
  val bus_last = Mux(bus_burst === 1.U, bus_count === 0.U, false.B)
  bus.ra.valid := bus_state === bus_ready && bus_read && !bus_write
  bus.r.ready := bus_state === bus_rd
  bus.wa.valid := bus_state === bus_ready && bus_write
  bus.w.valid := bus_state === bus_wd || bus.wa.valid
  bus.b.ready := bus_state === bus_br

  bus.init(bus_id, bus_burst, bus_len, bus_size, bus_addr,
    bus_data, bus_strb, bus_last)

  // transaction over, output
  val bus_read_over = bus_state === bus_rd && bus.r.fire
  val bus_write_over = bus_state === bus_br && bus.b.fire
}