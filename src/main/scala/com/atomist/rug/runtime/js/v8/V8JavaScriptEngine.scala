package com.atomist.rug.runtime.js.v8

import java.nio.file.{Files, StandardCopyOption}

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime.js.interop.JavaScriptRuntimeException
import com.atomist.source.file.FileSystemArtifactSource
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.eclipsesource.v8._
import com.eclipsesource.v8.utils.MemoryManager
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer


/**
  * v8 implementation. Currently a single v8
  */
class V8JavaScriptEngine(val rugAs: ArtifactSource,
                         val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends JavaScriptEngine
  with LazyLogging
  with JavaScriptUtils{

  private val node = new NodeWrapper(NodeJS.createNodeJS())

  private val scope = new MemoryManager(node.getRuntime)

  val atomistContent: ArtifactSource = atomistConfig.atomistContent(rugAs)

  private val exports: ListBuffer[JavaScriptMember] = new ListBuffer[JavaScriptMember]()

  // Extract all the Atomist stuff

  private val root = rugAs match {
    case fs: FileSystemArtifactSource => fs.id.rootFile.toPath
    case mem =>
      val tempRoot = Files.createTempDirectory("rug")
      mem.allFiles.foreach{ memFile =>
        val fsFile = tempRoot.resolve(memFile.path)
        fsFile.getParent.toFile.mkdirs()
        val io = memFile.inputStream()
        try{
          Files.copy(io, fsFile, StandardCopyOption.REPLACE_EXISTING)
        }finally{
          io.close()
        }
      }
      tempRoot
  }

  //eval all the atomist stuff
  atomistContent
    .filter(_ => true, atomistConfig.isJsSource)
    .allFiles.foreach(evaluate)


  override def evaluate(f: FileArtifact): Unit = {
    val path = root.resolve(f.path)
    val more: Seq[JavaScriptMember] =  node.node.require(path.toFile) match {
      case o: V8Object =>
        val mapped: Map[String, AnyRef] = o.getKeys.map(k => (k, o.get(k))).toMap
        val objects = mapped.filter(p => p._2.isInstanceOf[V8Object])
        objects.map(p => JavaScriptMember(p._1, new V8JavaScriptObject(node, p._2.asInstanceOf[V8Object]))).toSeq
      case _ => Nil
    }
    exports ++= more
  }

  override def members(): Seq[JavaScriptMember] = {
    exports
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

   // val scope = new MemoryManager(node.getRuntime)
    try{
      withEnhancedExceptions{
        if (params.nonEmpty) {
          setParameters(jsVar, params.get.parameterValues)
        }
        val v8o = jsVar.asInstanceOf[V8JavaScriptObject].getNativeObject.asInstanceOf[V8Object]

        val proxied = args.map(a => Proxy.ifNeccessary(node, a))
        v8o.executeJSFunction(member, proxied:_*) match {
          case x: V8Object if !x.isUndefined => node.get(x) match {
            case Some(jvmObj) => jvmObj
            case _ => new V8JavaScriptObject(node, x)
          }
          case _: V8Object => UNDEFINED
          case o => o
        }
      }
    }finally{
      //scope.release()
    }
  }

  override def parseJson(jsonStr: String): JavaScriptObject = {
    val json = node.getRuntime.get("JSON").asInstanceOf[V8Object]
    new V8JavaScriptObject(node, json.executeJSFunction("parse", jsonStr).asInstanceOf[V8Object])
  }

  override def setMember(name: String, value: AnyRef): Unit = {
    Proxy.addIfNeccessary(node, name, value)
  }

  override def eval(script: String): AnyRef = {
    node.getRuntime.executeScript(script) match {
      case x: V8Object if !x.isUndefined => new V8JavaScriptObject(node, x)
      case _: V8Object => UNDEFINED
      case o => o
    }
  }

  override def finalize(): Unit = {
    super.finalize()
//    scope.release()
//    node.getRuntime.release()
//    node.node.release()
  }
}


