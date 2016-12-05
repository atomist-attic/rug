package com.atomist.util.script.nashorn

import java.util.{List => JList}
import javax.script.{Invocable, ScriptEngineManager, ScriptException}

import com.atomist.project.common.ReportingUtils
import com.atomist.util.script.{InvalidScriptException, Script, ScriptBacked}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

/**
  * Convenient superclass for objects based by scripts.
  */
abstract class AbstractNashornScriptBacked(javaScripts: Seq[Script])
  extends ScriptBacked {

  private val engine = new ScriptEngineManager(null).getEngineByName("nashorn")

  protected val js = globalTypeDeclarations + combineScripts(javaScripts)

  protected def globalTypeDeclarations: String

  private def combineScripts(scripts: JList[Script]) =
    scripts
      .map(script => s"// ${script.name}\n//----------------------\n${script.content}")
      .mkString("\n\n")

  private val startupException: Option[InvalidScriptException] =
    try {
      val r = engine.eval(js)
      None
    } catch {
      case sex: ScriptException =>
        val bad = s"Error processing script:\n${ReportingUtils.withLineNumbers(js)}: ${sex.getMessage}"
        Some(new InvalidScriptException(bad, sex))
    }

  /**
    * Subclasses can call this to get an invocable script
    */
  @throws[InvalidScriptException]
  protected def invocable: Invocable =
  if (startupException.isDefined) throw startupException.get
  else engine.asInstanceOf[Invocable]

  override def validate(): Unit = {
    startupException.map(sex => throw sex)
  }
}

/**
  * Enables convenient validation of scripts.
  */
abstract class NashornScriptValidator(
                                       val globalTypeDeclarations: String,
                                       javaScripts: JList[Script])
  extends AbstractNashornScriptBacked(javaScripts) {

  /**
    * Try something with the Invocable (letting any exception pass through)
    * to validate the script
    *
    * @param invocable
    * @throws Exception javax.script exception or any other exception, such as NoSuchMethodException,
    * which will be translated
    */
  @throws[Exception]
  protected def testInvocable(invocable: Invocable): Unit

  @throws[InvalidScriptException]
  override def validate(): Unit = {
    super.validate()
    try {
      testInvocable(invocable)
    } catch {
      case sex: ScriptException =>
        val bad = s"Error processing script:\n${ReportingUtils.withLineNumbers(js)}: ${sex.getMessage}"
        throw new InvalidScriptException(bad, sex)
      case nsme: NoSuchMethodException =>
        val bad = s"Missing function in script:\n${ReportingUtils.withLineNumbers(js)}: ${nsme.getMessage}"
        throw new InvalidScriptException(bad, nsme)
      case wtf: Exception =>
        val bad = s"Unknown error in script:\n${ReportingUtils.withLineNumbers(js)}: ${wtf.getMessage}"
        throw new InvalidScriptException(bad, wtf)
    }
  }

  protected def requireFunction(name: String) = invocable.invokeFunction(name)

  protected def verifyFunction(name: String, args: AnyRef*) =
    Try {
      invocable.invokeFunction(name, args)
    } match {
      case s: Success[Object] => true
      case f: Failure[Object] => false
    }
}
