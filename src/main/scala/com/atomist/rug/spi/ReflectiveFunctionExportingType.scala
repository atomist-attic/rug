package com.atomist.rug.spi

import java.lang.reflect.{InvocationTargetException, Method}

import com.atomist.rug._
import com.atomist.rug.runtime.rugdsl.Evaluator.FunctionTarget
import com.atomist.rug.runtime.rugdsl.{FunctionInvocationContext, RugFunction}
import org.springframework.util.ReflectionUtils

/**
  * Export @[[ExportFunction]] methods.
  */
object ReflectiveFunctionExport {

  /**
    * Export functions.
    */
  def exportedFunctions(c: Class[_]): Traversable[RugFunction[_, _]] = {
    val functions: Traversable[RugFunction[_, _]] =
      ReflectionUtils.getAllDeclaredMethods(c)
        .filter(_.getAnnotations.exists(_.isInstanceOf[ExportFunction]))
        .map(new ReflectiveRugFunction[FunctionTarget, Any](_))
    functions
  }

  def exportedOperations(c: Class[_]): Seq[TypeOperation] = {
    ReflectionUtils.getAllDeclaredMethods(c)
      .filter(_.getAnnotations.exists(_.isInstanceOf[ExportFunction]))
      .map(m => {
        val a = m.getAnnotation(classOf[ExportFunction])
        val params = extractExportedParametersAndDocumentation(m)
        TypeOperation(m.getName, a.description(),
          a.readOnly(),
          params,
          m.getGenericReturnType.toString,
          m.getDeclaringClass,
          a.example() match {
            case "" => None
            case ex => Some(ex)
          })
      })
  }

  private def extractExportedParametersAndDocumentation(m: Method): Array[TypeParameter] = {
    val params = m.getParameters.map(p =>
      p.isAnnotationPresent(classOf[ExportFunctionParameterDescription]) match {
        case true => TypeParameter(p.getDeclaredAnnotation(classOf[ExportFunctionParameterDescription]).name(),
          p.getParameterizedType.toString,
          Some(p.getDeclaredAnnotation(classOf[ExportFunctionParameterDescription]).description()))
        case _ => TypeParameter(p.getName, p.getParameterizedType.toString, None)
      }
    )
    params
  }

  final def exportedRegistry(c: Class[_]): RugFunctionRegistry = {
    new FixedRugFunctionRegistry(exportedFunctions(c))
  }
}

private class ReflectiveRugFunction[T <: FunctionTarget, R](m: Method)
  extends RugFunction[T, R] {

  override def name: String = m.getName

  private val annotation: ExportFunction = m.getAnnotation(classOf[ExportFunction])

  require(annotation != null, s"Internal error. Reflective function $m must have an @ExportFunction annotation")

  override val description = Some(annotation.description())

  override val readOnly: Boolean = annotation.readOnly()

  override def invoke(ic: FunctionInvocationContext[T]): R = {
    // Pass invocation context as additional argument if it's called for
    val argsToUse = ic.localArgs ++ {
      if (m.getParameterCount > ic.localArgs.size)
        Seq(ic)
      else Nil
    }

    try {
      val result =
        if (ic.functionInvocation.pathBelow.isEmpty) {
          // Simple invocation of the target method
          val m2 = findMethod(ic.target, m.getName, ic.localArgs.length)
          if (m2 == null) {
            val m3 = findMethod(ic.target, m.getName)
            m3.invoke(ic.target, argsToUse: _*)
          } else {
            m2.invoke(ic.target, ic.localArgs: _*)
          }
        } else {
          var target = m.invoke(ic.target)
          for (pe <- ic.functionInvocation.pathBelow.dropRight(1)) {
            val m2 = findMethod(target, pe)
            target = m2.invoke(target)
          }
          val m2 = findMethod(target, ic.functionInvocation.pathBelow.last)
          m2.invoke(target, argsToUse: _*)
        }
      result.asInstanceOf[R]
    } catch {
      case iae: IllegalArgumentException =>
        throw new RugRuntimeException(name,
          s"Encountered '${iae.getMessage}' trying to invoke $m with [${argsToUse.mkString(",")}] on ${ic.target}", iae)
      case ite: InvocationTargetException =>
        ite.getTargetException match {
          case f: InstantEditorFailureException =>
            throw f
          case _ =>
            throw new RugRuntimeException(name,
              s"Encountered '${ite.getTargetException.getMessage}' invoking $m with [${ic.localArgs.mkString(",")}] on ${ic.target}",
              ite.getTargetException)
        }
    }
  }

  private def findMethod(target: Object, name: String): Method = {
    try {
      ReflectionUtils.getAllDeclaredMethods(target.getClass)
        .find(_.getName.equals(name))
        .getOrElse(throw new RugRuntimeException(null, s"Unknown method '$name' on ${target.getClass}"))
    } catch {
      case e: NoSuchMethodException =>
        throw new RugRuntimeException(null, s"Unknown method '$name' on ${target.getClass}", e)
    }
  }

  private def findMethod(target: Object, name: String, num_params: Int): Method = {
    try {
      ReflectionUtils.getAllDeclaredMethods(target.getClass).find(m =>
        m.getName.equals(name) && m.getParameterCount == num_params
      ).orNull
    } catch {
      case e: NoSuchMethodException =>
        throw new RugRuntimeException(null, s"Unknown method '$name' on ${target.getClass}", e)
    }
  }

  override def toString: String = name
}
