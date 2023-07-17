package core.hazard

import chisel3._
import chisel3.util._

import core._

class DataHazard extends Module {
  val io = IO(new Bundle{
    val regsRead = Flipped(new RegsReadIO)

    val exRegDest = Input(UInt(5.W))
    val exRegData = Input(UInt(64.W))
    val exLoad = Input(Bool())
    val meRegDest = Input(UInt(5.W))
    val meRegData = Input(UInt(64.W))
    val wbRegDest = Input(UInt(5.W))
    val wbRegData = Input(UInt(64.W))

    val idRegsRead = new RegsReadIO
    val dataForward = Output(Bool())
    val loadConflict = Output(Bool())   // can't forward
  })
  val rs1 = io.idRegsRead.rs1
  val rs2 = io.idRegsRead.rs2

  io.regsRead.rs1 := rs1
  io.regsRead.rs2 := rs2

  val reg1Conflict = WireDefault(false.B)
  val reg2Conflict = WireDefault(false.B)
  val data1 = WireDefault(0.U(64.W))
  val data2 = WireDefault(0.U(64.W))

  reg1Conflict := MuxLookup(rs1, false.B, Array(
    io.exRegDest -> true.B,
    io.meRegDest -> true.B,
    io.wbRegDest -> true.B,
  ))
  reg2Conflict := MuxLookup(rs2, false.B, Array(
    io.exRegDest -> true.B,
    io.meRegDest -> true.B,
    io.wbRegDest -> true.B,
  ))
  io.idRegsRead.rs1Data := MuxCase(io.regsRead.rs1Data, Array(
    (rs1 === io.exRegDest) -> io.exRegData,
    (rs1 === io.meRegDest) -> io.meRegData,
    (rs1 === io.wbRegDest) -> io.wbRegData,
  ))
  io.idRegsRead.rs2Data := MuxCase(io.regsRead.rs2Data, Array(
    (rs2 === io.exRegDest) -> io.exRegData,
    (rs2 === io.meRegDest) -> io.meRegData,
    (rs2 === io.wbRegDest) -> io.wbRegData,
  ))

  io.dataForward := (reg1Conflict || reg2Conflict) &&
    (rs1 =/= 0.U || rs2 =/= 0.U)
  io.loadConflict := io.exLoad && 
    (rs1 === io.exRegDest || rs2 === io.exRegDest)
}

// object DataHazard {
//   def apply(regs: Vec[UInt], idu: IDU, exu: EXU, meu: MEU
//   ): DataHazard = {
//     val dh = Module(new DataHazard)
//     dh.io.regsIn := regs
//     dh.rs1 := idu.rs1
//     dh.rs2 := idu.rs2
//     dh.io.exRegDest := exu.io.regDest
//     dh.io.exRegData := exu.io.regData
//     dh.io.exLoad := exu.io.exLoad
//     dh.io.meRegDest := meu.io.regDest
//     dh.io.meRegData := meu.io.regData
//     dh.io.wbRegDest := meu.io.flowOut.regDest
//     dh.io.wbRegData := meu.io.flowOut.regData
//     dh
//   }
// }

// class DataForward extends Module {
//   val io = IO(new Bundle{
//     val regsIn = Input(Vec(32, UInt(64.W)))
//     val useReg1 = Input(UInt(5.W))
//     val useReg2 = Input(UInt(5.W))

//     val exRegDest = Input(UInt(5.W))
//     val exRegData = Input(UInt(64.W))
//     val meRegDest = Input(UInt(5.W))
//     val meRegData = Input(UInt(64.W))
//     val wbRegDest = Input(UInt(5.W))
//     val wbRegData = Input(UInt(64.W))

//     val regsOut = Output(Vec(32, UInt(64.W)))
//   })
//   val conflictReg1 = WireDefault(0.U(5.W))
//   val conflictReg2 = WireDefault(0.U(5.W))
//   val conflictData1 = WireDefault(0.U(64.W))
//   val conflictData2 = WireDefault(0.U(64.W))

//   conflictReg1 := MuxLookup(rs1, 0.U, Array(
//     io.exRegDest -> rs1,
//     io.meRegDest -> rs1,
//     io.wbRegDest -> rs1,
//   ))
//   conflictReg2 := MuxLookup(rs2, 0.U, Array(
//     io.exRegDest -> rs2,
//     io.meRegDest -> rs2,
//     io.wbRegDest -> rs2,
//   ))
//   conflictData1 := MuxLookup(rs1, 0.U, Array(
//     io.exRegDest -> io.exRegData,
//     io.meRegDest -> io.meRegData,
//     io.wbRegDest -> io.wbRegData,
//   ))
//   conflictData2 := MuxLookup(rs2, 0.U, Array(
//     io.exRegDest -> io.exRegData,
//     io.meRegDest -> io.meRegData,
//     io.wbRegDest -> io.wbRegData,
//   ))

//   io.regsOut := io.regsIn
//   io.regsOut(conflictReg1) := conflictData1
//   io.regsOut(conflictReg2) := conflictData2
//   io.regsOut(0) := 0.U
// }


// class LoadConflict extends Module {

// }
