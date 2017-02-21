package com.atomist.rug.runtime.rugdsl

import com.atomist.graph.GraphNode
import com.atomist.param.ParameterValues
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.dynamic.{MutableContainerMutableView, MutableTreeNodeUpdater}
import com.atomist.rug.parser._
import com.atomist.rug.spi.{MutableView, Type, TypeRegistry}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.microgrammar.Microgrammar
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionEngine}
import com.typesafe.scalalogging.LazyLogging
import org.springframework.util.ObjectUtils

class DefaultViewFinder(typeRegistry: TypeRegistry)
  extends LazyLogging {

  type SelectedChildrenOrSuggestedKinds = Either[Set[String], Seq[TreeNode]]

  final def findIn(
                    rugAs: ArtifactSource,
                    selected: Selected,
                    context: TreeNode,
                    poa: ParameterValues,
                    identifierMap: Map[String, Object]): SelectedChildrenOrSuggestedKinds = {

    try {
      findAllIn(rugAs, selected, context, poa, identifierMap).right
        .map(_.filter(v => invokePredicate(rugAs, poa, identifierMap, selected.predicate, selected.alias, v))
        )
    }
    catch {
      case npe: NullPointerException =>
        val msg =
          s"""Internal error in Rug type with alias '${selected.alias}': A view was returned as null.
             | The context is: $context
             |"""
        throw new RugRuntimeException(null, msg, npe)
    }
  }

  /**
    * Finds views, first looking at children of current scope,
    * then identifiers in scope, then global identifiers.
    *
    * If it can't find anything it returns a list of suggestions
    */
  def findAllIn(rugAs: ArtifactSource, selected: Selected, context: TreeNode,
                poa: ParameterValues, identifierMap: Map[String, Object]): SelectedChildrenOrSuggestedKinds = {

    val fromIdentifierInScope: Option[Seq[TreeNode]] = identifierMap.get(selected.kind).flatMap(typ => {
      logger.debug(s"Getting type '${selected.kind}' from $typ")
      (typ, context) match {
        case (mg: Microgrammar, f: FileArtifactBackedMutableView) =>
          val l: Option[MatchListener] = None
          val ms = mg.findMatches(f.content, l)
          val views = ms collect {
            case moo: MutableContainerTreeNode =>
              f.registerUpdater(new MutableTreeNodeUpdater(moo))
              new MutableContainerMutableView(moo, f)
          }
      //   f.registerUpdater(new MutableTreeNodeUpdater(container)) does this matter?? can I do it individually?
          Some(views)
        case (pex: PathExpression, m: MutableView[_]) =>
          // TODO cache this
          val pe = new PathExpressionEngine()
          // TODO pass in NodePreparer?
          val evaluated: Either[String, List[GraphNode]] = pe.evaluate(context, pex, DefaultTypeRegistry, None)
          evaluated match {
            case Right(nodes) =>
              //nodes.map(tn => new MutableContainerTreeNodeMutableView(tn.asInstanceOf[MutableContainerTreeNode], context))
              Some(nodes map {
                case mv: TreeNode => mv
              })
            case Left(_) =>
              Some(Nil)
          }
        case (suovmv: Seq[MutableContainerMutableView @unchecked], _) =>
          Some(suovmv)
        case (childType, parent) =>
          // This is fine
          logger.debug(s"Handling selected block with alias [${selected.alias}]: " +
            s"No match for ($childType of ${childType.getClass}, in parent $parent of ${parent.getClass})")
          None
      }
    })

    val childOfCurrentContext: Option[Seq[TreeNode]] =
      if (context.childNodeTypes.contains(selected.kind))
        Some(context.childrenNamed(selected.kind).collect {
          case mv: MutableView[_] => mv
        })
      else if (context.childNodeNames.contains(selected.kind))
        Some(context.childrenNamed(selected.kind).collect {
          case mv: MutableView[_] => mv
        })
      else None

    val fromGlobalTypes: Option[Seq[TreeNode]] =
      typeRegistry.findByName(selected.kind) flatMap {
        case t: Type =>
          t.findAllIn(context)
      }

    childOfCurrentContext orElse fromIdentifierInScope orElse fromGlobalTypes match {
      case Some(thing) => Right(thing)
      case None =>
        val cromulentChildren = context.childNodeTypes ++ context.childNodeNames
        Left(cromulentChildren)
    }
  }

  def invokePredicate(rugAs: ArtifactSource,
                      poa: ParameterValues,
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
          case _ =>
            DefaultEvaluator.evaluate(predicate, null, null, v, targetAlias, identifierMap, poa)
        }
    }
  }
}

object DefaultViewFinder extends DefaultViewFinder(DefaultTypeRegistry)
