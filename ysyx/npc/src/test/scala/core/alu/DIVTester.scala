package core.alu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import chiseltest.simulator.WriteVcdAnnotation

class DIVTester extends AnyFlatSpec with ChiselScalatestTester {
  "DIVTester" should "pass" in {
    test(new Radix2DIV)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.divValid.poke(true.B)
      dut.io.flush.poke(false.B)
      dut.io.divw.poke(true.B)

      // dut.io.dividend.poke(45.U)
      // dut.io.divisor.poke(55.U)
      dut.io.dividend.poke(24.U)
      dut.io.divisor.poke(24.U)
      dut.io.divSigned.poke(false.B)

      dut.clock.step()

      dut.io.divValid.poke(false.B)
      for (a <- 0 to 10) {
        dut.clock.step()
      }
      dut.io.flush.poke(true.B)
      dut.io.divValid.poke(true.B)
      dut.io.divw.poke(true.B)
      for (a <- 0 to 32) {
        dut.clock.step()
      }
    }
  }
}
