package com.atomist.rug.runtime.js

import java.util.regex.Pattern
import javax.script.{ScriptContext, ScriptException}

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.{RugJavaScriptException, RugRuntimeException}
import com.atomist.source.ArtifactSource
import com.coveo.nashorn_modules.{AbstractFolder, Folder, Require}
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{ClassFilter, NashornScriptEngine, NashornScriptEngineFactory, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Context superclass for evaluating JavaScript.
  * Creates a Nashorn ScriptEngineManager and can evaluate files and JavaScript fragments,
  * exposing the known vars in a typesafe way so we partly avoid the horrific detyped
  * Nashorn API.
  */
class JavaScriptContext(allowedClasses: Set[String] = Set.empty[String], atomistConfig: AtomistConfig = DefaultAtomistConfig) extends LazyLogging {

  private val commonOptions = Array("--optimistic-types", "--language=es6")

  /**
    * At the time of writing, allowedClasses were only used for test.
    *
    * If you do need to expose some classes to JS, then make sure you configure to use a locked down classloader and security manager
    */
  val engine: NashornScriptEngine =
    new NashornScriptEngineFactory().getScriptEngine(
      if (allowedClasses.isEmpty) commonOptions :+ "--no-java" else commonOptions,
      if (allowedClasses.isEmpty) null else Thread.currentThread().getContextClassLoader, //TODO - do we need our own loader here?
      new ClassFilter {
        override def exposeToScripts(s: String): Boolean = {
          allowedClasses.contains(s)
        }
      }
    ).asInstanceOf[NashornScriptEngine]


  def load(rugAs: ArtifactSource) : Unit = {

    configureEngine(engine, rugAs)
    val filtered = atomistConfig.atomistContent(rugAs)
      .filter(d => true,
        f => atomistConfig.isJsSource(f))
    //require all the atomist stuff
    for (f <- filtered.allFiles) {
      val varName = f.path.dropRight(3).replaceAll("/", "_").replaceAll("\\.", "\\$")
      try{
        engine.eval(s"exports.$varName = require('./${f.path.dropRight(3)}');") //because otherwise the loader doesn't know about the paths and can't resolve relative modules
      }catch {
        case x: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}",x)
        case x: RuntimeException => x.getCause match {
          case c: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}",c)
          case c => throw x
        }
        case x => throw x
      }
    }
  }

  /**
    * Information about a JavaScript var exposed in the project scripts
    *
    * @param key                name of the var
    * @param scriptObjectMirror interface for working with Var
    */
  case class Var(key: String, scriptObjectMirror: ScriptObjectMirror) {
  }


  /**
    * Return all the vars known to the engine that expose ScriptObjectMirror objects, with the key
    *
    * @return ScriptObjectMirror objects for all vars known to the engine
    */
  def vars: Seq[Var] = {
    val res = engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE)
      .get("exports").asInstanceOf[ScriptObjectMirror]
      .asScala
      .foldLeft(Seq[Var]())((acc: Seq[Var], kv) => {
        acc ++ extractVars(kv._2.asInstanceOf[ScriptObjectMirror])
      })
    res
  }

  private def extractVars(obj: ScriptObjectMirror): Seq[Var] = {
    obj.entrySet().asScala.flatMap(e => {
      (e.getKey, e.getValue) match {
        case (k, som: ScriptObjectMirror) => Some(Var(k, som))
        case _ => None
      }
    }).toSeq
  }

  private def configureEngine(scriptEngine: NashornScriptEngine, rugAs: ArtifactSource): Unit = {
    //so we can print stuff out from TS
    val consoleJs =
      """
        |console = {
        |   log: print,
        |   warn: print,
        |   error: print
        |};
      """.stripMargin
    scriptEngine.eval(consoleJs)
    try{
      Require.enable(engine, new ArtifactSourceBasedFolder(rugAs))
  }catch {
      case e: Exception =>
        throw new RuntimeException("Unable to set up ArtifactSource based module loader", e)
    }
  }

  private class ArtifactSourceBasedFolder private(var artifacts: ArtifactSource, val parent: Folder, val path: String) extends AbstractFolder(parent, path) {

    private val commentPattern: Pattern = Pattern.compile("^//.*$", Pattern.MULTILINE)
    //put single line vars like var x = new Blah() into exports
    private val varPattern: Pattern = Pattern.compile("var (\\w+) = new .*\\);\\s+$")

    private val letPattern: Pattern = Pattern.compile("var (\\w+) = \\{.*\\};\\s+$", Pattern.DOTALL)

    def this(artifacts: ArtifactSource, rootPath: String = "") {
      this(artifacts.underPath(rootPath), null, "")
    }

    def getFile(s: String): String = {
      val file = artifacts.findFile(s)
      if (file.isEmpty) return null
      //remove remove these source-map comments because they seem to be breaking nashorn :/
      val withoutComments = commentPattern.matcher(file.get.content).replaceAll("")

      //add export for those vars without them. TODO should be removed at some point once all have moved over!
      val js = new StringBuilder(withoutComments)
      append(varPattern, withoutComments, js)
      append(letPattern, withoutComments, js)
      js.toString()
    }

    def getFolder(s: String): Folder = {
      val dir = artifacts.findDirectory(s)
      if (dir.isEmpty) return null
      new ArtifactSourceBasedFolder(artifacts.underPath(s), this, getPath + s + "/")
    }


    def append(p: Pattern, str: String, sb: StringBuilder): Unit ={
      val m = p.matcher(str)
      if(m.find()){
        val varName = m.group(1)
        sb.append(s"\nexports.$varName = $varName;\n")
      }
    }
  }
}
