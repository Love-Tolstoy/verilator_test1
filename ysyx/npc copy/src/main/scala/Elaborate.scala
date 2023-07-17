// import play._
import core._
import core.stages._
import core.alu._

import yourpackage._

object Elaborate extends App {
  // (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Radix2DIV())))
  // (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new IFU())))
  // (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new IDU())))
  // (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new EXU())))
  // (new chisel3.stage.ChiselStage).execute(args, Seq(
  //   chisel3.stage.ChiselGeneratorAnnotation(() => new CU()),
  //   firrtl.stage.RunFirrtlTransformAnnotation(new AddModulePrefix()),
  //   ModulePrefixAnnotation("yourprefix")
  // ))
  (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new SIM(false))))
  // (new chisel3.stage.ChiselStage).emitVerilog(new CU, Array("-td", "bulid","-fsm"))
  // (new chisel3.stage.ChiselStage).emitVerilog(new MEU)
}
