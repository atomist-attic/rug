package com.atomist.util

import java.lang.System._

/**
  * HOF to time operations.
  */
object Timing {

  /**
    * @return number of milliseconds it takes to execute
    * the block, if successful
    */
  def time[R](block: => R): (R, Long) = {
    val start = currentTimeMillis
    (block, currentTimeMillis - start)
  }
}
