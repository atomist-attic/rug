package com.atomist.rug.runtime

import com.atomist.model.content.text.{PathExpressionEngine, TreeNode}
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.spi.MutableView
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

case class Match(root: TreeNode, matches: _root_.java.util.List[TreeNode]) {
}

/**
  * JavaScript-friendly facade to PathExpressionEngine.
  * Paralleled by a UserModel TypeScript interface.
  */
class PathExpressionExposer {

  val pee = new PathExpressionEngine

  def evaluate(tn: TreeNode, pe: Object): Match = {
    pe match {
      case som: ScriptObjectMirror =>
        val expr: String = som.get("expression").asInstanceOf[String]
        pee.evaluate(tn, expr) match {
          case Right(nodes) =>
            val m = Match(tn, nodes.asJava)
            m
        }
    }
  }

  /**
    * Return a single match. Throw an exception otherwise.
    */
  def scalar(root: TreeNode, expr: String): TreeNode = ???

  // cast the current node
  def as(root: TreeNode, name: String): TreeNode = ???

  // Find the children of the current node of this time
  def children(root: TreeNode, name: String): Seq[TreeNode] = root match {
    case cvf: ContextlessViewFinder =>
      cvf.findAllIn(root.asInstanceOf[MutableView[_]]).getOrElse(Nil)
  }
}

trait UserModelContext {

  def registry: Map[String, Object]

}

object DefaultUserModelContext extends UserModelContext {

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}

