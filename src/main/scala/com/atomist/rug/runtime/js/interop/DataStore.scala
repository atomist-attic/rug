package com.atomist.rug.runtime.js.interop

import com.atomist.tree.TreeNode

/**
  * Allows us to persist data
  */
trait DataStore {

  def learn(f: Fact)

  def retract(f: Fact)

  def create(n: TreeNode)

}


object AmnesiacDataStore extends DataStore {

  override def learn(f: Fact): Unit = {}

  override def retract(f: Fact): Unit = {}

  override def create(n: TreeNode): Unit = {}
}
