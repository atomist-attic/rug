package com.atomist.project

/**
  * Supertrait for operations on a project such as modification and new project creation.
  * Subtraits will define operations such as generate or modify, working
  * on parameters specified by this type.
  */
trait ProjectOperation extends ProjectOperationInfo {

  /**
    * Return info about this delta.
    */
  def describe: ProjectOperationInfo = new SimpleProjectOperationInfo(this)
}
