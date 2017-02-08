package com.atomist.rug.kind.dynamic

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.parser._
import com.atomist.rug.spi.MutableView
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import org.springframework.util.ObjectUtils

/**
  * Try to find children of this type in the given context
  */
trait ChildResolver {

  /**
    * Find all in this context
    *
    * @param context
    */
  def findAllIn(context: TreeNode): Option[Seq[TreeNode]]
}

/**
  * Find views in a context, with Project knowledge. Used to drive `with` block execution.
  */
trait ViewFinder extends ChildResolver {

  final def findIn(
                    rugAs: ArtifactSource,
                    selected: Selected,
                    context: TreeNode,
                    poa: ProjectOperationArguments,
                    identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    try {
      findAllIn(context)
        .map(_.filter(v => invokePredicate(rugAs, poa, identifierMap, selected.predicate, selected.alias, v))
        )
    }
    catch {
      case npe: NullPointerException =>
        val msg =
          s"""Internal error in Rug type with alias '${selected.alias}': A view was returned as null.
             | The context is: $context
             | This is what is available: ${findAllIn(context)}
             |"""
        throw new RugRuntimeException(null, msg, npe)
    }
  }

  def invokePredicate(rugAs: ArtifactSource,
                      poa: ProjectOperationArguments,
                      identifierMap: Map[String, Object],
                      predicate: Predicate,
                      targetAlias: String,
                      v: TreeNode): Boolean = {
    predicate match {
      case and: AndExpression =>
        invokePredicate(rugAs, poa, identifierMap, and.a, targetAlias, v) &&
          invokePredicate(rugAs, poa, identifierMap, and.b, targetAlias, v)
      case or: OrExpression =>
        invokePredicate(rugAs, poa, identifierMap, or.a, targetAlias, v) ||
          invokePredicate(rugAs, poa, identifierMap, or.b, targetAlias, v)
      case not: NotExpression =>
        !invokePredicate(rugAs, poa, identifierMap, not.inner, targetAlias, v)
      case eq: EqualsExpression =>
        v match {
          case v: MutableView[_] =>
            val l = v.evaluator.evaluate[MutableView[_], Object](eq.a, null, null, v, targetAlias, identifierMap, poa)
            val r = v.evaluator.evaluate[MutableView[_], Object](eq.b, null, null, v, targetAlias, identifierMap, poa)
            ObjectUtils.nullSafeEquals(l, r)
        }
      case _ =>
        v match {
          case v: MutableView[_] =>
            v.evaluator.evaluate[MutableView[_], Boolean](predicate, null, null, v, targetAlias, identifierMap, poa)
        }
    }
  }

}
