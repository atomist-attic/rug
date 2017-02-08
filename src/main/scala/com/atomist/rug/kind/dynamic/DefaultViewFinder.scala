package com.atomist.rug.kind.dynamic

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.spi.{MutableView, Type, TypeRegistry}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.microgrammar.Microgrammar
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionEngine}
import com.typesafe.scalalogging.LazyLogging

class DefaultViewFinder(typeRegistry: TypeRegistry)
  extends ViewFinder with LazyLogging {

  final def findIn(
                    rugAs: ArtifactSource,
                    selected: Selected,
                    context: TreeNode,
                    poa: ProjectOperationArguments,
                    identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    try {
      findAllIn(rugAs, selected, context, poa, identifierMap)
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

  override def findAllIn(context: TreeNode) = findAllIn(null, null, context, null, null)
  /**
    * Finds views, first looking at children of current scope,
    * then identifiers in scope, then global identifiers.
    */
  def findAllIn(rugAs: ArtifactSource, selected: Selected, context: TreeNode,
                poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {

    val fromIdentifierInScope: Option[Seq[MutableView[_]]] = identifierMap.get(selected.kind).flatMap(typ => {
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
          val evaluated: Either[String, List[TreeNode]] = pe.evaluate(context, pex, DefaultTypeRegistry, None)
          evaluated match {
            case Right(nodes) =>
              //nodes.map(tn => new MutableContainerTreeNodeMutableView(tn.asInstanceOf[MutableContainerTreeNode], context))
              Some(nodes map {
                case mv: MutableView[_] => mv
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

    childOfCurrentContext orElse fromIdentifierInScope orElse fromGlobalTypes
  }
}

object DefaultViewFinder extends DefaultViewFinder(DefaultTypeRegistry)
