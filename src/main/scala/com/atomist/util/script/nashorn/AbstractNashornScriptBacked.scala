package com.atomist.util.script.nashorn

import java.util.{List => JList}
import javax.script.{Invocable, ScriptEngineManager, ScriptException}

import com.atomist.project.common.ReportingUtils
import com.atomist.util.script.{InvalidScriptException, Script, ScriptBacked}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Convenient superclass for objects based by scripts.
  */
abstract class AbstractNashornScriptBacked(javaScripts: Seq[Script])
  extends ScriptBacked {

  private val engine = new ScriptEngineManager(null).getEngineByName("nashorn")

  protected val js = globalTypeDeclarations + combineScripts(javaScripts.asJava)

  protected def globalTypeDeclarations: String

  private def combineScripts(scripts: JList[Script]) =
    scripts.asScala
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


