package core

import chisel3._
import chisel3.util._

import CSRSNumber._

object CSRSNumber {
  val Mstatus = "x300".U
  val Mtvec   = "x305".U
  val Mepc    = "x341".U
  val Mcause  = "x342".U
  val Mie     = "x304".U
  val Mip     = "x344".U
}

class CSRS(nums: Int) extends Module {
  val io = IO(new Bundle {
    // for EXU
    val rwio = new ExpectRWOneIO
    // for exception
    // val exceptWen = Input(Bool())
    // val exceptIn = Input(new ExceptCSRIO)
    // val exceptOut = Output(new ExceptCSRIO)
    val regsWen = Input(Bool())
    val regsIn = Input(Vec(nums, UInt(64.W)))
    val regsOut = Output(Vec(nums, UInt(64.W)))
  })

  val csrRegs = RegInit(VecInit({
    val a = Seq.fill(nums)(0.U(64.W))
    a.updated(1, "x0000000000000008".U)   // mstatus
  }))

  def getCSR(index: UInt): UInt = {
    csrRegs(CSRS.mapIndex(index))
  }

  when(io.regsWen) {
    csrRegs := io.regsIn
  }

  io.rwio.dataOut := getCSR(io.rwio.readIndex)
  getCSR(io.rwio.writeIndex) :=
    Mux(io.rwio.wen, io.rwio.dataIn, getCSR(io.rwio.writeIndex))

  csrRegs(0) := 0.U

  // when(io.exceptWen) {
  //   csrRegs(mapIndex("x300".U)) := io.exceptIn.mstatus
  //   csrRegs(mapIndex("x341".U)) := io.exceptIn.mepc
  //   csrRegs(mapIndex("x342".U)) := io.exceptIn.mcause
  // }

  // io.exceptOut.mstatus := csrRegs(mapIndex("x300".U))
  // io.exceptOut.mtvec := csrRegs(mapIndex("x305".U))
  // io.exceptOut.mepc := csrRegs(mapIndex("x341".U))
  // io.exceptOut.mcause := csrRegs(mapIndex("x342".U))

  io.regsOut := csrRegs
}
object CSRS {
  def mapIndex(index: UInt): UInt = {
    MuxCase(0.U, Array(
      (index === Mstatus) -> 1.U,
      (index === Mtvec)   -> 2.U,
      (index === Mepc)    -> 3.U,
      (index === Mcause)  -> 4.U,
      (index === Mie)     -> 5.U,
      (index === Mip)     -> 6.U,
    ))
  }
}

// class ExceptCSRIO extends Bundle {
//   val mstatus = UInt(64.W)
//   val mtvec = UInt(64.W)
//   val mepc = UInt(64.W)
//   val mcause = UInt(64.W)
// }

class ExpectRWOneIO extends Bundle {
  // read
  val readIndex = Input(UInt(12.W))
  val dataOut = Output(UInt(64.W))
  // write
  val wen = Input(Bool())
  val writeIndex = Input(UInt(12.W))
  val dataIn = Input(UInt(64.W))
}
