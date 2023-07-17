// AXI4-lite BUS
package device

import chisel3._
import chisel3.util._

class AXI4Lite_xAddrIO extends Bundle {
  val addr = UInt(64.W)
  val prot = UInt(3.W)
  val id = UInt(4.W)
}

// class AXI4Lite_WADDR extends Bundle {
//   val addr = UInt(64.W)
//   val prot = UInt(3.W)
//   val id = UInt(4.W)
// }

class AXI4Lite_RDataIO extends Bundle {
  val data = UInt(64.W)
  val prot = UInt(3.W)
  val rid = UInt(4.W)
}

class AXI4Lite_WDataIO extends Bundle {
  val data = UInt(64.W)
  val strb = UInt(8.W)
}

class AXI4Lite_WRespIO extends Bundle {
  val resp = UInt(2.W)
  val bid = UInt(4.W)
}

class AXI4Lite_MasterIO extends Bundle {
  val readAddr = Decoupled(new AXI4Lite_xAddrIO)
  val readData = Flipped(Decoupled(new AXI4Lite_RDataIO))
  val writeAddr = Decoupled(new AXI4Lite_xAddrIO)
  val writeData = Decoupled(new AXI4Lite_WDataIO)
  val writeResp = Flipped(Decoupled(new AXI4Lite_WRespIO))
}

class AXI4Lite_SlaveIO extends Bundle {
  val readAddr = Flipped(Decoupled(new AXI4Lite_xAddrIO))
  val readData = Decoupled(new AXI4Lite_RDataIO)
  val writeAddr = Flipped(Decoupled(new AXI4Lite_xAddrIO))
  val writeData = Flipped(Decoupled(new AXI4Lite_WDataIO))
  val writeResp = Decoupled(new AXI4Lite_WRespIO)
}

class AXI4Lite_Arbiter extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(2, new AXI4Lite_MasterIO))
    val out = Flipped(new AXI4Lite_SlaveIO)
    val chosen = Output(UInt(2.W))
  })
  // read addr conflict (main)
  val arbiter1 = Module(new Arbiter(new AXI4Lite_xAddrIO, 2))
  arbiter1.io.in(0) <> io.in(0).readAddr
  arbiter1.io.in(1) <> io.in(1).readAddr
  arbiter1.io.in(0).bits.id := 0.U
  arbiter1.io.in(1).bits.id := 1.U
  io.out.readAddr <> arbiter1.io.out
  io.chosen := arbiter1.io.chosen

  val arbiter2 = Module(new Arbiter(new AXI4Lite_xAddrIO, 2))
  arbiter2.io.in(0) <> io.in(0).writeAddr
  arbiter2.io.in(1) <> io.in(1).writeAddr
  arbiter2.io.in(0).bits.id := 0.U
  arbiter2.io.in(1).bits.id := 1.U
  io.out.writeAddr <> arbiter2.io.out
  val arbiter3 = Module(new Arbiter(new AXI4Lite_WDataIO, 2))
  arbiter3.io.in(0) <> io.in(0).writeData
  arbiter3.io.in(1) <> io.in(1).writeData
  io.out.writeData <> arbiter3.io.out

  // return
  io.out.readData <> io.in(1).readData
  io.out.writeResp <> io.in(1).writeResp
  io.out.readData <> io.in(0).readData
  io.out.writeResp <> io.in(0).writeResp
  io.out.readData.ready := Mux(io.out.readData.bits.rid === 0.U,
    io.in(0).readData.ready, io.in(1).readData.ready)
  io.out.writeResp.ready := Mux(io.out.writeResp.bits.bid === 0.U,
    io.in(0).writeResp.ready, io.in(1).writeResp.ready)
}

object AXI4Lite {
  def initMaster(m: AXI4Lite_MasterIO) = {
    m.writeAddr.valid := false.B
    m.writeAddr.bits.addr := 0.U
    m.writeAddr.bits.prot := 0.U
    m.writeAddr.bits.id := 0.U
    m.writeData.valid := false.B
    m.writeData.bits.data := 0.U
    m.writeData.bits.strb := 0.U
    m.writeResp.ready := false.B
    m.readAddr.valid := false.B
    m.readAddr.bits.addr := 0.U
    m.readAddr.bits.prot := 0.U
    m.readAddr.bits.id := 0.U
    m.readData.ready := false.B
  }
  def connect(m: AXI4Lite_MasterIO, s: AXI4Lite_SlaveIO) {
    m.writeAddr.valid := s.writeAddr.valid
    s.writeAddr.ready := m.writeAddr.ready
    m.writeAddr.bits := s.writeAddr.bits
    m.writeData.valid := s.writeData.valid
    s.writeData.ready := m.writeData.ready
    m.writeData.bits := s.writeData.bits
    s.writeResp.valid := m.writeResp.valid
    m.writeResp.ready := s.writeResp.ready
    s.writeResp.bits := m.writeResp.bits
    m.readAddr.valid := s.readAddr.valid
    s.readAddr.ready := m.readAddr.ready
    m.readAddr.bits := s.readAddr.bits
    s.readData.valid := m.readData.valid
    m.readData.ready := s.readData.ready
    s.readData.bits := m.readData.bits
  }
  // def connect(m: AXI4Lite_Master, s: AXI4Lite_Slave) {
  //   m.writeAddr <> s.writeAddr
  //   m.writeData <> s.writeData
  //   m.writeResp <> s.writeResp
  //   m.readAddr <> s.readAddr
  //   m.readData <> s.readData
  // }
}

class AXI4Lite_Slave extends Module {
  val bus = IO(new AXI4Lite_SlaveIO)
}
