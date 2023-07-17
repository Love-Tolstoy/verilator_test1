package device

import chisel3._
import chisel3.util._

class AXI4_Crossbar extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(2, new AXI4M_MasterIO))
    val out = Flipped(new AXI4M_SlaveIO)
  })

  val s_0 :: s_1 :: Nil = Enum(2)
  val ar_state = RegInit(s_0)
  val aw_state = RegInit(s_0)

  ar_state := MuxCase(ar_state, Array(
    (io.in(0).ra.valid && !io.in(1).ra.valid) -> s_0,
    (!io.in(0).ra.valid && io.in(1).ra.valid) -> s_1,
  ))
  val w0valid = io.in(0).wa.valid || io.in(0).w.valid
  val w1valid = io.in(1).wa.valid || io.in(1).w.valid
  aw_state := MuxCase(aw_state, Array(
    (w0valid && !w1valid) -> s_0,
    (!w0valid && w1valid) -> s_1,
  ))

  def distrInit[T <: DecoupledIO[Data]](din0: T, din1: T, dout: T) = {
    dout.valid := false.B
    dout.bits := din0.bits
    din0.ready := false.B
    din1.ready := false.B
  }

  // M --> S
  distrInit(io.in(0).ra, io.in(1).ra, io.out.ra)
  for (i <- 0 to 1) {
    when(ar_state === i.U) {
      io.out.ra.valid := io.in(i).ra.valid
      io.out.ra.bits := io.in(i).ra.bits
      io.out.ra.bits.id := i.U
      io.in(i).ra.ready := io.out.ra.ready
    }
  }
  distrInit(io.in(0).wa, io.in(1).wa, io.out.wa)
  distrInit(io.in(0).w, io.in(1).w, io.out.w)
  for (i <- 0 to 1) {
    when(aw_state === i.U) {
      io.out.wa.valid := io.in(i).wa.valid
      io.out.wa.bits := io.in(i).wa.bits
      io.out.wa.bits.id := i.U
      io.in(i).wa.ready := io.out.wa.ready

      io.out.w.valid := io.in(i).w.valid
      io.out.w.bits := io.in(i).w.bits
      io.in(i).w.ready := io.out.w.ready
    }
  }

  // M <-- S
  io.out.r.ready := Mux(io.out.r.bits.rid === 1.U,
    io.in(1).r.ready, io.in(0).r.ready)
  io.out.b.ready := Mux(io.out.b.bits.bid === 1.U,
    io.in(1).b.ready, io.in(0).b.ready)

  for (i <- 0 to 1) {
    io.in(i).r.bits := io.out.r.bits
    io.in(i).b.bits := io.out.b.bits
    io.in(i).r.valid := io.out.r.valid && io.out.r.bits.rid === i.U
    io.in(i).b.valid := io.out.b.valid && io.out.b.bits.bid === i.U
  }
}

class AXI4M_Arbiter extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(2, new AXI4M_MasterIO))
    val out = Flipped(new AXI4M_SlaveIO)
  })

  // M --> S
  val arbRA = Module(new Arbiter(new AXI4M_AXIO, 2))
  arbRA.io.in(0) <> io.in(0).ra
  arbRA.io.in(1) <> io.in(1).ra
  arbRA.io.in(0).bits.id := 0.U
  arbRA.io.in(1).bits.id := 1.U
  io.out.ra <> arbRA.io.out

  val arbWA = Module(new Arbiter(new AXI4M_AXIO, 2))
  arbWA.io.in(0) <> io.in(0).wa
  arbWA.io.in(1) <> io.in(1).wa
  arbWA.io.in(0).bits.id := 0.U
  arbWA.io.in(1).bits.id := 1.U
  io.out.wa <> arbWA.io.out

  // val arbW = Module(new Arbiter(new AXI4M_WIO, 2))
  // arbW.io.in(0) <> io.in(0).w
  // arbW.io.in(1) <> io.in(1).w
  // io.out.w <> arbW.io.out
  io.out.w.valid := false.B
  io.out.w.bits := 0.U.asTypeOf(new AXI4M_WIO)
  io.in(0).w.ready := io.out.w.ready
  io.in(1).w.ready := io.out.w.ready
  when(arbWA.io.chosen === 0.U) {
    io.out.w <> io.in(0).w
  }.otherwise {
    io.out.w <> io.in(1).w
  }

  io.out.r.ready := Mux(io.out.r.bits.rid === 1.U,
    io.in(1).r.ready, io.in(0).r.ready)
  io.out.b.ready := Mux(io.out.b.bits.bid === 1.U,
    io.in(1).b.ready, io.in(0).b.ready)

  // M <-- S
  io.in(0).r.bits := io.out.r.bits
  io.in(1).r.bits := io.out.r.bits
  io.in(0).b.bits := io.out.b.bits
  io.in(1).b.bits := io.out.b.bits

  io.in(0).r.valid := io.out.r.valid && io.out.r.bits.rid === 0.U
  io.in(1).r.valid := io.out.r.valid && io.out.r.bits.rid === 1.U
  io.in(0).b.valid := io.out.b.valid && io.out.b.bits.bid === 0.U
  io.in(1).b.valid := io.out.b.valid && io.out.b.bits.bid === 1.U
}

