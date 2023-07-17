package core.alu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import chiseltest.simulator.WriteVcdAnnotation

class ALUTester extends AnyFlatSpec with ChiselScalatestTester {
  "ALUTester" should "pass" in {
    test(new ALU)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (a <- 0 to 15) {
        for (b <- 0 to 15) {
          for (sel <- 0 to 7) {
            dut.io.aluIn.bits.num1.poke(a.U)
            dut.io.aluIn.bits.num2.poke(b.U)
            dut.io.aluIn.bits.op.poke(ALUOp.add)
            dut.clock.step()
            printf("dut: %d %d %d\n",dut.io.aluIn.bits.num1, dut.io.aluIn.bits.num2, dut.io.ans)
          }
        }
      }
    }
  }
}
