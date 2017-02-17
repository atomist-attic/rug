package com.atomist.project

import com.atomist.rug.runtime.ParameterizedRug

/**
  * Supertrait for operations on a project such as modification and new project creation.
  * Subtraits will define operations such as generate or modify, working
  * on parameters specified by this type.
  */
trait ProjectOperation extends ParameterizedRug {

  /**
    * Return info about this delta.
    */
  def describe: ParameterizedRug = new SimpleParameterizedRug(this)
}
