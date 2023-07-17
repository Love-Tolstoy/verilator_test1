package device

import chisel3._
import chisel3.util._
import core.Tool.saveInit

class MEM extends BlackBox with HasBlackBoxPath {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val reset  = Input(Bool())
    val en = Input(Bool())
    val rw = Input(Bool())      // true: r ,false: w
    val addr = Input(UInt(64.W))
    val len  = Input(UInt(4.W))
    val dataIn = Input(UInt(64.W))
    val dataOut = Output(UInt(64.W))
    val valid = Output(Bool())
  })
  addPath("./src/vsrc/MEM.v")
}

// class MEMImm extends BlackBox with HasBlackBoxPath {
//   val io = IO(new Bundle {
//     val en = Input(Bool())
//     val rw = Input(Bool())
//     val addr = Input(UInt(64.W))
//     val len  = Input(UInt(4.W))
//     val dataIn = Input(UInt(64.W))
//     val dataOut = Output(UInt(64.W))
//     val valid = Output(Bool())
//   })
//   addPath("./src/vsrc/MEM_imm.v")
// }

class AXI4IF extends Module {
  val bus = IO(new Bundle {
    val readAddr = Flipped(Decoupled(new AXI4Lite_xAddrIO))
    val readData = Decoupled(new AXI4Lite_RDataIO)
  })
  val get_addr = RegInit(false.B)

  get_addr := MuxCase(get_addr, Array(
    bus.readAddr.fire -> true.B,
    bus.readData.fire -> false.B,
  ))

  val mem = withClockAndReset(clock, reset) {
    Module(new MEM())
  }
  mem.io.clock := clock
  mem.io.reset := reset
  mem.io.en := get_addr
  mem.io.rw := true.B
  mem.io.len := 8.U

  bus.readAddr.ready := Mux(
    !get_addr && bus.readAddr.valid, true.B, false.B)
  mem.io.addr := bus.readAddr.bits.addr

  bus.readData.valid := Mux(
    get_addr && bus.readData.ready && mem.io.valid, true.B, false.B)
  bus.readData.bits.data := mem.io.dataOut
  bus.readData.bits.prot := 0.U
}

class AXI4MEM extends Module {
  val bus = IO(new AXI4M_SlaveIO)

  // fifo
  // val inRAddr = Queue(bus.readAddr, 1)
  // val inWAddr = Queue(bus.writeAddr, 1)
  // val inWData = Queue(bus.writeData, 1)

  // save reg
  val addrReg = RegInit(0.U(64.W))
  val dataInReg = RegInit(0.U(64.W))
  val strbReg = RegInit(0.U(8.W))

  val mem = withClockAndReset(clock, reset) {
    Module(new MEM)
  }
  val rw = Wire(Bool())
  mem.io.clock := clock
  mem.io.reset := reset

  val nu :: rd :: rwait :: wd :: wr :: wwait :: Nil = Enum(6)
  val state = RegInit(nu)
  state := MuxCase(state, Array(
    (state === nu && bus.ra.fire) -> rd,
    (state === rd && bus.r.fire) -> nu,
    (state === rd && !bus.r.fire && mem.io.valid) -> rwait,
    (state === rwait && bus.r.fire) -> nu,
    (state === nu && bus.wa.fire) -> wd,
    (state === wd && bus.w.fire) -> wr,
    (state === wr && bus.b.fire) -> nu,
    (state === wr && !bus.b.fire && mem.io.valid) -> wwait,
    (state === wwait && bus.b.fire) -> nu,
  ))
  val axi4Id = RegInit(0.U)
  axi4Id := MuxCase(axi4Id, Array(
    bus.ra.fire -> bus.ra.bits.id,
    bus.wa.fire -> bus.wa.bits.id,
  ))

  addrReg := MuxCase(addrReg, Array(
    bus.ra.fire -> bus.ra.bits.addr,
    bus.wa.fire -> bus.wa.bits.addr,
  ))
  dataInReg := Mux(bus.w.fire, bus.w.bits.data, dataInReg)
  strbReg := Mux(bus.w.fire, bus.w.bits.strb, strbReg)

  mem.io.en := (bus.ra.fire || state === rd ||
    bus.w.fire || state === wr)
  rw := !(bus.w.fire || state === wr)
  mem.io.rw := rw

  // read
  bus.ra.ready := (state === nu && bus.ra.valid)

  bus.r.valid := ((state === rd && mem.io.valid) || state === rwait)
  bus.r.bits.data := mem.io.dataOut
  bus.r.bits.rid := axi4Id
  bus.r.bits.rlast := false.B

  // write
  bus.wa.ready := (state === nu && bus.wa.valid 
    && !bus.ra.valid)

  bus.w.ready := (state === wd && bus.w.valid)

  bus.b.valid := ((state === wr && mem.io.valid) || state === wwait)
  bus.b.bits.resp := 0.U
  bus.b.bits.bid := axi4Id

  val addr = Mux(rw,
    Mux(bus.ra.fire, bus.ra.bits.addr, addrReg),
    Mux(bus.wa.fire, bus.wa.bits.addr, addrReg))
  val strb = Mux(bus.w.fire, bus.w.bits.strb, strbReg)
  // val data = MuxLookup(addr(2, 0), )
  mem.io.len := MuxCase(0.U, Array(
    // rw -> 8.U,
    (rw && bus.ra.bits.size === 0.U) -> 1.U,
    (rw && bus.ra.bits.size === 1.U) -> 2.U,
    (rw && bus.ra.bits.size === 2.U) -> 4.U,
    (rw && bus.ra.bits.size === 3.U) -> 8.U,

    (!rw && bus.wa.bits.size === 0.U) -> 1.U,
    (!rw && bus.wa.bits.size === 1.U) -> 2.U,
    (!rw && bus.wa.bits.size === 2.U) -> 4.U,
    (!rw && bus.wa.bits.size === 3.U) -> 8.U,
  ))
  mem.io.addr := addr
  mem.io.dataIn := Mux(bus.w.fire,
    bus.w.bits.data, dataInReg)
}

/*
 * unsupport unaligned transfers
 * unsupport burst
 */
class AXI4MEM2 extends AXI4M_Slave {
  val mem = withClockAndReset(clock, reset) {
    Module(new MEM)
  }
  mem.io.clock := clock
  mem.io.reset := reset
  mem.io.en := false.B
  mem.io.rw := true.B
  mem.io.addr := 0.U
  mem.io.len := 0.U
  mem.io.dataIn := 0.U

  val addr = saveInit(0.U(64.W))
  val strb = saveInit(0.U(8.W))

  addr := MuxCase(addr, Array(
    bus.ra.fire -> bus.ra.bits.addr,
    bus.wa.fire -> bus.wa.bits.addr,
  ))
  strb := Mux(bus.w.fire, bus.w.bits.strb, strb)

  // mem.io.en := true.B
  mem.io.addr := addr
  mem.io.len := Mux(mem.io.rw,
    MuxCase(0.U, Array(
      (bus.ra.bits.size === 0.U) -> 1.U,
      (bus.ra.bits.size === 1.U) -> 2.U,
      (bus.ra.bits.size === 2.U) -> 4.U,
      (bus.ra.bits.size === 3.U) -> 8.U,
    )),
    MuxCase(0.U, Array(
      (bus.wa.bits.size === 0.U) -> 1.U,
      (bus.wa.bits.size === 1.U) -> 2.U,
      (bus.wa.bits.size === 2.U) -> 4.U,
      (bus.wa.bits.size === 3.U) -> 8.U,
    ))
  )

  switch(bus_state) {
    is(bus_r) {
      mem.io.en := true.B
      mem.io.rw := true.B
      when(mem.io.valid) {
        bus_dataIn := mem.io.dataOut
        r_valid := true.B
      }
    }
    is(bus_w) {
      mem.io.rw := false.B
      when(bus.w.fire) {
        mem.io.en := true.B
        mem.io.dataIn := bus_dataOut
      }
    }
    is(bus_b) {
      mem.io.rw := false.B
      when(mem.io.valid) {
        b_valid := true.B
      }
    }
  }
}
