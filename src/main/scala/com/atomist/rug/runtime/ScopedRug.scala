package com.atomist.rug.runtime

import com.atomist.rug.InvalidRugScopeException
import com.atomist.rug.runtime.RugScopes.Scope

/**
  * Used to describe the visibility of a Rug
  */
trait ScopedRug {
  def scope: Scope
}

object RugScopes {

  sealed abstract class Scope(name: String)
  case object ARCHIVE extends Scope("archive")
  case object DEFAULT extends Scope("default")

  def allScopes: Seq[Scope] = Seq(ARCHIVE, DEFAULT)

  def from(name: String): Scope = {
    name match {
      case "archive" => ARCHIVE
      case "default" => DEFAULT
      case _ => throw new InvalidRugScopeException(s"'$name' is not a known scope", allScopes)
    }
  }
}

