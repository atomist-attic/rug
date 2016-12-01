package com.atomist.rug.ts

import com.atomist.param.Parameter
import com.atomist.rug.parser._
import com.atomist.rug.{RugEditor, RugProgram}
import com.atomist.util.scalaparsing.{JavaScriptBlock, Literal, ToEvaluate}
import com.atomist.rug.compiler.Compiler
import com.atomist.source.{ArtifactSource, StringFileArtifact}
import com.atomist.util.SaveAllDescendantsVisitor
import com.atomist.util.lang.JavaHelpers
import jdk.nashorn.api.scripting.ScriptObjectMirror

object NashornUtils {

  import scala.collection.JavaConverters._

  def extractProperties(som: ScriptObjectMirror): Map[String, Object] =
    som.entrySet().asScala.map(me => me.getKey -> me.getValue).toMap
}

/**
  * Turns Rug into Typescript.
  */
class RugTranspiler(config: RugTranspilerConfig = RugTranspilerConfig(),
                    rugParser: RugParser = new ParserCombinatorRugParser())
  extends Compiler {

  val helper = new TypeScriptGenerationHelper()

  override def compile(source: ArtifactSource): ArtifactSource = {
    val typeScripts =
      source.allFiles
        .filter(f => f.name.endsWith(".rug"))
        .map(f => {
          val ts = transpile(f.content)
          StringFileArtifact(f.path.dropRight(4) + ".ts", ts)
        })
    source + typeScripts
  }

  override def supports(source: ArtifactSource): Boolean = true

  /**
    * Turn Rug into Typescript.
    */
  def transpile(rug: String): String = {
    val rugs = rugParser.parse(rug)
    emit(rugs)
  }

  def emit(rugs: Seq[RugProgram]): String = {
    val ts = new StringBuilder()
    ts ++= licenseHeader
    ts ++= imports

    for {
      rug <- rugs
    } {
      val pc = parameterClassWithName(rug)
      if (pc.isDefined) {
        ts ++= helper.indented(pc.get.body, 0)
      }
      ts ++= tsProg(rug, pc)
    }

    ts.toString
  }

  case class ParamClass(name: String, body: String)

  private def parameterClassWithName(rug: RugProgram): Option[ParamClass] = {
    def emitParam(p: Parameter): String = {
      s"${p.name}: string"
    }

    if (rug.parameters.nonEmpty) {
      val paramClassName = rug.name + "Parameters"
      val params = rug.parameters.map(emitParam).mkString(config.separator)
      val headerComment = s"${helper.toJsDoc(s"Parameters for operation ${rug.name}")}"
      val body =
        s"""
           |$headerComment
           |class $paramClassName extends ParametersSupport {
           |
           |${helper.indented(params, 1)}
           |
           |}
      """.stripMargin
      Some(ParamClass(paramClassName, body))
    }
    else None
  }

  // Emit an entire program
  private def tsProg(rug: RugProgram, pc: Option[ParamClass]): String = {
    val ts = new StringBuilder()
    ts ++= helper.toJsDoc(rug.description)

    ts ++= decorators(rug)

    ts ++= s"\nclass ${rug.name} "
    rug match {
      case ed: RugEditor =>
        ts ++= editorHeader(ed, pc)
        ts ++= helper.indented(editorBody(ed, pc), 1)
        ts ++= "}"
        ts ++= config.separator
    }
    ts ++= "}\n"
    ts.toString()
  }

  private def decorators(rug: RugProgram): String = {
    //    @tag("java")
    //    @tag("maven")
    //    @editor("A nice little editor")

    s"@editor('${rug.name}')"
  }

  private def paramClassName(pc: Option[ParamClass]) = pc match {
    case None => "Parameters"
    case Some(t) => t.name
  }

  private def editorHeader(ed: RugEditor, pc: Option[ParamClass]): String = {
    s"implements ProjectEditor<${paramClassName(pc)}> {\n\n"
  }

  private def editorBody(ed: RugEditor, pc: Option[ParamClass]): String = {
    val ts = new StringBuilder()

    ts ++= constructorCode(ed)

    ts ++= config.separator

    ts ++= s"${config.editMethodName}(${config.projectVarName}: Project, ${config.parametersVarName}: ${paramClassName(pc)}) {${config.separator}"

    // Add aliases needed in this case
    val v = new SaveAllDescendantsVisitor
    ed.accept(v, 0)
    if ((ed.computations.nonEmpty || v.descendants.exists(_.isInstanceOf[JavaScriptBlock])) && ed.parameters.nonEmpty) {
      for (p <- ed.parameters) {
        ts ++= helper.indented(s"let ${p.name} = ${config.parametersVarName}.${p.name}\n", 1)
      }
      ts ++= config.separator
    }

    for (l <- ed.computations) {
      ts ++= helper.indented(letCode(ed, l), 1)
      ts ++= config.separator
    }

    for (a <- ed.actions) {
      ts ++= helper.indented(actionCode(ed, a, config.projectVarName, 1), 1)
      ts ++= config.separator
    }
    ts ++= helper.indented(s"return new Result(Status.Success, 'Editor [${ed.name}] executed OK')\n", 1)
    ts.toString
  }

  private def letCode(prog: RugProgram, l: Computation): String = {
    s"let ${l.name} = ${extractValue(prog, l.te, config.projectVarName)}"
  }

  private def constructorCode(ed: RugEditor): String = {
    def argList = ed.runs.map(roo => {
      s"${JavaHelpers.lowerize(roo.name)}: ${roo.name}"
    }).mkString(" ,")
    def body = ed.runs.map(roo => {
      s"this.${JavaHelpers.lowerize(roo.name)} = ${JavaHelpers.lowerize(roo.name)}"
    }).mkString(" ,")

    s"""
       |constructor($argList) {
       |  ${helper.indented(body, 1)}
       |}
     """.stripMargin
  }

  private def actionCode(prog: RugProgram, a: Action, outerAlias: String, indentDepth: Int): String = a match {
    case w: With =>
      helper.indented(withBlockCode(prog, w, outerAlias), indentDepth)
    case roo: RunOtherOperation =>
      helper.indented(rooCode(roo), indentDepth)
  }

  private def rooCode(roo: RunOtherOperation): String = {
    val opName = JavaHelpers.lowerize(roo.name)
    s"$opName.${config.editMethodName}(${config.projectVarName}, ${config.parametersVarName})"
  }

  private def withBlockCode(prog: RugProgram, wb: With, outerAlias: String): String = {
    // TODO problem is working out plan to get it if it's not simple descent - or does tree execution engine do it?
    // Or we expose a typescript helper

    val doSteps =
      (for (d <- wb.doSteps)
        yield {
          doStepCode(prog, d, wb.alias)
        }).mkString("\n")

    val blockHeader =
      s"for (let ${wb.alias} of $outerAlias.${wb.kind}s()) {"

    val blockBody = wrapInCondition(prog, wb.predicate, doSteps, wb.alias, 1)
    blockHeader + "\n" + helper.indented(blockBody, 1) + "\n}"
  }

  private def extractValue(prog: RugProgram, te: ToEvaluate, outerAlias: String): String = te match {
    case l: Literal[_] => l.value match {
      case null => "null"
      case s: String => s""""$s""""
      case x => x.toString
    }
    case js: JavaScriptBlock => handleJs(js.content)
    case rf: ParsedRegisteredFunctionPredicate =>
      emit(rf, outerAlias)
    case idf: IdentifierFunctionArg =>
      if (prog.parameters.exists(_.name.equals(idf.name)))
        config.parametersVarName + "." + idf.name
      else
        idf.name
    case wfa: WrappedFunctionArg => extractValue(prog, wfa.te, outerAlias)
  }

  private def emit(rf: ParsedRegisteredFunctionPredicate, outerAlias: String): String = {
    outerAlias + "." + rf.function
  }

  // Handle possibly multi-line JS
  private def handleJs(js: String): String = {
    if (js.contains("\n") || js.contains("return")) {
      arrowFunctionify(js)
    }
    else {
      js
    }
  }

  private def arrowFunctionify(js: String) =
    s"(() => { \n$js })()"

  private def doStepCode(prog: RugProgram, doStep: DoStep, alias: String): String = doStep match {
    case f: FunctionDoStep if "eval".equals(f.function) =>
      // We are only to evaluate the single arg for a side effect
      require(f.args.size == 1)
      extractValue(prog, f.args.head, "")
    case f: FunctionDoStep =>
      val args: Seq[String] = f.args
        .map(a => extractValue(prog, a, alias))
      // Apply special rules for map
      val extraArg: Option[String] = if ("merge".equals(f.function)) {
        Some(parametersToJsonObject(prog, alias))
      }
      else None

      val argsToUse = (args ++ extraArg).mkString(", ")
      s"$alias.${f.function}($argsToUse)"
    case wds: WithDoStep =>
      helper.indented(withBlockCode(prog, wds.wth, alias), 1)
  }

  private def parametersToJsonObject(prog: RugProgram, outerAlias: String): String = {
    val computations = prog.computations
      .map(comp => extractValue(prog, comp.te, outerAlias))
      .mkString("\n")
    val js =
      s"""
         |let allParams = clone(${config.parametersVarName})
         |$computations
         |return allParams
       """.stripMargin
    arrowFunctionify(js)
  }

  // Wrap in an if statement
  private def wrapInCondition(prog: RugProgram, predicate: Predicate, block: String, alias: String, indentDepth: Int): String = {
    val argContent = predicate match {
      case TruePredicate => "true"
      case FalsePredicate => "false"
      case pjsf: ParsedJavaScriptFunction => handleJs(pjsf.js.content)
      case prfp: ParsedRegisteredFunctionPredicate => emit(prfp, alias)
      case comp: ComparisonPredicate =>
        extractValue(prog, comp.a, alias) + "() == " + extractValue(prog, comp.b, alias)
    }
    s"if ($argContent) {\n${helper.indented(block, 1)}\n}"
  }

  val licenseHeader =
    """
      |// Generated by Rug to TypeScript transpiler.
      |// To take ownership of this file, simply delete the .rug file
    """.stripMargin

  // TODO remove the utility function here: Move to a standard class
  val imports =
  """
    |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
    |import {Parameters} from 'user-model/operations/Parameters'
    |import {ParametersSupport} from 'user-model/operations/Parameters'
    |import {Project} from 'user-model/model/Core'
    |import {Result,Status} from 'user-model/operations/Result'
    |
    |import {tag} from 'user-model/support/Metadata'
    |import {editor} from 'user-model/support/Metadata'
    |
    |
    |function clone(obj) {
    |    if (null == obj || "object" != typeof obj) return obj;
    |    var copy = {};
    |    for (var attr in obj) {
    |        if (obj.hasOwnProperty(attr)) copy[attr] = obj[attr];
    |    }
    |    return copy;
    |}
  """.stripMargin

}

trait TypeScriptGenerationConfig {

  def indent: String

  def separator: String

}

case class RugTranspilerConfig(
                                indent: String = "    ",
                                separator: String = "\n\n",
                                projectVarName: String = "project",
                                parametersVarName: String = "parameters",
                                editMethodName: String = "edit"
                              )
  extends TypeScriptGenerationConfig
