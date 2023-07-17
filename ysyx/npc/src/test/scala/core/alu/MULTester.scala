package core.alu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import chiseltest.simulator.WriteVcdAnnotation

class MULTester extends AnyFlatSpec with ChiselScalatestTester {
  "MULTester" should "pass" in {
    test(new Shift1MUL)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.mulValid.poke(true.B)
      dut.io.flush.poke(false.B)
      dut.io.mulw.poke(true.B)

      dut.io.multiplicand.poke(0.U)
      dut.io.multiplier.poke(1.U)
      dut.io.mulSigned.poke(3.U)

      dut.clock.step()

      dut.io.mulValid.poke(false.B)
      for (a <- 0 to 64) {
        dut.clock.step()
      }
    }
  }
}
