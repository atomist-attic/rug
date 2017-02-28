package com.atomist.rug.runtime

import com.atomist.param.Tag

import scala.collection.mutable.ListBuffer

/**
  * For things common to _all_ Rugs
  */
trait Rug {

  def name: String

  def description: String

  def tags: Seq[Tag]

  //group:artifact:name
  private def fqRex = "^(.*?):(.*?):(.*?)$".r

  def findRug(simpleOrFq: String) : Option[Rug] = {
    simpleOrFq match {
      case simple: String if !simple.contains(":") =>
        archiveContext.find(p => p.name == simple) match {
          case Some(rug) => Some(rug)
          case _ =>
            if (this.name == simpleOrFq) {
            Some(this)
          } else {
            None
          }
        }
      case fq: String => fqRex.findFirstMatchIn(fq) match {
        case Some(m) => externalContext.find(p =>
          p.group == m.group(1) &&
            p.artifact == m.group(2) &&
            p.name == m.group(3))
        case _ => None
      }
    }
  }

  def findParameterizedRug(simpleOrFq: String) : Option[ParameterizedRug] = {
    findRug(simpleOrFq) match {
      case Some(o: ParameterizedRug) => Some(o)
      case _ => None
    }
  }

  def allRugs: Seq[Rug] = archiveContext ++ externalContext

  val _otherRugs = new ListBuffer[Rug]

  /**
    * Add other rugs in the same archive to the context
    * @param rugs
    */
  def addToArchiveContext(rugs: Seq[Rug]) : Unit = {
    rugs.foreach(r =>
      if(!_otherRugs.contains(r))
        _otherRugs += r)
  }

  /**
    * For rugs referenced in the manifest outside of this rugs archive
    * @return
    */
  def externalContext: Seq[AddressableRug] = Nil

  /**
    * The set of rugs inside the same archive/runtime context
    * @return
    */
  def archiveContext: Seq[Rug] = _otherRugs
}

/**
  * Like a rug, but we know a bit more about it
  */
trait AddressableRug extends Rug {
  def artifact: String
  def group: String
  def version: String
}
