package com.atomist.rug.runtime.js.v8

import java.nio.file.{Files, StandardCopyOption}

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js._
import com.atomist.source.file.FileSystemArtifactSource
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.eclipsesource.v8._
import com.eclipsesource.v8.utils.MemoryManager
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer


/**
  * v8 implementation. Currently a single v8
  */
class V8JavaScriptEngineContext(val rugAs: ArtifactSource,
                                val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends JavaScriptEngineContext
  with LazyLogging
  with JavaScriptUtils{

  private val node = new NodeWrapper(NodeJS.createNodeJS())

  private val scope = new MemoryManager(node.getRuntime)

  val atomistContent: ArtifactSource = atomistConfig.atomistContent(rugAs)

  private val exports: ListBuffer[JavaScriptMember] = new ListBuffer[JavaScriptMember]()

  // Require all the Atomist stuff

  private val root = rugAs match {
    case fs: FileSystemArtifactSource => fs.id.rootFile.toPath
    case mem =>
      val tempRoot = Files.createTempDirectory("rug")
      mem.allFiles.foreach{ memFile =>
        val fsFile = tempRoot.resolve(memFile.path)
        fsFile.getParent.toFile.mkdirs()
        Files.copy(memFile.inputStream(), fsFile, StandardCopyOption.REPLACE_EXISTING)
      }
      tempRoot
  }

  atomistContent
    .filter(_ => true, atomistConfig.isJsSource)
    .allFiles.foreach(evaluate)


  override def evaluate(f: FileArtifact): Unit = {

    val path = root.resolve(f.path)
    val scope = new MemoryManager(node.getRuntime)
    val more: Seq[JavaScriptMember] =  node.node.require(path.toFile) match {
      case o: V8Object => o.getKeys.map(k => JavaScriptMember(k, new V8JavaScriptObject(node, o.get(k).asInstanceOf[V8Object])))
      case _ => Nil
    }
    exports ++= more
  }

  override def members(): Seq[JavaScriptMember] = {
    exports
  }

  override def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): AnyRef = {
//    val scope = new MemoryManager(node.getRuntime)

    if (params.nonEmpty) {
      setParameters(jsVar, params.get.parameterValues)
    }
    val v8o = jsVar.asInstanceOf[V8JavaScriptObject].getNativeObject.asInstanceOf[V8Object]

    try{
      v8o.executeJSFunction(member, args.map(a => Proxy.ifNeccessary(node, a)):_*) match {
        case x: V8Object if !x.isUndefined => node.get(x) match {
          case Some(jvmObj) => jvmObj
          case _ => new V8JavaScriptObject(node, x)
        }
        case _: V8Object => UNDEFINED
        case o => o
      }
    }finally{
//      scope.release()
    }
  }

  override def parseJson(json: String): JavaScriptObject = ???

  override def setMember(name: String, value: AnyRef): Unit = {
    Proxy.addIfNeccessary(node, name, value)
  }

  override def eval(script: String): AnyRef = {
    node.getRuntime.executeObjectScript(script) match {
      case x: V8Object if !x.isUndefined => new V8JavaScriptObject(node, x)
      case _: V8Object => UNDEFINED
      case o => o
    }
  }

  override def finalize(): Unit = {
    super.finalize()
    scope.release()
    node.getRuntime.release()
    node.node.release()
  }
}



/**
  * Use V8
  */
object TestV8JavaScriptContext {
  def main(args: Array[String]) {
    val node = NodeJS.createNodeJS()
    val runtime = node.getRuntime
    val o = new V8Object(runtime)
    o.add("cool", "beans")
    runtime.executeScript("var f = function(arg){return arg;};")
    val res = runtime.executeJSFunction("f",o)
    println("blah")
//    val spring = FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("tmp")))
//    val (result, elapsedTime) = time {
//      val ctx = new V8JavaScriptEngineContext(spring)
//      val resolver = new ArchiveRugResolver(Dependency(spring))
//      resolver.resolvedDependencies.rugs
//    }
//    println(s"Loaded: ${result.allRugs.size} in $elapsedTime ms")
  }
}

