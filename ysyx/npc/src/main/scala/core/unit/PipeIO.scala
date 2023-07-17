package core.unit

import chisel3._
import core.RVCommand
import chisel3.util.is

class PipeIO extends Bundle {}

class PC_IFPipeIO extends PipeIO {
  val pc = UInt(64.W)
}
object PC_IFPipeIO {
  def apply(pc: UInt): PC_IFPipeIO = {
    val bundle = Wire(new PC_IFPipeIO)
    bundle.pc := pc
    bundle
  }
}

class IF_IDPipeIO extends PipeIO {
  val inst = UInt(32.W)

  val pc = UInt(64.W)
  val isBubble = Bool()
}
object IF_IDPipeIO {
  def apply(inst: UInt, pc: UInt, isBubble: Bool = false.B): IF_IDPipeIO = {
    val w = Wire(new IF_IDPipeIO)
    w.inst := inst
    w.pc := pc
    w.isBubble := isBubble
    w
  }
}

class ID_EXPipeIO extends PipeIO {
  val comm = RVCommand()
  val dest = UInt(64.W)
  val src1 = UInt(64.W)
  val src2 = UInt(64.W)
  val regWen = Bool()

  val pc = UInt(64.W)
  val isBubble = Bool()
}
object ID_EXPipeIO {
  def apply(comm: Data, dest: UInt, src1: UInt, src2: UInt,
    regWen: Bool, pc: UInt, isBubble: Bool = false.B): ID_EXPipeIO = {
    val bundle = Wire(new ID_EXPipeIO)
    bundle.comm := comm
    bundle.dest := dest
    bundle.src1 := src1
    bundle.src2 := src2
    bundle.regWen := regWen
    bundle.pc := pc
    bundle.isBubble := isBubble
    bundle
  }
}

class EX_MEPipeIO extends PipeIO {
  val memEn = Bool()        // r,w
  val memRw = Bool()
  val comm = RVCommand()      // r,w
  val memAddr = UInt(64.W)  // r,w
  val memData = UInt(64.W)  // w
  val regDest = UInt(5.W)   // r
  val regData = UInt(64.W)

  val pc = UInt(64.W)
  // val isBubble = Bool()
}
object EX_MEPipeIO {
  def apply(memEn: Bool, memRw: Bool, comm: Data,
    memAddr: UInt, memData: UInt, regDest: UInt,
    regData: UInt, pc: UInt): EX_MEPipeIO = {
    val bundle = Wire(new EX_MEPipeIO)
    bundle.memEn := memEn
    bundle.memRw := memRw
    bundle.comm := comm
    bundle.memAddr := memAddr
    bundle.memData := memData
    bundle.regDest := regDest
    bundle.regData := regData
    bundle.pc := pc
    // bundle.isBubble := isBubble
    bundle
  }
}

class ME_WBPipeIO extends PipeIO {
  val regDest = UInt(5.W)
  val regData = UInt(64.W)

  val pc = UInt(64.W)
}
object ME_WBPipeIO {
  def apply(regDest: UInt, regData: UInt, pc: UInt) = {
    val bundle = Wire(new ME_WBPipeIO)
    bundle.regDest := regDest
    bundle.regData := regData
    bundle.pc := pc
    bundle
  }
}
