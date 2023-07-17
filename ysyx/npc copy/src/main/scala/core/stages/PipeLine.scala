package core.stages

import chisel3._

import core.Control

object PipeLine {
  def init(ifu: IFU, idu: IDU, exu: EXU, meu: MEU) = {
    ifu.pipe.out <> idu.pipe.in
    idu.pipe.out <> exu.pipe.in
    exu.pipe.out <> meu.pipe.in

    ifu.pipe.con.prevValid := true.B
    idu.pipe.con.prevValid := ifu.pipe.valid
    exu.pipe.con.prevValid := idu.pipe.valid
    meu.pipe.con.prevValid := exu.pipe.valid
    ifu.pipe.con.nextReady := idu.pipe.ready
    idu.pipe.con.nextReady := exu.pipe.ready
    exu.pipe.con.nextReady := meu.pipe.ready
    meu.pipe.con.nextReady := true.B

    Control.init(ifu.pipe.con)
    Control.init(idu.pipe.con)
    Control.init(exu.pipe.con)
    Control.init(meu.pipe.con)
  }
}
