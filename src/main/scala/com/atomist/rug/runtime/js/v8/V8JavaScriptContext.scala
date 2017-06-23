package com.atomist.rug.runtime.js.v8

import java.io.File
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.nio.file.{CopyOption, Files, Path, StandardCopyOption}

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{ArchiveRugResolver, AtomistConfig, DefaultAtomistConfig, Dependency}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js._
import com.atomist.rug.spi.ExportFunction
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.util.Timing.time
import com.eclipsesource.v8._
import com.eclipsesource.v8.utils.MemoryManager
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.ClassUtils
import org.springframework.util.ReflectionUtils

import scala.collection.mutable.ListBuffer


/**
  * v8 implementation. Currently a single v8
  */
class V8JavaScriptEngineContext(val rugAs: ArtifactSource,
                                val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends JavaScriptEngineContext
  with LazyLogging
  with JavaScriptUtils{

  private val node = NodeJS.createNodeJS()

  private val runtime = node.getRuntime
  private val scope = new MemoryManager(runtime)

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
    val scope = new MemoryManager(runtime)
    val more: Seq[JavaScriptMember] =  node.require(path.toFile) match {
      case o: V8Object => o.getKeys.map(k => JavaScriptMember(k, new V8JavaScriptObject(o.get(k))))
      case _ => Nil
    }
    exports ++= more
  }

  override def members(): Seq[JavaScriptMember] = {
    exports
  }

  override def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): AnyRef = {
    val scope = new MemoryManager(runtime)
    val v8o = jsVar.asInstanceOf[V8JavaScriptObject].getNativeObject.asInstanceOf[V8Object]
    params match {
      case Some(pvs) => pvs.parameterValues.foreach{ kv =>
        kv.getValue match {
          case o: String => v8o.add(kv.getName, o)
          case x => throw new RuntimeException(s"Don't know how to deal with $x")
        }

      }
      case _ =>
    }
    try{
      v8o.executeJSFunction(member, args.map(proxy):_*)
    }finally{
      scope.release()
    }
  }

  override def parseJson(json: String): JavaScriptObject = ???

  override def setMember(name: String, value: AnyRef): Unit = {
    runtime.add(name, proxy(value))
  }

  override def eval(script: String): AnyRef = {
    runtime.executeObjectScript(script) match {
      case x: V8Object if !x.isUndefined => new V8JavaScriptObject(x)
      case _: V8Object => UNDEFINED
      case o => o
    }
  }

  override def finalize(): Unit = {
    super.finalize()
    scope.release()
    runtime.release()
  }

  /**
    * Set up our module loader
    */
  def configureLoader(): Unit = {
    runtime.registerJavaMethod(new JavaCallback(){
      override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
        val root = parameters.getString(0)
        val id = parameters.getString(1)
        val rel = """\./(.*)""".r
        val path = root + id match {
          case rel(rhs) => rhs
          case x => x
        }
        println(s"Looking for file $path")
        rugAs.findFile(path).nonEmpty.asInstanceOf[AnyRef]
      }
    }, "fileExists")
    runtime.registerJavaMethod(new JavaCallback(){
      override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
        val path = parameters.getString(0)
        println(s"Looking for dir $path")
        rugAs.findChildDirectory(path).nonEmpty.asInstanceOf[AnyRef]
      }
    }, "dirExists")
    runtime.registerJavaMethod(new JavaCallback(){
      override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
        val root = parameters.getString(0)
        val id = parameters.getString(1)
        val rel = """\./(.*)""".r
        val path = root + id match {
          case rel(rhs) => rhs
          case x => x
        }
        println(s"Loading file $path")
        rugAs.findFile(path) match {
          case Some(f: FileArtifact) => f.content
            //runtime.executeScript(f.content)
          case _ =>
            println(s"Failed to load: $path")
           // TODO is this undefined?
            null
        }
      }
    }, "load")
    val require = IOUtils.toString(Thread.currentThread().getContextClassLoader.getResourceAsStream("tiny_require.js"), Charset.defaultCharset)
    runtime.executeVoidScript(s"$require;\n")
  }

  /**
    * Create a proxy around obj
    * @param obj
    * @return
    */
  private def proxy(obj: AnyRef): V8Object = {
    val v8pmv = new V8Object(runtime)
    obj.getClass.getMethods.foreach {
      case m: Method if exposeAsProperty(m) => v8pmv.add(m.getName, new PropertyProxy(runtime, obj, m))
      case m: Method => v8pmv.registerJavaMethod(new MethodProxy(obj,m), m.getName)
    }
    v8pmv
  }

  private def exposeAsProperty(m: Method): Boolean = {
    m.getDeclaredAnnotations.exists {
      case o: ExportFunction => o.exposeAsProperty()
      case _ => false
    }
  }
}

class MethodProxy(obj: AnyRef, method: Method) extends JavaCallback {

  override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
    val args = new scala.collection.mutable.ListBuffer[AnyRef]
    for(i <- 0 to parameters.length()){
      args.append(parameters.get(i))
    }
    method.invoke(obj, args:_*)//TODO proxy the response
  }
}

/**
  * Use V8
  */
object TestV8JavaScriptContext {
  def main(args: Array[String]) {
    val spring = FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("tmp")))
    val (result, elapsedTime) = time {
      val ctx = new V8JavaScriptEngineContext(spring)
      val resolver = new ArchiveRugResolver(Dependency(spring))
      resolver.resolvedDependencies.rugs
    }
    println(s"Loaded: ${result.allRugs.size} in $elapsedTime ms")
  }
}

