package com.atomist.rug.runtime.rugdsl

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.project.common.support.ProjectOperationSupport
import com.atomist.rug._
import com.atomist.rug.runtime.NamespaceUtils
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer

trait ContextAwareProjectOperation extends ProjectOperation {

  /**
    * Should be called before use to expose the context from which other editors can be found.
    * Will compute parameters and validate dependencies in our Uses blocks.
    *
    * @param ctx context where we can find this editor
    */
  def setContext(ctx: Seq[ProjectOperation]): Unit

}

abstract class RugDrivenProjectOperation(
                                          val program: RugProgram,
                                          val rugAs: ArtifactSource,
                                          val kindRegistry: TypeRegistry,
                                          val namespace: Option[String])
  extends ContextAwareProjectOperation
    with ProjectOperationSupport
    with RugOperationSupport
    with LazyLogging {

  import NamespaceUtils._

  private var context: Seq[ProjectOperation] = Nil

  program.parameters.foreach(
    addParameter
  )

  program.tags.foreach(t =>
    addTag(Tag(t, t))
  )

  protected override def operations = context

  override def imports: Seq[Import] = program.imports

  override def setContext(ctx: Seq[ProjectOperation]): Unit = {
    this.context = ctx
    validateUses(ctx)
    validateParameters(ctx)
    onSetContext()
  }

  private def validateParameters(ctx: Seq[ProjectOperation]): Unit = {
    val paramsToAdd = new ListBuffer[Parameter]()
    for {
      roo <- program.runs
      op <- ctx.find(_.name.equals(namespaced(roo.name, namespace)))
      newParam <- op.parameters
      if !program.parameters.exists(existingParam => existingParam.getName.equals(newParam.getName))
      if !paramsToAdd.exists(alreadyAdded => alreadyAdded.getName.equals(newParam.getName))
      if !program.computations.exists(computed => computed.name.equals(newParam.getName))
      if !roo.args.exists(arg => arg.parameterName.contains(newParam.getName))
    } {
      paramsToAdd.append(newParam)
    }

    for (newParam <- paramsToAdd)
      addParameter(newParam)
    for {
      roo <- program.runs
      arg <- roo.args
      op <- ctx.find(_.name.equals(namespaced(roo.name, namespace)))
    } {
      if (arg.parameterName.isEmpty)
        throw new RugRuntimeException(name, s"Operation $name - All local arguments to 'run' statement must be named: Offending argument was $arg in ${roo.name}", null)
      else if (!op.parameters.exists(p => p.name.equals(arg.parameterName.get)))
        throw new RugRuntimeException(name, s"Operation $name - Local argument ${arg.parameterName.get} to 'run ${roo.name}' does not match one of the operation's arguments, which are [${op.parameters.map(_.name).mkString(",")}]", null)
    }
  }

  private def validateUses(ctx: Seq[ProjectOperation]): Unit = {
    val missingUsed =
      for {
        used <- program.runs
        resolved = resolve(used.name, namespace, ctx, program.imports)
        if resolved.isEmpty
      }
        yield used.name
    if (missingUsed.nonEmpty)
      throw new UndefinedRugUsesException(name,
        s"'run' operation(s) not found when processing operation $name: [${missingUsed.mkString(",")}]. " +
          s"Known operations are [${ctx.map(_.name).mkString(",")}]",
        missingUsed)

    val simpleNamesForImports = program.imports.map(imp => imp.simpleName)
    val namesActuallyUsed = program.runs.map(roo => roo.name)
    val usedNotActuallyUsed = simpleNamesForImports.filter(sn => !namesActuallyUsed.contains(sn))
    if (usedNotActuallyUsed.nonEmpty)
      throw new UnusedUsesException(name,
        s"Unused uses in operation $name: [${missingUsed.mkString(",")}]. " +
          s"Offending uses are [${usedNotActuallyUsed.mkString(",")}]",
        usedNotActuallyUsed)
  }

  /**
    * Subclasses can override this to perform further validity checks and initialization.
    * They can now rely on the context.
    */
  protected def onSetContext(): Unit

  override val name = namespaced(program.name, namespace)

  override def description = program.description

  override def toString =
    s"${getClass.getName} name=${program.name},description=${program.description}, wrapping \n$program"
}
