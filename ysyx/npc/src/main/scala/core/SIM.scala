package core

import chisel3._
import chisel3.util._

import core.hazard._
import core.stages._
import core.except._
import device._

class SIM(soc: Boolean = false) extends CU {
  // use for sim
  val ioSim = IO(new Bundle {
    val regsChange = Output(Bool())

    val regs = Output(Vec(32, UInt(64.W)))
    val pc = Output(UInt(64.W))
    val pcOver = Output(UInt(64.W))
    val csrs = Output(Vec(10, UInt(64.W)))

    val branch = Output(Bool())
    val dataForward = Output(Bool())
    val loadConflict = Output(Bool())
    val raiseIntr = Output(Bool())
    val mret = Output(Bool())

    val ifHit = Output(Bool())
  })

  if (!soc) {
    val mem = Module(new AXI4MEM)
    arbiter.io.out <> mem.bus
  }

  // sim
  ioSim.regsChange := regsWen
  ioSim.regs := regs.io.regsOut
  ioSim.pc := pc.io.pcOut
  ioSim.pcOver := meu.pipe.out.pc
  ioSim.csrs := csrs.io.regsOut

  ioSim.branch := exu.io.branch
  ioSim.loadConflict := dh.io.loadConflict
  ioSim.dataForward := dh.io.dataForward
  ioSim.raiseIntr := exp.io.raiseIntr && exu.pipe.fire
  ioSim.mret := exu.io.mret && exu.pipe.fire

  // record
  ioSim.ifHit := ifu.io.hit
}
