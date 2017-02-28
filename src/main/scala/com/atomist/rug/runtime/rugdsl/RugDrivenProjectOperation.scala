package com.atomist.rug.runtime.rugdsl

import com.atomist.param.{Parameter, Tag}
import com.atomist.rug._
import com.atomist.rug.runtime._
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer

abstract class RugDrivenProjectOperation(
                                          val program: RugProgram,
                                          val rugAs: ArtifactSource,
                                          val kindRegistry: TypeRegistry,
                                          override val externalContext: Seq[AddressableRug])
  extends RugOperationSupport
    with ParameterizedRug
    with Rug
    with LazyLogging {

  private val params = new ListBuffer[Parameter] ++ program.parameters

  override def addToArchiveContext(rugs: Seq[Rug]): Unit = {
    super.addToArchiveContext(rugs)
    validateUses
    validateParameters()
  }

  override def parameters: Seq[Parameter] = {
    //remove duplicates and maintain ordering
    params.foldLeft[Seq[Parameter]](Nil) { (acc, par) =>
      if(acc.exists(p => p.name == par.name)){
        acc
      }else{
        acc :+ par
      }
    }
  }

  override def tags: Seq[Tag] = program.tags.map(t => Tag(t, t))

  override def imports: Seq[Import] = program.imports

  override def name: String = program.name

  override def description: String = program.description

  private def validateParameters(): Unit = {
    val paramsToAdd = new ListBuffer[Parameter]()
    for {
      roo <- program.runs
      op <- this.findParameterizedRug(roo.name)
      newParam <- op.parameters
      if !program.parameters.exists(existingParam => existingParam.getName.equals(newParam.getName))
      if !paramsToAdd.exists(alreadyAdded => alreadyAdded.getName.equals(newParam.getName))
      if !program.computations.exists(computed => computed.name.equals(newParam.getName))
      if !roo.args.exists(arg => arg.parameterName.contains(newParam.getName))
    } {
      paramsToAdd.append(newParam)
    }

    for (newParam <- paramsToAdd)
      params += newParam
    for {
      roo <- program.runs
      arg <- roo.args
      op <-  this.findParameterizedRug(roo.name)
    } {
      if (arg.parameterName.isEmpty)
        throw new RugRuntimeException(name, s"Operation $name - All local arguments to 'run' statement must be named: Offending argument was $arg in ${roo.name}", null)
      else if (!op.parameters.exists(p => p.name.equals(arg.parameterName.get)))
        throw new RugRuntimeException(name, s"Operation $name - Local argument ${arg.parameterName.get} to 'run ${roo.name}' does not match one of the operation's arguments, which are [${op.parameters.map(_.name).mkString(",")}]", null)
    }
  }

  private def validateUses: Unit = {
    val missingUsed =
      for {
        used <- program.runs
        resolved = resolve(used.name)
        if resolved.isEmpty
      }
        yield used.name
    if (missingUsed.nonEmpty)
      throw new UndefinedRugUsesException(name,
        s"'run' operation(s) not found when processing operation $name: [${missingUsed.mkString(",")}]. " +
          s"Known operations are [${allRugs.map(_.name).mkString(",")}]",
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

  override def toString =
    s"${getClass.getName} name=${program.name},description=${program.description}, wrapping \n$program"

}
