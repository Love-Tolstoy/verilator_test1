package core.cache

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR

import device._
import core.Param

/**
  * ram width >= 128bits
  * default: 4096,64,2
  */
case class CacheConfig (
  // cacheSize: Int = 4096,        // B
  depth: Int = 128,             // dataline s
  datalineSize: Int = 64,       // bits
  round: Int = 2,
  ramWidth: Int = 128,
) {
  assert(datalineSize == 64)
  assert(round == 2)
  assert(ramWidth == 128)
  def offset = log2Ceil(datalineSize / 8)
  // def depth = cacheSize / (datalineSize * round / 8)
  def index = log2Ceil(depth)
  def tagLength = 64 - offset - index
}

class CacheTagVD(c: CacheConfig) extends Bundle {
  val tag = UInt((c.tagLength).W)
  val v = Bool()
  val d = Bool()
}
object CacheTagVD {
  def apply(c: CacheConfig, tag: UInt, v: Bool, d: Bool) = {
    val t = Wire(new CacheTagVD(c))
    t.tag := tag
    t.v := v
    t.d := d
    t
  }
}

/**
  * stage1: get dataline, state; get read data
  * stage2: hit: read or write -> stage1
            !hit: !dirty->stage3 ; dirty->stage4
  * stage3: axi4 read , write data -> stage1 ; wen -> stage5
  * stage4: axi4 write back -> stage3
  * stage5: write cache
  * stage6: fence.i
  */
class Cache(c: CacheConfig) extends AXI4M_Master {
  val io = IO(new Bundle {
    val en = Input(Bool())
    val wen = Input(Bool())
    val fencei = Input(Bool())

    val addr = Input(UInt(64.W))

    val strb = Input(UInt(8.W))
    val dataIn = Input(UInt(64.W))

    val over = Output(Bool())
    val dataOut = Output(UInt(64.W))
  })

  val tagVDArray = RAM(c.depth, c.ramWidth)
  val dataArray = RAM(c.depth, c.ramWidth)

  // for lookup
  val tag = WireInit(0.U(c.tagLength.W))
  val index =  WireInit(0.U(c.index.W))
  val offset = WireInit(0.U(c.offset.W))
  tag := io.addr(63, c.index + c.offset)
  index := io.addr(c.index + c.offset - 1, c.offset)
  offset := io.addr(c.offset - 1, 0)

  // for state
  val hit = WireInit(false.B)
  val dirty = WireInit(false.B)

  // fence.i
  val fence = Module(new FenceI(c))
  fence.io.en := false.B
  fence.io.tagVDArray := tagVDArray.io.dataOut
  fence.io.dataArray := dataArray.io.dataOut
  fence.bus <> DontCare
  val clearD = RegInit(false.B)

  val s_stage1 :: s_stage2 :: s_stage3 :: s_stage4 :: s_stage5 :: s_stage6 :: Nil = Enum(6)
  val state = RegInit(s_stage1)
  state := MuxCase(state, Array(
    !io.en -> s_stage1,
    io.fencei -> s_stage6,
    (state === s_stage1 && io.en) -> s_stage2,
    (state === s_stage2 && hit) -> s_stage1,
    (state === s_stage2 && !hit && !dirty) -> s_stage3,
    (state === s_stage2 && !hit && dirty) -> s_stage4,
    (state === s_stage4 && bus_write_over) -> s_stage3,
    (state === s_stage3 && bus_read_over && !io.wen) -> s_stage1,
    (state === s_stage3 && bus_read_over && io.wen) -> s_stage5,
    (state === s_stage5) -> s_stage1,
  ))

  // record
  val round = WireInit(0.U(1.W))
  val dataline = WireInit(0.U(64.W))

  val random = LFSR(16, state === s_stage1 && io.en)(0)

  val tagVDOut0 = tagVDArray.io.dataOut(63, 0).asTypeOf(new CacheTagVD(c))
  val tagVDOut1 = tagVDArray.io.dataOut(127, 64).asTypeOf(new CacheTagVD(c))

  val hitReg = RegNext(hit, false.B)
  hit := Mux(state === s_stage2,
    (tagVDOut0.v && tagVDOut0.tag === tag) ||
    (tagVDOut1.v && tagVDOut1.tag === tag), hitReg)

  // dirty := Mux(round === 0.U, tagVDOut0.d, tagVDOut1.d)

  val roundReg = RegNext(round, 0.U(1.W))
  round := Mux(state === s_stage2,
    Mux(hit, Mux(tagVDOut1.v && tagVDOut1.tag === tag, 1.U, 0.U), random), roundReg)

  val datalineReg = RegNext(dataline, 0.U(64.W))
  dataline := MuxCase(datalineReg, Array(
    (state === s_stage2 && tagVDOut0.tag === tag) -> dataArray.io.dataOut(63, 0),
    (state === s_stage2 && tagVDOut1.tag === tag) -> dataArray.io.dataOut(127, 64),
    (!io.wen && bus_read_over) -> bus.r.bits.data,
  ))

  // save tagVD and dataline according round
  val tagVDSave = Wire(new CacheTagVD(c))
  val tagVDSaveReg = RegNext(tagVDSave, 0.U.asTypeOf(new CacheTagVD(c)))
  tagVDSave := MuxCase(tagVDSaveReg, Array(
    (state === s_stage2 && round === 0.U) -> tagVDOut0,
    (state === s_stage2 && round === 1.U) -> tagVDOut1,
  ))
  val dataArraySave = WireInit(0.U(64.W))
  val dataArraySaveReg = RegNext(dataArraySave, 0.U(64.W))
  dataArraySave := MuxCase(dataArraySaveReg, Array(
    (state === s_stage2 && round === 0.U) -> dataArray.io.dataOut(63, 0),
    (state === s_stage2 && round === 1.U) -> dataArray.io.dataOut(127, 64),
  ))

  dirty := tagVDSave.d

  val dataIn = MuxLookup(offset, io.dataIn, Array(
    0.U -> io.dataIn(63,0),
    1.U -> io.dataIn(55,0) ## 0.U(8.W),
    2.U -> io.dataIn(47,0) ## 0.U(16.W),
    3.U -> io.dataIn(39,0) ## 0.U(24.W),
    4.U -> io.dataIn(31,0) ## 0.U(32.W),
    5.U -> io.dataIn(23,0) ## 0.U(40.W),
    6.U -> io.dataIn(15,0) ## 0.U(48.W),
    7.U -> io.dataIn(7,0) ## 0.U(56.W),
  ))
  val strb = io.strb << offset

  // init
  dataArray.init()
  tagVDArray.init()
  dataArray.io.addr := index
  tagVDArray.io.addr := index
  dataArray.io.cen := false.B
  tagVDArray.io.cen := false.B
  dataArray.io.wen := false.B
  tagVDArray.io.wen := false.B

  io.over := false.B

  switch(state) {
    is(s_stage1) {
      tagVDArray.io.cen := true.B
      dataArray.io.cen := true.B
    }
    is(s_stage2) {
      when(hit) {
        when(io.wen) {
          writeTagVDArray(CacheTagVD(c, tag, true.B, true.B))
          writeDataArray(strb, dataIn)
        }

        io.over := true.B
      }.otherwise {
        when(!dirty) {
          bus_read := true.B
          bus_addr := io.addr(63, c.offset) ## 0.U(c.offset.W)
        }
      }
    }
    is(s_stage3) {
      // bus read
      bus_read := true.B
      bus_addr := io.addr(63, c.offset) ## 0.U(c.offset.W)

      // write
      when(bus_read_over) {
        writeTagVDArray(CacheTagVD(c, tag, true.B, false.B))
        writeDataArray("xff".U(8.W), bus.r.bits.data)
      }

      io.over := !io.wen && bus_read_over
    }
    is(s_stage4) {
      bus_write := true.B
      bus_addr := tagVDSave.tag ## index ## 0.U(c.offset.W)
      bus_data := dataArraySave
      bus_strb := "b11111111".U
    }
    is(s_stage5) {
      writeTagVDArray(CacheTagVD(c, tag, true.B, true.B))
      writeDataArray(strb, dataIn)

      io.over := true.B
    }
    is(s_stage6) {
      fence.io.en := true.B
      fence.bus <> bus
      when(fence.io.ren) {
        tagVDArray.io.cen := true.B
        dataArray.io.cen := true.B
        dataArray.io.addr := fence.io.index
        tagVDArray.io.addr := fence.io.index
        clearD := true.B
      }
      when(clearD && fence.io.wen) {
        tagVDArray.io.cen := true.B
        tagVDArray.io.wen := true.B
        tagVDArray.io.mask := "xffff".U
        tagVDArray.io.dataIn :=
          CacheTagVD(c, tagVDOut1.tag, tagVDOut1.v, false.B).asUInt ##
          0.U(64.W) | CacheTagVD(c, tagVDOut0.tag, tagVDOut0.v, false.B).asUInt
        clearD := false.B
      }
      when(fence.io.over) {
        io.over := true.B
        state := s_stage1
      }
    }
  }

  io.dataOut := MuxLookup(offset, 0.U, Array(
    0.U -> dataline(63, 0),
    1.U -> dataline(63, 8),
    2.U -> dataline(63, 16),
    3.U -> dataline(63, 24),
    4.U -> dataline(63, 32),
    5.U -> dataline(63, 40),
    6.U -> dataline(63, 48),
    7.U -> dataline(63, 56),
  ))

  def writeTagVDArray(data: CacheTagVD) = {
    tagVDArray.io.cen := true.B
    tagVDArray.io.wen := true.B
    tagVDArray.io.mask := Mux(round === 0.U, "x00ff".U, "xff00".U)
    tagVDArray.io.dataIn := data.asUInt ## 0.U(64.W) | data.asUInt
  }

  def writeDataArray(mask: UInt, data: UInt) = {
    dataArray.io.cen := true.B
    dataArray.io.wen := true.B
    dataArray.io.mask := Mux(round === 0.U,
      0.U(8.W) ## mask, mask ## 0.U(8.W))
    dataArray.io.dataIn := data ## data
  }
}

// object Cache {
//   def apply(en: Bool, wen: Bool, addr: UInt, size: UInt,
//     strb: UInt, dataIn: UInt) = {
//     val dcache = Module(new Cache(CacheConfig()))
//     val uncache = Module(new UNCache)
//   }
// }

class MMIOControl(c: CacheConfig) extends Module {
  val io = IO(new Bundle {
    val en = Input(Bool())
    val wen = Input(Bool())
    val addr = Input(UInt(64.W))

    val fencei = Input(Bool())

    val strb = Input(UInt(8.W))
    val dataIn = Input(UInt(64.W))

    val size = Input(UInt(2.W))

    val over = Output(Bool())
    val dataOut = Output(UInt(64.W))

    val bus = new AXI4M_MasterIO
  })

  val cache = Module(new Cache(c))
  val uncache = Module(new UNCache)

  val mmio = (io.addr > Param.M_UPPER_LIMIT.U) ||
    (io.addr < Param.CONFIG_MBASE.U)

  io.bus.init()

  cache.io.en := false.B
  cache.io.wen := io.wen
  cache.io.fencei := io.fencei
  cache.io.addr := io.addr
  cache.io.strb := io.strb
  cache.io.dataIn := io.dataIn
  cache.bus <> DontCare

  uncache.io.en := false.B
  uncache.io.wen := io.wen
  uncache.io.addr := io.addr
  uncache.io.strb := io.strb
  uncache.io.dataIn := io.dataIn
  uncache.io.size := io.size
  uncache.bus <> DontCare

  io.over := false.B
  io.dataOut := 0.U
  when(mmio && !io.fencei) {
    uncache.io.en := io.en
    uncache.bus <> io.bus

    io.over := uncache.io.over
    io.dataOut := uncache.io.dataOut
  }.otherwise {
    cache.io.en := io.en
    cache.bus <> io.bus

    io.over := cache.io.over
    io.dataOut := cache.io.dataOut
  }
}
