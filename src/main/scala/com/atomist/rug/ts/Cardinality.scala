package com.atomist.rug.ts

sealed trait Cardinality

case object OneToOne extends Cardinality

case object OneToM extends Cardinality

object Cardinality {

  val One2One = "1:1"

  val One2Many = "1:M"

  def apply(s: String): Cardinality = s match {
    case One2One => OneToOne
    case One2Many => OneToM
    case x =>
      throw new IllegalArgumentException(s"Unknown cardinality: [$x]: Valid values are [$One2One] and [$One2Many]")
  }
}
