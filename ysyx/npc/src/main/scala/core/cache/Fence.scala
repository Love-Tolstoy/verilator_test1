package core.cache

import chisel3._
import chisel3.util._

import device._
import core.Tool.saveInit

/**
  * after get index, tag.D = false
  * TODO: tagVD and data shouldn't change, shouldn't use save
  * 
  * stage1: get first index 0 -> stage2
  * stage2: !dirty -> stage2; dirty -> stage3 or 4
  * stage3: write back 0 -> 2 or 4
  * stage4: write back 1
  *
  * @param c
  */
class FenceI(c: CacheConfig) extends AXI4M_Master {
  val io = IO(new Bundle {
    val en = Input(Bool())

    // get SRAM
    val ren = Output(Bool())
    val wen = Output(Bool())
    val index = Output(UInt(c.index.W))
    val tagVDArray = Input(UInt(c.ramWidth.W))
    val dataArray = Input(UInt(c.ramWidth.W))

    val over = Output(Bool())
  })

  // flag
  val dirty = WireInit(0.U(2.W))
  val indexOver = WireInit(false.B)

  val idx_state = RegInit(0.U(c.index.W))
  idx_state := MuxCase(idx_state, Array(
    !io.en -> 0.U,
    indexOver -> (idx_state + 1.U),
  ))

  val s_stage1 :: s_stage2 :: s_stage3 :: s_stage4 :: Nil = Enum(4)
  val state = RegInit(s_stage1)
  state := MuxCase(state, Array(
    (state === s_stage1 && io.en) -> s_stage2,
    (state === s_stage2 && dirty === 0.U) -> s_stage2,
    (state === s_stage2 && dirty(0).asBool) -> s_stage3,
    (state === s_stage3 && bus_write_over && dirty(1).asBool) -> s_stage4,
    (state === s_stage3 && bus_write_over && !dirty(1).asBool) -> s_stage2,
    (state === s_stage2 && !dirty(0).asBool && dirty(1).asBool) -> s_stage4,
    (state === s_stage4 && bus_write_over) -> s_stage2
    // (state === s_stage3 && indexWriteOver) -> s_stage2,
  ))

  // save
  val tagVD0 = saveInit(0.U.asTypeOf(new CacheTagVD(c)))
  val tagVD1 = saveInit(0.U.asTypeOf(new CacheTagVD(c)))

  val dataline0 = saveInit(0.U(64.W))
  val dataline1 = saveInit(0.U(64.W))

  dirty := tagVD1.d.asUInt ## tagVD0.d.asUInt

  // if true forever, tag and data don't need save
  io.ren := Mux(indexOver, true.B, false.B)
  io.index := Mux(indexOver, idx_state + 1.U, idx_state)

  switch(state) {
    is(s_stage1) { io.ren := io.en }
    is(s_stage2) {
      tagVD0 := io.tagVDArray(63, 0).asTypeOf(new CacheTagVD(c))
      tagVD1 := io.tagVDArray(127, 64).asTypeOf(new CacheTagVD(c))
      dataline0 := io.dataArray(63, 0)
      dataline1 := io.dataArray(127, 64)

      indexOver := dirty === 0.U
    }
    is(s_stage3) {
      bus_write := true.B
      bus_addr := tagVD0.tag ## idx_state ## 0.U(c.offset.W)
      bus_data := dataline0
      bus_strb := "b11111111".U

      indexOver := !dirty(1).asBool && bus_write_over
    }
    is(s_stage4) {
      bus_write := true.B
      bus_addr := tagVD1.tag ## idx_state ## 0.U(c.offset.W)
      bus_data := dataline1
      bus_strb := "b11111111".U

      indexOver := bus_write_over
    }
  }

  io.wen := dirty =/= 0.U
  io.over := idx_state === (c.depth-1).U && indexOver
}
