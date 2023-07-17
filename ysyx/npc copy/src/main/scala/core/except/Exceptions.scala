package core.except

import chisel3._

import core._
import core.CSRSNumber._
import device.CLINT
import Tool._

class Exceptions extends Module {
  val io = IO(new Bundle {
    // val en = Input(Bool())
    val en = Input(Bool())
    val ecode = Input(UInt(64.W))

    val epc = Input(UInt(64.W))   // Expect
    // val dnpc = Input(UInt(64.W))  // Intr

    val mret = Input(Bool())

    val csrsIn = Input(Vec(10, UInt(64.W)))
    val csrsOut = Output(Vec(10, UInt(64.W)))

    // val exceptOut = Output(new ExceptCSRIO)
    val exceptPc = Output(UInt(64.W))
    val raiseIntr = Output(Bool())
  })
  io.csrsOut := io.csrsIn

  def csrsRead(index: UInt) = io.csrsIn(CSRS.mapIndex(index))
  def csrsWrite(index: UInt) = io.csrsOut(CSRS.mapIndex(index))

  io.exceptPc := csrsRead(Mtvec)(63, 2) ## 0.U(2.W)
  io.raiseIntr := false.B

  when(io.en && csrsRead(Mstatus)(3) === 1.U) {
    csrsWrite(Mstatus) := changeBits(
      changeBits(
        changeBits(csrsRead(Mstatus), 7, csrsRead(Mstatus)(3)),
        3, 0.U
      ),
      12, 11, 3.U
    )
    // csrsWrite(Mstatus)(7) := csrsRead(Mstatus)(3)
    // csrsWrite(Mstatus)(3) := 0.U    // mie
    // csrsWrite(Mstatus)(12, 11) := 3.U

    csrsWrite(Mcause) := io.ecode
    csrsWrite(Mepc) := io.epc

    // io.raiseIntr := true.B

    when(io.ecode === 11.U) { // ecall
      // printf("got ecall\n")
      io.raiseIntr := true.B
    }.elsewhen(io.ecode === CLINT.ecode && csrsRead(Mie)(7).asBool) {  // time intr
      // printf("got time intr\n")
      // csrsWrite(Mepc) := io.dnpc
      csrsWrite(Mip) := changeBits(csrsRead(Mip), 7, 1.U)
      io.raiseIntr := true.B
    }
  }

  when(io.mret) {
    // printf("got mret %n\n",csrsRead(Mstatus))
    csrsWrite(Mip) := changeBits(csrsRead(Mip), 7, 0.U)
    csrsWrite(Mstatus) :=
      changeBits(csrsRead(Mstatus), 3, csrsRead(Mstatus)(7))
  }
}