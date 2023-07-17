package core.stages

import chisel3._
import chisel3.util._

import core.unit._
import device._
import core.cache._
import core.RVCommand
import core.Param
import core.Tool._

class MEU extends MeUnit {
  val io = IO(new Bundle {
    // forward
    val regDest = Output(UInt(5.W))
    val regData = Output(UInt(64.W))

    // CLINT
    val clintio = Flipped(new CLINTRWIO)

    val bus = new AXI4M_MasterIO

    // fence.i flush pipeline
    val fencei = Output(Bool())
  })

  io.clintio.mtimeWen := false.B
  io.clintio.mtimeIn := io.clintio.mtimeOut
  io.clintio.mtimecmpWen := false.B
  io.clintio.mtimecmpIn := io.clintio.mtimecmpOut

  val clint = memAddr === Param.CLINT_MTIME_ADDR.U ||
    memAddr === Param.CLINT_MTIMECMP_ADDR.U
  val clintOut = WireInit(0.U(64.W))

  val size = WireInit(0.U(2.W))
  val strb = Wire(UInt(8.W))
  val len = Wire(UInt(4.W))
  val offset = Wire(UInt(3.W))

  val dcache = Module(new MMIOControl(CacheConfig()))
  dcache.io.en := false.B
  dcache.io.wen := !pipe.in.memRw
  dcache.io.fencei := false.B
  dcache.io.addr := 0.U
  dcache.io.strb := strb
  dcache.io.dataIn := 0.U
  dcache.io.size := size
  dcache.io.bus <> io.bus

  strb := MuxCase(0.U, Array(
    comm.isOneOf(RVCommand.lb,RVCommand.sb,RVCommand.lbu) -> "b00000001".U,
    comm.isOneOf(RVCommand.lh,RVCommand.sh,RVCommand.lhu) -> "b00000011".U,
    comm.isOneOf(RVCommand.lw,RVCommand.sw,RVCommand.lwu) -> "b00001111".U,
    comm.isOneOf(RVCommand.ld,RVCommand.sd) -> "b11111111".U,
  ))
  offset := memAddr(2, 0)
  len := MuxCase(0.U, Array(
    comm.isOneOf(RVCommand.lb,RVCommand.sb,RVCommand.lbu) -> 1.U,
    comm.isOneOf(RVCommand.lh,RVCommand.sh,RVCommand.lhu) -> 2.U,
    comm.isOneOf(RVCommand.lw,RVCommand.sw,RVCommand.lwu) -> 4.U,
    comm.isOneOf(RVCommand.ld,RVCommand.sd) -> 8.U,
  ))

  size := MuxCase(0.U, Array(
    comm.isOneOf(RVCommand.lb,RVCommand.sb,RVCommand.lbu) -> 0.U,
    comm.isOneOf(RVCommand.lh,RVCommand.sh,RVCommand.lhu) -> 1.U,
    comm.isOneOf(RVCommand.lw,RVCommand.sw,RVCommand.lwu) -> 2.U,
    comm.isOneOf(RVCommand.ld,RVCommand.sd) -> 3.U,
  ))

  val overflow = Wire(UInt(4.W))
  val s1 :: s_dcache2 :: s_waitCon :: Nil = Enum(3)
  val state = RegInit(s1)
  overflow := Mux(offset + len > 8.U, offset + len - 8.U, 0.U)
  state := MuxCase(state, Array(
    // (state === s1 && mmio && uncache.io.over && !pipe.con.fire) -> s_waitCon, // uncache
    (state === s1 && dcache.io.over && pipe.con.fire && overflow === 0.U) -> s1,
    (state === s1 && dcache.io.over && overflow =/= 0.U) -> s_dcache2,
    (state === s_dcache2 && dcache.io.over && pipe.con.fire) -> s1,
    (state === s_dcache2 && dcache.io.over && !pipe.con.fire) -> s_waitCon,
    (state === s1 && dcache.io.over && !pipe.con.fire && overflow === 0.U) -> s_waitCon,
    (state === s_waitCon && pipe.con.fire) -> s1,
  ))

  switch(state) {
    is(s1) {
      when(clint) {
        when(memAddr === Param.CLINT_MTIME_ADDR.U) {
          io.clintio.mtimeWen := pipe.in.memEn && !pipe.in.memRw
          io.clintio.mtimeIn := memData
          clintOut := io.clintio.mtimeOut
        }.elsewhen(memAddr === Param.CLINT_MTIMECMP_ADDR.U) {
          io.clintio.mtimecmpWen := pipe.in.memEn && !pipe.in.memRw
          io.clintio.mtimecmpIn := memData
          clintOut := io.clintio.mtimecmpOut
        }

        taskOver := true.B
      }.elsewhen(pipe.in.comm === RVCommand.fencei) {
        dcache.io.en := true.B
        dcache.io.fencei := true.B
        taskOver := dcache.io.over
      }.otherwise{
        dcache.io.en := pipe.in.memEn
        dcache.io.addr := memAddr
        dcache.io.strb := strb
        dcache.io.dataIn := memData

        taskOver := !pipe.in.memEn || (dcache.io.over && overflow === 0.U)
      }
    }
    is(s_dcache2) {
      dcache.io.en := pipe.in.memEn
      dcache.io.addr := memAddr(63,3) + 1.U ## 0.U(3.W)
      dcache.io.strb := MuxLookup(overflow, 0.U, Array(
        1.U -> "b00000001".U,
        2.U -> "b00000011".U,
        3.U -> "b00000111".U,
        4.U -> "b00001111".U,
        5.U -> "b00011111".U,
        6.U -> "b00111111".U,
        7.U -> "b01111111".U,
      ))
      dcache.io.dataIn := MuxLookup(8.U(4.W) - offset, memData, Array(
        1.U -> memData(63, 8),
        2.U -> memData(63, 16),
        3.U -> memData(63, 24),
        4.U -> memData(63, 32),
        5.U -> memData(63, 40),
        6.U -> memData(63, 48),
        7.U -> memData(63, 56),
      ))

      taskOver := dcache.io.over
    }
    is(s_waitCon) {
      taskOver := true.B
    }
  }

  val data = Wire(UInt(64.W))
  val dataReg = RegNext(data, 0.U(64.W))
  data := MuxCase(dataReg, Array(
    (state === s1 && clint) -> clintOut,  // clint
    // (state === s1 && mmio && uncache.io.over) -> uncache.io.dataOut,
    (state === s1 && dcache.io.over) -> dcache.io.dataOut,
    (state === s_dcache2 && dcache.io.over) -> dcache.io.dataOut ##
      MuxLookup(8.U(4.W) - offset, dataReg, Array(
        1.U -> dataReg(7,0),
        2.U -> dataReg(15,0),
        3.U -> dataReg(23,0),
        4.U -> dataReg(31,0),
        5.U -> dataReg(39,0),
        6.U -> dataReg(47,0),
        7.U -> dataReg(55,0),
      )),
    )
  )

  val regData = MuxLookup(comm.asUInt, pipe.in.regData, Array(
    RVCommand.lb.asUInt     -> sext(data, 8),
    RVCommand.lh.asUInt     -> sext(data, 16),
    RVCommand.lw.asUInt     -> sext(data, 32),
    RVCommand.ld.asUInt     -> data,
    RVCommand.lbu.asUInt    -> uext(data, 8),
    RVCommand.lhu.asUInt    -> uext(data, 16),
    RVCommand.lwu.asUInt    -> uext(data, 32),
  ))

  memDataOut := data

  // data forward
  io.regDest := Mux(pipe.in.memEn && (!pipe.in.memRw),
    0.U, pipe.in.regDest)
  io.regData := Mux(pipe.in.memEn, regData, pipe.in.regData)

  // pipeline flush
  io.fencei := pipe.in.comm === RVCommand.fencei
}
