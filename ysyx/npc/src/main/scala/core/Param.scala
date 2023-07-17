package core

object Param {
  val PC_INIT = "x80000000"
  val PC_OVER = "x87ffff01"
  val PC_NOIMPL = "x87ffff02"

  val CONFIG_MBASE = "x80000000"
  val CONFIG_MSIZE = "x8000000"
  val M_UPPER_LIMIT = "x88000000"

  val CLINT_BASE_ADDR = "x2000000"
  val CLINT_MTIME_ADDR = "x200bff8"
  val CLINT_MTIMECMP_ADDR = "x2004000"
}
