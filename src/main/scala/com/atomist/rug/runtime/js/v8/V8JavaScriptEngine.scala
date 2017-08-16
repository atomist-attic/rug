package com.atomist.rug.runtime.js.v8

import java.nio.file.{Files, StandardCopyOption}

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime.js.interop.JavaScriptRuntimeException
import com.atomist.source.file.FileSystemArtifactSource
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.eclipsesource.v8._
import com.typesafe.scalalogging.LazyLogging


/**
  * v8 implementation. Currently a single v8
  */
class V8JavaScriptEngine(val rugAs: ArtifactSource,
                         val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends JavaScriptEngine
    with LazyLogging
    with JavaScriptUtils {

  private val node = new NodeWrapper(NodeJS.createNodeJS())

  val atomistContent: ArtifactSource = atomistConfig.atomistContent(rugAs)

  //  private val exports: ListBuffer[JavaScriptMember] = new ListBuffer[JavaScriptMember]()

  // Extract all the Atomist stuff

  private val root = rugAs match {
    case fs: FileSystemArtifactSource => fs.id.rootFile.toPath
    case mem =>
      val tempRoot = Files.createTempDirectory("rug")
      mem.allFiles.foreach { memFile =>
        val fsFile = tempRoot.resolve(memFile.path)
        fsFile.getParent.toFile.mkdirs()
        val io = memFile.inputStream()
        try {
          Files.copy(io, fsFile, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          io.close()
        }
      }
      tempRoot
  }

  //eval all the atomist stuff
  atomistContent
    .filter(_ => true, atomistConfig.isJsSource)
    .allFiles.foreach(evaluate)

  /**
    * val varName = f.path.dropRight(3).replaceAll("/", "_").replaceAll("\\.", "\\$")
    * try {
    * // Because otherwise the loader doesn't know about the paths and can't resolve relative modules
    * val toEval = s"exports.$varName = require('./${f.path.dropRight(3)}');"
    * // TODO is it safe to keep calling this?
    *engine.put(ScriptEngine.FILENAME, f.path)
    *engine.eval(toEval)
    * }
    *
    * @param f
    */

  override def evaluate(f: FileArtifact): Unit = {
    Proxy.withMemoryManagement(node, {
      val path = root.resolve(f.path)
      val varName = f.path.dropRight(3).replaceAll("/", "_").replaceAll("\\.", "\\$")
      val exports = node.getRuntime.get("exports") match {
        case o: V8Object if !o.isUndefined=> o
        case _ =>
          val newExports = new V8Object(node.getRuntime)
          node.getRuntime.add("exports", newExports)
          newExports
      }

      node.node.require(path.toFile) match {
        case o: V8Object =>
          exports.add(varName, o)
        case _ =>
      }
    })
  }

  override def members(): Seq[JavaScriptMember] = {
    Proxy.withMemoryManagement(node, {
      node.node.getRuntime.get("exports") match {
        case o: V8Object =>
          val mapped: Map[String, AnyRef] = o.getKeys.map(k => (k, o.get(k))).toMap
          val objects = mapped.collect {
            case (key, o:V8Object) => (key, o)
            case _ =>
          }.toMap

          objects.map(p => {
            val members = p._2
            JavaScriptMember(p._1, new ReleasedV8JavaScriptObject(node, p._2, Seq(p._1)))
          }).toSeq
      }
    })
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
      case ecmaEx: V8ScriptExecutionException =>
        throw ExceptionEnhancer.enhanceIfPossible(rugAs, ecmaEx)
    }
  }

  override def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): AnyRef = {
    Proxy.withMemoryManagement(node, {
      withEnhancedExceptions {
        if (params.nonEmpty) {
          setParameters(jsVar, params.get.parameterValues)
        }
        val v8o = jsVar.asInstanceOf[V8JavaScriptObject].getNativeObject.asInstanceOf[V8Object]

        val proxied = args.map(a => Proxy.ifNeccessary(node, a))
        val fn = jsVar.asInstanceOf[V8JavaScriptObject].getMember(member).asInstanceOf[V8JavaScriptObject]
        fn.call(null, proxied: _*) match {
          //v8o.executeJSFunction(member, proxied: _*) match {
          case x: V8Object if !x.isUndefined => node.get(x) match {
            case Some(jvmObj) =>
              jvmObj
            case _ => new V8JavaScriptObject(node, x)
          }
          case _: V8Object => UNDEFINED
          case o => o
        }
      }
    })
  }

  override def parseJson(jsonStr: String): JavaScriptObject = {
    Proxy.withMemoryManagement(node, {
      val json = node.getRuntime.get("JSON").asInstanceOf[V8Object]
      new ReleasedV8JavaScriptObject(node, json.executeJSFunction("parse", jsonStr).asInstanceOf[V8Object])
    })
  }

  override def setMember(name: String, value: AnyRef): Unit = {
    Proxy.withMemoryManagement(node, {
      Proxy.addIfNeccessary(node, name, value)
    })
  }

  override def eval(script: String): AnyRef = {
    Proxy.withMemoryManagement(node, {
      node.getRuntime.executeScript(script) match {
        case x: V8Object if !x.isUndefined => new ReleasedV8JavaScriptObject(node, x)
        case _: V8Object => UNDEFINED
        case o => o
      }
    })
  }

  override def finalize(): Unit = {
    super.finalize()
    node.getRuntime.release()
    node.node.release()
  }
}


