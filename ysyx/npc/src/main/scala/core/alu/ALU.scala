package core.alu

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

import core.Tool._

class ALU extends Module {
  val io = IO(new Bundle {
    val aluIn = Flipped(Decoupled(new ALUIO))
    val flush = Input(Bool())

    val outValid = Output(Bool())    // only one clock
    val ans = Output(UInt(64.W))
  })
  val num1 = io.aluIn.bits.num1
  val num2 = io.aluIn.bits.num2
  val op = io.aluIn.bits.op
  val sign = io.aluIn.bits.sign
  val aluw = io.aluIn.bits.aluw

  val mul = Module(new Shift1MUL)
  val div = Module(new Radix2DIV)

  // printf("%n,%d\n", num1,num1)
  // printf("%n,\n" + op.toString(), op.asUInt)
  // printf(op.toPrintable)

  // val mulNeed = op === ALUOp.mul
  // val divNeed = op === ALUOp.div || op === ALUOp.rem
  val mulValid = io.aluIn.fire && op === ALUOp.mul
  val divValid = io.aluIn.fire && (op === ALUOp.div || op === ALUOp.rem)
  val s_none :: s_muldiv :: s_mulDoing :: s_divDoing :: Nil= Enum(4)
  val state = RegInit(s_none)
  state := MuxCase(state, Array(
    // (state === s_none && (mulNeed || divNeed)) -> s_muldiv,
    // (state === s_muldiv && mulNeed)

    (state === s_none && mulValid && mul.io.mulReady) -> s_mulDoing,
    (state === s_none && divValid && div.io.divReady) -> s_divDoing,
    (state === s_mulDoing && mul.io.outValid) -> s_none,
    (state === s_divDoing && div.io.outValid) -> s_none,
  ))

  io.aluIn.ready := state === s_none
  mul.io.mulValid := mulValid
  mul.io.flush := io.flush
  div.io.divValid := divValid
  div.io.flush := io.flush

  mul.io.mulw := aluw
  mul.io.multiplier := num1
  mul.io.multiplicand := num2
  mul.io.mulSigned := Mux(sign, 3.U, 0.U)

  div.io.divw := aluw
  div.io.dividend := num1
  div.io.divisor := num2
  div.io.divSigned := sign

  // val mulResultHi = WireInit(0.U(64.W))
  // val mulResultLo = WireInit(0.U(64.W))
  // val divQuotient = WireInit(0.U(64.W))
  // val divRemainder = WireInit(0.U(64.W))
  // val mulResultHiReg = RegEnable(mulResultHi)
  // val mulResultLoReg = RegNext(mulResultLo)
  // val divQuotientReg = RegNext(divQuotient)
  // val divRemainderReg = RegNext(divRemainder)
  // when(state === s_mulDoing && mul.io.outValid) {
  //   mulResultHi := mul.io.result.hi
  //   mulResultLo := mul.io.result.lo
  // }
  // when(state === s_divDoing && div.io.outValid) {
  //   divQuotient := div.io.quotient
  //   divRemainder := div.io.remainder
  // }

  val ans = Wire(UInt(64.W))
  val snum1 = Wire(SInt(64.W))
  val snum2 = Wire(SInt(64.W))
  snum1 := Mux(aluw, sext(num1, 32).asSInt, num1.asSInt)
  snum2 := Mux(aluw, sext(num2, 32).asSInt, num2.asSInt)

  ans := MuxLookup(op.asUInt, num1, Array(
    ALUOp.add.asUInt -> Mux(sign, (snum1 + snum2).asUInt, num1 + num2),
    ALUOp.sub.asUInt -> Mux(sign, (snum1 - snum2).asUInt, num1 - num2),
    ALUOp.mul.asUInt -> Mux(sign, mul.io.result.hi(63) ## mul.io.result.lo(62, 0),
      mul.io.result.lo),
    ALUOp.div.asUInt -> div.io.quotient,
    ALUOp.rem.asUInt -> div.io.remainder,

    ALUOp.sl.asUInt  -> Mux(sign, (snum1 << num2(5,0)).asUInt, num1 << num2(5,0)),
    ALUOp.sr.asUInt  -> Mux(sign, (snum1 >> num2(5,0)).asUInt, num1 >> num2(5,0)),

    ALUOp.eq.asUInt  -> (num1 === num2),
    ALUOp.gt.asUInt  -> Mux(sign, snum1 > snum2, num1 > num2),
    ALUOp.gte.asUInt -> Mux(sign, snum1 >= snum2, num1 >= num2),
    ALUOp.lt.asUInt  -> Mux(sign, snum1 < snum2, num1 < num2),
    ALUOp.lte.asUInt -> Mux(sign, snum1 <= snum2, num1 <= num2),

    ALUOp.and.asUInt -> (num1 & num2),
    ALUOp.or.asUInt  -> (num1 | num2),
    ALUOp.not.asUInt -> ~num1,
    ALUOp.xor.asUInt -> (num1 ^ num2),

    ALUOp.none.asUInt -> num1,
  ))

  io.ans := Mux(aluw, sext(ans, 32), ans)
  io.outValid := (state === s_none &&
    !op.isOneOf(ALUOp.mul,ALUOp.div,ALUOp.rem)) ||
    (state === s_mulDoing && mul.io.outValid) ||
    (state === s_divDoing && div.io.outValid)
}
object ALU {
  def apply(w: ALUIO) = {
    val alu = Module(new ALU)
    alu.io.aluIn.bits := w
    alu
  }
}

class ALUIO extends Bundle {
  val num1  = UInt(64.W)
  val num2  = UInt(64.W)
  val op    = ALUOp()
  val sign  = Bool()
  val aluw = Bool()    // 32 bits
}
object ALUIO {
  def apply(num1: UInt, num2: UInt, op: Data,
    sign: Bool = false.B, aluw: Bool = false.B) = {
    val w = Wire(new ALUIO)
    w.num1 := num1
    w.num2 := num2
    w.op := op
    w.sign := sign
    w.aluw := aluw
    w
  }
}

object ALUOp extends ChiselEnum {
  val add, sub, mul, div, rem = Value
  val sl, sr = Value
  val eq, gt, gte, lt, lte = Value
  val and, or, not, xor = Value   // Bit
  val none = Value
}
