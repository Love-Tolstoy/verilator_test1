package device

import chisel3._
import chisel3.util._

class AXI4M_AXIO extends Bundle {
  val id = UInt(4.W)
  val addr = UInt(64.W)
  val burst = UInt(2.W)
  val len = UInt(8.W)
  val size = UInt(3.W)
}

class AXI4M_RIO extends Bundle {
  val rid = UInt(4.W)
  val data = UInt(64.W)
  val rlast = Bool()
}

class AXI4M_WIO extends Bundle {
  val data = UInt(64.W)
  val strb = UInt(8.W)
  val wlast = Bool()
}

class AXI4M_BIO extends Bundle {
  val bid = UInt(4.W)
  val resp = UInt(2.W)
}

class AXI4M_MasterIO extends Bundle {
  val ra = Decoupled(new AXI4M_AXIO)
  val r = Flipped(Decoupled(new AXI4M_RIO))
  val wa = Decoupled(new AXI4M_AXIO)
  val w = Decoupled(new AXI4M_WIO)
  val b = Flipped(Decoupled(new AXI4M_BIO))

  def init() = {
    ra.valid    := false.B
    ra.bits     := 0.U.asTypeOf(new AXI4M_AXIO)
    r.ready     := false.B
    wa.valid    := false.B
    wa.bits     := 0.U.asTypeOf(new AXI4M_AXIO)
    w.valid     := false.B
    w.bits      := 0.U.asTypeOf(new AXI4M_WIO)
    b.ready     := false.B
  }

  def init(id: UInt, burst: UInt, len: UInt, size: UInt,
    addr: UInt, data: UInt, strb: UInt, last: Bool) = {
    ra.bits.id    := id
    ra.bits.addr  := addr
    ra.bits.burst := burst
    ra.bits.len   := len
    ra.bits.size  := size
    wa.bits.id    := id
    wa.bits.addr  := addr
    wa.bits.burst := burst
    wa.bits.len   := len
    wa.bits.size  := size
    w.bits.data   := data
    w.bits.strb   := strb
    w.bits.wlast  := last
  }
}

class AXI4M_SlaveIO extends Bundle {
  val ra = Flipped(Decoupled(new AXI4M_AXIO))
  val r = Decoupled(new AXI4M_RIO)
  val wa = Flipped(Decoupled(new AXI4M_AXIO))
  val w = Flipped(Decoupled(new AXI4M_WIO))
  val b = Decoupled(new AXI4M_BIO)

  def init() = {
    ra.ready    := false.B
    r.valid     := false.B
    r.bits      := 0.U.asTypeOf(new AXI4M_RIO)
    wa.ready    := false.B
    w.ready     := false.B
    b.valid     := false.B
    b.bits      := 0.U.asTypeOf(new AXI4M_BIO)
  }

  def init(id: UInt, resp: UInt, last: Bool) = {
    r.bits.rid    := id
    // r.bits.data   := dataIn
    r.bits.rlast  := last
    // dataOut       := w.bits.data
    b.bits.bid    := id
    b.bits.resp   := resp
  }
}

