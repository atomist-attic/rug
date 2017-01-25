package com.atomist.rug.ts

import java.util.Collections

import com.atomist.param.Parameter
import com.atomist.rug.compiler.Compiler
import com.atomist.rug.parser._
import com.atomist.rug.{RugEditor, RugProgram}
import com.atomist.source.{ArtifactSource, StringFileArtifact}
import com.atomist.util.SaveAllDescendantsVisitor
import com.atomist.util.lang.{JavaHelpers, TypeScriptGenerationHelper}
import com.atomist.util.scalaparsing.{JavaScriptBlock, Literal, ToEvaluate}

/**
  * Turns Rug into Typescript.
  */
class RugTranspiler(config: RugTranspilerConfig = RugTranspilerConfig(),
                    rugParser: RugParser = new ParserCombinatorRugParser())
  extends Compiler {

  val helper = new TypeScriptGenerationHelper()

  override def name = "Rug Transpiler"

  override def extensions =  Collections.singleton("rug")

  // Make sure the transpiler gets loaded first
  override def order: Int = Integer.MIN_VALUE

  val RugExtension = ".rug"

  override def compile(source: ArtifactSource): ArtifactSource = {
    val typeScripts =
      source.allFiles
        .filter(f => f.name.endsWith(RugExtension))
        .map(f => {
          val ts = transpile(f.content)
          StringFileArtifact(rugPathToTsPath(f.path), ts)
        })
    source + typeScripts
  }

  def rugPathToTsPath(rugPath: String): String = rugPath.dropRight(RugExtension.length) + ".ts"

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
    ts ++= standardImports
    ts ++= specificImports(rugs)

    for {
      rug <- rugs
    } {
      ts ++= tsProg(rug)
    }
    // println(ts)
    ts.toString
  }

  private def specificImports(rugs: Seq[RugProgram]): String = {
    val set = importSet(rugs)
    val ordered = set.toList.sorted
    val importLines = ordered
      .map(imp => s"import {${JavaHelpers.toJavaClassName(imp)}} from '@atomist/rug/model/Core'")
    val specImports = importLines.mkString("\n")
    specImports
  }

  // Set of all imports in these rugs
  private def importSet(rugs: Seq[RugProgram]): Set[String] = {
    val v = new SaveAllDescendantsVisitor()
    rugs.foreach(rug => rug.accept(v, 0))
    (v.descendants collect {
      case w: With if !"Project".equals(w.kind) => w.kind
    }).toSet
  }

  private def paramsInterface(params: Seq[Parameter]) : String = {
    if(params.isEmpty){
      return ""
    }
    s"""interface Parameters {
       |
       |${helper.indented(params.map(p => p.getName + ": string").mkString("\n"),1)}
       |
       |}
     """.stripMargin
  }

  // Emit an entire program
  private def tsProg(rug: RugProgram): String = {
    val ts = new StringBuilder()
    ts ++= helper.toJsDoc(rug.description)

    rug match {
      case ed: RugEditor =>
        ts ++= config.separator
        ts ++= paramsInterface(rug.parameters)
    }

    ts ++= s"\nclass ${rug.name} "
    rug match {
      case ed: RugEditor =>
        ts ++= editorHeader(ed)
        ts ++= helper.indented(editorBody(ed), 1)
        ts ++= "}"
        ts ++= config.separator
    }
    ts ++= "}\n"

    // Check that editors have distinct names
    ts ++= s"""export let editor_${JavaHelpers.lowerize(rug.name)} = new ${rug.name}();"""
    ts.toString()
  }

  private def editorHeader(ed: RugEditor): String = {
    s"implements ProjectEditor {\n\n"
  }

  private def editorBody(ed: RugEditor): String = {
    val ts = new StringBuilder()

    ts ++= s"""name: string = "${ed.name}""""
    ts ++= config.separator
    ts ++= s"""description: string = "${ed.description}""""
    ts ++= config.separator

    if(ed.tags.nonEmpty){
      ts ++= s"""tags: string[] = ${ed.tags.toArray};"""
      ts ++= config.separator
    }

    if(ed.parameters.nonEmpty){
      ts ++= s"""parameters: Parameter[] = ${toTSParameters(ed.parameters)};"""
      ts ++= config.separator
    }
    ts ++= config.separator

    ts ++= s"${config.editMethodName}(${config.projectVarName}: Project${if(ed.parameters.nonEmpty) ", parameters: Parameters" else ""}) {${config.separator}"

    ts ++= helper.indented(s"""let eng: PathExpressionEngine = project.context().pathExpressionEngine();${config.separator}""", 1)

    ts ++= config.separator
    // Add aliases needed in this case
    val v = new SaveAllDescendantsVisitor
    ed.accept(v, 0)
    if ((ed.computations.nonEmpty || v.descendants.exists(_.isInstanceOf[JavaScriptBlock])) && ed.parameters.nonEmpty) {
      for (p <- ed.parameters) {
        ts ++= helper.indented(s"let ${p.name} = parameters.${p.name}\n", 1)
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
    ts.toString
  }

  private def letCode(prog: RugProgram, l: Computation): String = {
    s"let ${l.name} = ${extractValue(prog, l.te, config.projectVarName)}"
  }

  private def actionCode(prog: RugProgram, a: Action, outerAlias: String, indentDepth: Int): String = a match {
    case w: With =>
      helper.indented(withBlockCode(prog, w, outerAlias), indentDepth)
    case roo: RunOtherOperation =>
      helper.indented(rooCode(roo,prog.parameters), indentDepth)
    case sba: ScriptBlockAction =>
      sba.scriptBlock.content
    case _ => ???
  }

  private def rooCode(roo: RunOtherOperation, params: Seq[Parameter]): String = {
    val kvs = ", {"+ params.map(p => s"""${p.getName}: ${p.getName}""").mkString(", ") + "}"
    s"""project.editWith("${roo.name}"$kvs)"""
  }

  private def withBlockCode(prog: RugProgram, wb: With, outerAlias: String): String = {
    val doSteps =
      (for (d <- wb.doSteps)
        yield {
          doStepCode(prog, d, wb.alias)
        }).mkString("\n")

    val pathExpr = s"'//${wb.kind}()'"
    val descent = s"eng.with<${wb.kind}>($outerAlias, $pathExpr, ${wb.alias} => {"
    val blockBody = wrapInCondition(prog, wb.predicate, doSteps, wb.alias, 1)

    val referencingAccessibleThing = Set("Project").contains(wb.kind)
      //!wb.kind.equals(outerAlias)
    if (!referencingAccessibleThing) {
      descent + "\n" + helper.indented(blockBody, 1) + "\n})"
    }
    else {
      // Special case where inner and outer block are the same type, like "with Project" under a project
      (if (wb.alias.equals(wb.kind)) "" else s"let ${wb.alias} = ${JavaHelpers.lowerize(wb.kind)}\n") +
      helper.indented(blockBody, 1)
    }
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
        "parameters." + idf.name
        //idf.name
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
    val params = prog.parameters
    val kvs = if(params.nonEmpty) ", {"+ params.map(p => s"""${p.getName}: ${p.getName}""").mkString(", ") + "}" else "{}"

    val js =
      s"""
         |let allParams = $kvs
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

  private def toTSParameters(params: Seq[Parameter]) : String = {

    val b = new StringBuilder("[")

    params.foreach(p => {
      b.append("{")
      val pb = new StringBuilder()
      appendIfNotNull(pb,"name", p.getName)
      appendIfNotNull(pb,"description", p.getDescription)

      appendIfNotNull(pb,"pattern", p.getPattern.replace("\\","\\\\"))
      appendIfNotNull(pb,"maxLength",p.getMaxLength)
      appendIfNotNull(pb,"minLength",p.getMinLength)
      appendIfNotNull(pb,"defaultValue",p.getDefaultValue)
      appendIfNotNull(pb, "defaultRef",p.getDefaultRef)
      appendIfNotNull(pb, "displayName",p.getDisplayName)
      appendIfNotNull(pb, "validInput",p.getValidInputDescription)
      pb.setLength(pb.length - 2)
      b.append(pb)
      b.append("}, ")
    })

    b.setLength(b.length - 2)

    b.append("]").toString()
  }

  private def appendIfNotNull(b: StringBuilder, key: String, value: Any): Unit ={
    if(value != null && !"".equals(value)){
      b.append(key).append(": ")
      value match {
        case s: String => b.append("\"").append(value).append("\"")
        case s: Int => b.append(value)
      }
      b.append(", ")
    }
  }
  val licenseHeader =
    """
      |// Generated by Rug to TypeScript transpiler.
      |// To take ownership of this file, simply delete the .rug file
    """.stripMargin

  val standardImports =
    """
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Project} from '@atomist/rug/model/Core'
      |import {Parameter} from '@atomist/rug/operations/RugOperation'
      |
      |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
      |
    """.stripMargin

}

case class RugTranspilerConfig(
                                indent: String = "    ",
                                separator: String = "\n\n",
                                projectVarName: String = "project",
                                editMethodName: String = "edit"
                              )
  extends TypeScriptGenerationConfig

// For the JDK ServiceLoader we need a no arg constructor
class DefaultRugTranspiler extends RugTranspiler {}