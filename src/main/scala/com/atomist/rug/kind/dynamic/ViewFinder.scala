package com.atomist.rug.kind.dynamic

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.parser._
import com.atomist.rug.spi.MutableView
import com.atomist.source.ArtifactSource
import org.springframework.util.ObjectUtils

/**
  * Try to find children of this type in the given context
  */
trait ChildResolver {

  /**
    * The set of node types this can resolve from
    * @return set of node types this can resolve from
    */
  def resolvesFromNodeTypes: Set[String]

  /**
    * Find all in this context
    * @param context
    */
  def findAllIn(context: MutableView[_]): Option[Seq[MutableView[_]]]
}

/**
  * Adapter making it possible to resolve children without context in a ViewFinder.
  * Subclasses can implement this if they don't need ArtifactSource context etc.
  */
trait ContextlessViewFinder extends ViewFinder with ChildResolver {

  override final def findAllIn(context: MutableView[_]): Option[Seq[MutableView[_]]] =
    findAllIn(null, null, context, null, null)
}

/**
  * Find views in a context, with project knowledge. Used to drive `with` block execution.
  */
trait ViewFinder {

  final def findIn(
                    rugAs: ArtifactSource,
                    selected: Selected,
                    context: MutableView[_],
                    poa: ProjectOperationArguments,
                    identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    try {
      findAllIn(rugAs, selected, context, poa, identifierMap)
        .map(_.filter(v => invokePredicate(rugAs, poa, identifierMap, selected.predicate, selected.alias, v))
        )
    }
    catch {
      case npe: NullPointerException =>
        val msg = s"""Internal error in Rug type with alias '${selected.alias}': A view was returned as null.
                      | The context is: ${context}
                      | This is what is available: ${findAllIn(rugAs, selected, context, poa, identifierMap)}
                      |"""
        throw new RugRuntimeException(null, msg, npe)
    }
  }

  protected def findAllIn(
                           rugAs: ArtifactSource,
                           selected: Selected,
                           context: MutableView[_],
                           poa: ProjectOperationArguments,
                           identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]]

  def invokePredicate(rugAs: ArtifactSource,
                      poa: ProjectOperationArguments,
                      identifierMap: Map[String, Object],
                      predicate: Predicate,
                      targetAlias: String,
                      v: MutableView[_]): Boolean = {
    predicate match {
      case and: AndExpression =>
        invokePredicate(rugAs, poa, identifierMap, and.a, targetAlias, v) &&
          invokePredicate(rugAs, poa, identifierMap, and.b, targetAlias, v)
      case or: OrExpression =>
        invokePredicate(rugAs, poa, identifierMap, or.a, targetAlias, v) ||
          invokePredicate(rugAs, poa, identifierMap, or.b, targetAlias, v)
      case eq: EqualsExpression =>
        val l = v.evaluator.evaluate[MutableView[_], Object](eq.a, null, null, v, targetAlias, identifierMap, poa)
        val r = v.evaluator.evaluate[MutableView[_], Object](eq.b, null, null, v, targetAlias, identifierMap, poa)
        ObjectUtils.nullSafeEquals(l, r)
      case _ =>
        v.evaluator.evaluate[MutableView[_], Boolean](predicate, null, null, v, targetAlias, identifierMap, poa)
    }
  }

}
