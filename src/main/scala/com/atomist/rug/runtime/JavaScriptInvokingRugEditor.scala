package com.atomist.rug.runtime

import com.atomist.model.content.text.{PathExpressionEngine, TreeNode}
import com.atomist.param.ParameterValue
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit._
import com.atomist.project.edit.common.ProjectEditorSupport
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._


case class Match(root: TreeNode, matches: _root_.java.util.List[TreeNode]) {
}

/**
  * JavaScript-friendly facade to PathExpressionEngine
  */
class PathExpressionExposer {

  val pee = new PathExpressionEngine

  def evaluate(tn: TreeNode, pe: Object): Match = {
    println("Foo bar")
    pe match {
      case som: ScriptObjectMirror =>
        val expr: String = som.get("expression").asInstanceOf[String]
        pee.evaluate(tn, expr) match {
          case Right(nodes) =>
            val m = Match(tn, nodes.asJava)
            m
        }
    }
  }
}

trait Registry {

  def registry: Map[String, Object]

}

object DefaultRegistry extends Registry {

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}



/**
  * ProjectEditor implementation that invokes a JavaScript function. This will probably be the result of
  * TypeScript compilation, but need not be. Attempts to source metadata from annotations.
  */
class JavaScriptInvokingRugEditor(
                                   jsc: JavaScriptContext,
                                   className: String,
                                   jsVar: ScriptObjectMirror,
                                   rugAs: ArtifactSource
                                 )
  extends JavaScriptInvokingRugOperation(jsc, className, jsVar, rugAs)
    with ProjectEditorSupport {

  override def impacts: Set[Impact] = Impacts.UnknownImpacts

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ProjectOperationArguments): ModificationAttempt = {
    val tr = time {
      val pmv = new ProjectMutableView(rugAs, targetProject, atomistConfig = DefaultAtomistConfig)

      val params = new Parameters(poa)

      //  println(editMethod.entrySet())

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        val res = jsc.engine.get(className.toLowerCase).asInstanceOf[ScriptObjectMirror].callMember("edit", pmv, params)

        if (pmv.currentBackingObject == targetProject) {
          NoModificationNeeded("OK")
        }
        else {
          SuccessfulModification(pmv.currentBackingObject, impacts, "OK")
        }
      }
      catch {
        case f: InstantEditorFailureException =>
          FailedModificationAttempt(f.getMessage)
      }

    }
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

}


/**
  * Dynamic properties holder that represents the JVM counterpart of a TypeScript class that
  * doesn't exist in Java.
  */
private class Parameters(poa: ProjectOperationArguments) extends AbstractJSObject {

  override def getMember(name: String): AnyRef = {
    val resolved: ParameterValue = poa.parameterValueMap.getOrElse(
      name,
      throw new RugRuntimeException(null, s"Cannot resolve parameter [$name]"))
    //println(s"Call to getMember with [$name]")

    // The below is what you use for a function
    //    new AbstractJSObject() {
    //
    //      override def isFunction: Boolean = true
    //
    //      override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
    //        resolved
    //      }
    //    }

    // This works for a method
    resolved.getValue
    // TODO fall back to the value in the field?
  }
}

