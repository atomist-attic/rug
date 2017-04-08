package com.atomist.rug.runtime.js

import java.util.regex.Pattern
import javax.script._

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugJavaScriptException
import com.atomist.rug.runtime.js.JavaScriptContext.EngineInitializer
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, FileArtifact}
import com.coveo.nashorn_modules.{AbstractFolder, Folder, Require}
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory, ScriptObjectMirror}
import jdk.nashorn.internal.runtime.ECMAException

import scala.collection.JavaConverters._

object JavaScriptContext {

  /**
    * Function that can initialize a NashornScriptEngine before use.
    * Typically evaluates JavaScript strings or binds objects.
    */
  type EngineInitializer = NashornScriptEngine => Unit

  /**
    * JavaScript snippet to evaluate to
    */
  private val ConsoleToSysOut: String =
    """
      |console = {
      |   log: print,
      |   warn: print,
      |   error: print
      |};
    """.stripMargin

  def redirectConsoleToSysOut(engine: NashornScriptEngine): Unit = {
    engine.eval(ConsoleToSysOut)
  }
}

/**
  * Context superclass for evaluating JavaScript.
  * Creates a Nashorn ScriptEngineManager and can evaluate files and JavaScript fragments,
  * exposing the known vars in a typesafe way so we partly avoid the horrific detyped
  * Nashorn API.
  *
  * One of these per rug please, or else they may stomp on one-another
  *
  * @param initializer function to initialize the engine:
  *                          for example, binding objects or evaluating standard scripts
  */
class JavaScriptContext(val rugAs: ArtifactSource,
                        val atomistConfig: AtomistConfig = DefaultAtomistConfig,
                        bindings: Bindings = new SimpleBindings(),
                        initializer: EngineInitializer = JavaScriptContext.redirectConsoleToSysOut)
  extends LazyLogging {

  val engine: NashornScriptEngine =
    new NashornScriptEngineFactory()
      .getScriptEngine("--optimistic-types", "--language=es6", "--no-java")
      .asInstanceOf[NashornScriptEngine]

  engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

   initializer(engine)

  try {
    Require.enable(engine, new ArtifactSourceBasedFolder(rugAs))
  }
  catch {
    case e: Exception =>
      throw new RuntimeException("Unable to set up ArtifactSource based module loader", e)
  }

  val atomistContent: ArtifactSource = atomistConfig.atomistContent(rugAs)

  // Require all the Atomist stuff
  atomistContent
    .filter(_ => true, atomistConfig.isJsSource)
    .allFiles.foreach(evaluate)

  /**
    * Evaluate the given file, with proper exports handling.
    * NB: Must be a file from atomistContent.
    */
  def evaluate(f: FileArtifact): Unit = {
    val varName = f.path.dropRight(3).replaceAll("/", "_").replaceAll("\\.", "\\$")
    try {
      // Because otherwise the loader doesn't know about the paths and can't resolve relative modules
      val toEval = s"exports.$varName = require('./${f.path.dropRight(3)}');"
      // TODO is it safe to keep calling this?
      engine.put(ScriptEngine.FILENAME, f.path)
      engine.eval(toEval)
    }
    catch {
      case x: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}", x)
      case x: RuntimeException => x.getCause match {
        case c: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}", c)
        case _ => throw x
      }
    }
  }

  /**
    * Translate to more informative exceptions, allowing for possible JS to TS translation
    */
  @throws[JavaScriptRuntimeException]
  def withEnhancedExceptions[T](result: => T): T = {
    try {
      result
    }
    catch {
      case ecmaEx: ECMAException =>
        throw ExceptionEnhancer.enhanceIfPossible(rugAs, ecmaEx)
    }
  }

  /**
    * Return all the vars known to the engine that expose ScriptObjectMirror objects, with the key.
    *
    * @return ScriptObjectMirror objects for all vars known to the engine
    */
  def vars: Seq[Var] = {
    engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE)
      .get("exports").asInstanceOf[ScriptObjectMirror]
      .asScala.collect {
      case (_, v: ScriptObjectMirror) => extractVars(v)
    }.flatten.toSeq
  }

  private def extractVars(obj: ScriptObjectMirror): Seq[Var] = {
    obj.entrySet().asScala.flatMap(e => {
      (e.getKey, e.getValue) match {
        case (k, som: ScriptObjectMirror) => Some(Var(k, som))
        case _ => None
      }
    }).toSeq
  }

  private class ArtifactSourceBasedFolder private(val artifacts: ArtifactSource, val parent: Folder, val path: String)
    extends AbstractFolder(parent, path) {

    private val commentPattern: Pattern = Pattern.compile("^//.*$", Pattern.MULTILINE)

    def this(artifacts: ArtifactSource, rootPath: String = "") {
      this(artifacts.underPath(rootPath), null, "")
    }

    def getFile(s: String): String = {
      val file = artifacts.findFile(getPath + s)
      if (file.isEmpty) return null
      //remove these source-map comments because they seem to be breaking nashorn :/
      commentPattern.matcher(file.get.content).replaceAll("")
    }

    def getFolder(s: String): Folder = {
      val dir = artifacts.findDirectory(getPath + s)
      if (dir.isEmpty) return null
      new ArtifactSourceBasedFolder(artifacts, this, getPath + s + "/")
    }
  }

  override def toString: String =
    s"${getClass.getSimpleName} backed by ArtifactSource with ${rugAs.totalFileCount} artifacts\n" +
    s"User JS files=${ArtifactSourceUtils.prettyListFiles(atomistContent.filter(_ => true, f =>
      atomistConfig.isJsSource(f) || atomistConfig.isJsTest(f)))}\n" +
    s" - test features [${atomistContent.allFiles.filter(f => f.name.endsWith(".feature"))}]"

}

/**
  * Information about a JavaScript var exposed in the project scripts
  *
  * @param key                name of the var
  * @param scriptObjectMirror interface for working with Var
  */
case class Var(key: String, scriptObjectMirror: ScriptObjectMirror)
