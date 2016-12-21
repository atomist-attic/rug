package com.atomist.rug.kind.dynamic

import com.atomist.project.ProjectOperationArguments
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

  /**
    * Finds views, first looking at children of current scope,
    * then identifiers in scope, then global identifiers.
    */
  override def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                         poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {

    val fromIdentifierInScope: Option[Seq[MutableView[_]]] = identifierMap.get(selected.kind).flatMap(typ => {
      logger.debug(s"Getting type '${selected.kind}' from $typ")
      (typ, context) match {
        case (mg: Microgrammar, f: FileArtifactBackedMutableView) =>
          val l: Option[MatchListener] = None
          val container = mg.matchesInContainer(f.content, l)
          val views = container.childNodes collect {
            case moo: MutableContainerTreeNode =>
              new MutableContainerMutableView(moo, f)
          }
          f.registerUpdater(new MutableTreeNodeUpdater(container))
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

    val childOfCurrentContext: Option[Seq[MutableView[_]]] =
      if (context.childrenNames.contains(selected.kind))
        Some(context.children(selected.kind))
      else None

    val fromGlobalTypes: Option[Seq[MutableView[_]]] =
      typeRegistry.findByName(selected.kind) flatMap {
        case t: Type =>
          t.findIn(rugAs, selected, context, poa, identifierMap)
      }

    childOfCurrentContext orElse fromIdentifierInScope orElse fromGlobalTypes
  }
}

object DefaultViewFinder extends DefaultViewFinder(DefaultTypeRegistry)
