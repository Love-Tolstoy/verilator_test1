package core.stages

import chisel3._
import chisel3.util._

import core.unit._
import device._
import core.cache._

class IFU extends IfUnit {
  val io = IO(new Bundle {
    val pc = Output(UInt(64.W))  // branch
    val inst = Output(UInt(32.W))

    val bus = new AXI4M_MasterIO
    val hit = Output(Bool())    // record hit times
  })

  val icache = Module(new MMIOControl(CacheConfig()))

  val s_icache :: s_waitCon :: Nil = Enum(2)
  val state = RegInit(s_icache)
  state := MuxCase(state, Array(
    (state === s_icache && icache.io.over && pipe.con.fire) -> s_icache,
    (state === s_icache && icache.io.over && !pipe.con.fire) -> s_waitCon,
    (state === s_waitCon && pipe.con.fire) -> s_icache,
  ))

  icache.io.en := (state === s_icache)
  icache.io.wen := false.B
  icache.io.fencei := false.B
  icache.io.addr := pipe.in.pc
  icache.io.strb := 0.U
  icache.io.dataIn := 0.U
  icache.io.size := 2.U
  icache.io.bus <> io.bus

  val icacheDataOut = Wire(UInt(64.W))
  val icacheDataOutSave = RegNext(icacheDataOut, 0.U(64.W))
  icacheDataOut := Mux(icache.io.over, icache.io.dataOut, icacheDataOutSave)

  memDataOut := icacheDataOut

  taskOver := icache.io.over || state === s_waitCon

  io.pc := pipe.in.pc
  io.inst := memDataOut(31,0)

  // record
  io.hit := icache.io.over
}
