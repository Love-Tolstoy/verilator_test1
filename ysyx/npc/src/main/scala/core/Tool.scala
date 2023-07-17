package core

import chisel3._
import chisel3.util._

object Tool {
  // 64len
  def sext(x:UInt, len:Int) = {
    Fill(64-len, x(len-1)) ## x(len-1, 0)
  }
  def uext(x:UInt, len:Int) = {
    Fill(64-len, 0.U) ## x(len-1, 0)
  }
  // def Persistence[T <: Data](typ: T, cond: Bool, data: T): T = {
  //   val wire = Wire(typ)
  //   val reg = Reg(typ)
  //   reg := Mux(cond, data, reg)
  //   wire := Mux(cond, data, reg)
  //   wire
  // }
  // def Persistence[T <: Data](typ: T, next: T): T = {
  //   val wire = Wire(typ)
  //   val reg = Reg(typ)
  //   reg := next
  //   wire := next
  //   wire
  // }
  def saveInit[T <: Data](init: T): T = {
    val wire = WireInit(init)
    val reg = RegNext(wire, init)
    wire := reg
    wire
  }
  //  64len
  def changeBits(data: UInt, idx: Int, bit: UInt) = {
    assert(idx > 0 && idx < 63)
    data(63, idx+1) ## bit ## data(idx-1, 0)
  }
  def changeBits(data: UInt, ia: Int, ib: Int, bit: UInt) = {
    assert(ia-ib > 0 && ia-ib < 63)
    data(63, ia+1) ## bit ## data(ib-1, 0)
  }
}
