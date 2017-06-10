package com.atomist.rug.runtime.js.nashorn

import java.lang.reflect.{Field, Method}
import java.util.regex.Pattern
import javax.script._

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugJavaScriptException
import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime.js.interop.jsScalaHidingProxy
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, FileArtifact}
import com.coveo.nashorn_modules.{AbstractFolder, Folder, Require}
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{AbstractJSObject, NashornScriptEngine, NashornScriptEngineFactory, ScriptObjectMirror}
import jdk.nashorn.internal.runtime.{ECMAException, ScriptRuntime}
import org.springframework.util.ReflectionUtils

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object NashornContext {

  /**
    * Function that can initialize a ScriptEngine before use.
    * Typically evaluates JavaScript strings or binds objects.
    */
  type EngineInitializer = ScriptEngine => Unit

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

  def redirectConsoleToSysOut(engine: ScriptEngine): Unit = {
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
class NashornContext(val rugAs: ArtifactSource,
                     val atomistConfig: AtomistConfig = DefaultAtomistConfig,
                     bindings: Bindings = new SimpleBindings(),
                     initializer: NashornContext.EngineInitializer = NashornContext.redirectConsoleToSysOut)
  extends LazyLogging
  with JavaScriptEngineContext
  with JavaScriptUtils{

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
  def members(): Seq[JavaScriptMember] = {
    engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE)
      .get("exports").asInstanceOf[ScriptObjectMirror]
      .asScala.collect {
      case (_, v: ScriptObjectMirror) => extractVars(v)
    }.flatten.toSeq
  }

  private def extractVars(obj: ScriptObjectMirror): Seq[JavaScriptMember] = {
    obj.entrySet().asScala.flatMap(e => {
      (e.getKey, e.getValue) match {
        case (k, som: ScriptObjectMirror) => Some(JavaScriptMember(k, new NashornJavaScriptObject(som)))
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

  override def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): Any = {
    withEnhancedExceptions {
      val clone = cloneVar(jsVar.asInstanceOf[NashornJavaScriptObject].som)
      if (params.nonEmpty) {
        setParameters(clone, params.get.parameterValues)
      }
      clone.callMember(member, args: _*)
    }
  }
  /**
    * Separate for test
    */
  private[js] def cloneVar(objToClone: ScriptObjectMirror): NashornJavaScriptObject = {
    val bindings = new SimpleBindings()
    bindings.put("rug", objToClone)

    //TODO - why do we need this?
    engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).asScala.foreach {
      case (k: String, v: AnyRef) => bindings.put(k, v)
    }
    new NashornJavaScriptObject(engine.eval("Object.create(rug);", bindings).asInstanceOf[ScriptObjectMirror])
  }

  override def parseJson(body: String): JavaScriptObject = {
    val bindings = new SimpleBindings()
    bindings.put("__coercion", body)

    //TODO - why do we need this?
    engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).asScala.foreach {
      case (k: String, v: AnyRef) => bindings.put(k, v)
    }
    new NashornJavaScriptObject(engine.eval(s"""JSON.parse(__coercion);""", bindings).asInstanceOf[ScriptObjectMirror])
  }

  override def setMember(name: String, value: AnyRef): Unit = {
    value match {
      case o: NashornJavaScriptObject => engine.put(name, o.som)
      case _ => engine.put(name, jsScalaHidingProxy(value))
    }
  }
}
/**
  *
  * @param delegate
  */
//class ObjectWrapperX(delegate: AnyRef) extends AbstractJSObject {
//
//
//  if(delegate.isInstanceOf[ObjectWrapperX]){
//    throw new RuntimeException("Don't wrap wrappers")
//  }
//
//  if(delegate.isInstanceOf[NashornJavaScriptObject]){
//    throw new RuntimeException("These should _not_ be wrapped!")
//  }
//
//  if(delegate.isInstanceOf[ScriptObjectMirror]){
//    throw new RuntimeException("These should _not_ be wrapped!")
//  }
//
//  override def getMember(name: String): AnyRef = {
//
//    if("valueOf" == name){
//      super.getMember(name)
//    }else{
//      ReflectionUtils.getAllDeclaredMethods(delegate.getClass).find(_.getName == name) match {
//        case Some(m) => new TempWrappingFunction(m, delegate)
//        case _ =>
//          ReflectionUtils.findField(delegate.getClass, name) match {
//            case o: Field => new ObjectWrapper(o.get(delegate))
//            case _ => ScriptRuntime.UNDEFINED
//          }
//      }
//    }
//  }
//}
//
///**
//  * Ensure we use an ObjectWrapper for normal java objects
//  * @param m
//  * @param delegate
//  */
//class TempWrappingFunction(m: Method, delegate: AnyRef) extends AbstractJSObject {
//  override def isFunction: Boolean = true
//  override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
//    try {
//      val fixed = args.collect {
//        case o: ScriptObjectMirror => new NashornJavaScriptObject(o)
//        case x => x
//      }
//      m.invoke(delegate, fixed: _*) match {
//        case o if !o.isInstanceOf[String] && !o.isInstanceOf[JavaScriptObject] &&  !o.isInstanceOf[ScriptObjectMirror] => new ObjectWrapper(o)
//        //case x: ScriptObjectMirror => new NashornJavaScriptObject(x)
//        case y: AnyRef => y
//      }
//
//    }
//    catch {
//      case iex: IllegalArgumentException =>
//        throw new IllegalArgumentException(s"Illegal ${args.size} arguments for ${delegate.getClass}.${m.getName}: [$args]", iex)
//      case o: Throwable =>{
//        o.printStackTrace()
//        throw o
//      }
//    }
//  }
//}

