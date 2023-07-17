package core

import chisel3._
import chisel3.util._

class HandShake extends Bundle {
  val lastValid = Input(Bool())
  val lastReady = Output(Bool())
  val nextValid = Output(Bool())
  val nextReady = Input(Bool())
}

// class Flow extends Bundle {
//   val valid = Output(Bool())
//   val ready = Input(Bool())
// }

object Flow {
  
  /**
    * control flow reg change
    * priority: bubble > block > next
    */
  def apply[T <: Data](
    flowReg: T,
    ifNext: Bool, next: T,
    ifBubble: Bool, bubbleNext: T,
    ifBlock: Bool, blockNext: T,
  ): T = {
    flowReg := MuxCase(flowReg, Array(
      ifBubble -> bubbleNext,
      ifBlock -> blockNext,
      ifNext -> next,
    ))
    flowReg
  }

  /**
    * commonly use
    * @param ifOver necessary work if over
    */
  def apply[T <: Data](
    flowReg: T, con: Control, bubbleNext: T, ifOver: Bool, next: T
  ): T = apply(
    flowReg,
    ifOver && con.fire, next,
    ifOver && con.bubble && con.fire, bubbleNext, // TODO: del ifOver
    con.block, flowReg
  )
}

class PC_IF extends Bundle {
  val pc = Output(UInt(64.W))
}

class IF_ID extends Bundle {
  val inst = Output(UInt(32.W))

  val pc = Output(UInt(64.W))
}
object IF_ID {
  def initReg(flow: IF_ID) = {
    val reg = RegInit({
      val bundle = Wire(new IF_ID())
      bundle.inst := 19.U
      bundle.pc := Param.PC_INIT.U
      bundle
    })
    flow <> reg
    reg
  }
}

class ID_EX extends Bundle {
  val comm = Output(RVCommand())
  val dest = Output(UInt(64.W))
  val src1 = Output(UInt(64.W))
  val src2 = Output(UInt(64.W))
  val regWEn = Output(Bool())

  val pc = Output(UInt(64.W))
}
object ID_EX {
  def initReg(flow: ID_EX) = {
    val reg = RegInit({
      val bundle = Wire(new ID_EX())
      bundle.comm := RVCommand.addi
      bundle.dest := 0.U
      bundle.src1 := 0.U
      bundle.src2 := 0.U
      bundle.regWEn := false.B
      bundle.pc := Param.PC_INIT.U
      bundle
    })
    flow <> reg
    reg
  }
}

class EX_ME extends Bundle {
  val memEn = Output(Bool())        // r,w
  val memRw = Output(Bool())
  val comm = Output(RVCommand())      // r,w
  val memAddr = Output(UInt(64.W))  // r,w
  val memData = Output(UInt(64.W))  // w
  val regDest = Output(UInt(5.W))   // r
  val regData = Output(UInt(64.W))

  val pc = Output(UInt(64.W))
}
object EX_ME {
  def initReg(flow: EX_ME) = {
    val reg = RegInit({
      val bundle = Wire(new EX_ME())
      bundle.memEn := false.B
      bundle.memRw := true.B
      bundle.comm := RVCommand.addi
      bundle.memAddr := 0.U
      bundle.memData := 0.U
      bundle.regDest := 0.U
      bundle.regData := 0.U
      bundle.pc := Param.PC_INIT.U
      bundle
    })
    flow <> reg
    reg
  }
}

class ME_WB extends Bundle {
  val regDest = Output(UInt(5.W))
  val regData = Output(UInt(64.W))

  val pc = Output(UInt(64.W))
}
object ME_WB {
  def initReg(flow: ME_WB) = {
    val reg = RegInit({
      val bundle = Wire(new ME_WB())
      bundle.regDest := 0.U
      bundle.regData := 0.U
      bundle.pc := Param.PC_INIT.U
      bundle
    })
    flow <> reg
    reg
  }
}

class Control extends Bundle {
  val block = Input(Bool())
  val bubble = Input(Bool())
  def en: Bool = !(block || bubble)

  val nextReady = Input(Bool())
  val prevValid = Input(Bool())
  def fire: Bool = nextReady && prevValid && !block
}
object Control {
  def apply(): Control = {
    val con = Wire(new Control)
    con.block := false.B
    con.bubble := false.B
    con
  }
  def init(con: Control) = {
    con.block := false.B
    con.bubble := false.B
  }
}

class DataForwardBundle extends Bundle {
  val exRegDest = Input(UInt(5.W))
  val exRegData = Input(UInt(64.W))
  val meRegDest = Input(UInt(5.W))
  val meRegData = Input(UInt(64.W))
  val wbRegDest = Input(UInt(5.W))
  val wbRegData = Input(UInt(64.W))
}
