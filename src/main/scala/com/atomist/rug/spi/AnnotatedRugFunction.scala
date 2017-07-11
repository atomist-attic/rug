package com.atomist.rug.spi

import java.lang.reflect.{InvocationTargetException, Method}

import com.atomist.param.{Parameter, ParameterValues, Tag}
import org.springframework.core.annotation.AnnotationUtils

/**
  * Use this for a terser way to define RugFunctions in Scala.
  */
trait AnnotatedRugFunction extends RugFunction {

  override def secrets: Seq[Secret] = {
    functionMethod().getParameters.flatMap(p => {
      AnnotationUtils.findAnnotation(p, classOf[com.atomist.rug.spi.annotation.Secret]) match {
        case s: com.atomist.rug.spi.annotation.Secret => Some(Secret(s.name(), s.path()))
        case _ => None
      }
    }).toSeq
  }

  override def name: String = functionAnnotation().name()

  override def description: String = functionAnnotation().description()

  override def tags: Seq[Tag] = functionAnnotation().tags().map(t => Tag(t.name(), t.name())).toSeq

  private def functionAnnotation(): com.atomist.rug.spi.annotation.RugFunction =
    functionMethod().getAnnotation(classOf[com.atomist.rug.spi.annotation.RugFunction])

  private def functionMethod(): Method = {
    getClass.getMethods.find(m => m.getAnnotation(classOf[com.atomist.rug.spi.annotation.RugFunction]) != null) match {
      case Some(m) => m
      case _ => throw new IllegalArgumentException(s"${getClass.getName} has no method annotated with @RugFunction")
    }
  }

  override def parameters: Seq[Parameter] = functionMethod().getParameters.flatMap(p => {
    AnnotationUtils.findAnnotation(p, classOf[com.atomist.rug.spi.annotation.Parameter]) match {
      case pa: com.atomist.rug.spi.annotation.Parameter =>
        Some(Parameter(pa.name(), pa.pattern(), pa.defaultValue()))
      case _ => None
    }
  }).toSeq

  override def run(parameters: ParameterValues): FunctionResponse = {
    val method = functionMethod()
    val args = method.getParameters.map(p => {
      (p.getAnnotation(classOf[com.atomist.rug.spi.annotation.Parameter]),
      p.getAnnotation(classOf[com.atomist.rug.spi.annotation.Secret])) match {
        case (null, secretAnnotation) =>
          convert(p, parameters.paramValue(secretAnnotation.name()))
        case (parameterAnnotation, null) if parameterAnnotation.required() =>
          convert(p, parameters.paramValue(parameterAnnotation.name()))
        case (parameterAnnotation, null) =>
          parameterAnnotation.defaultValue() match {
            case defaultValue: String => convert(p, defaultValue)
            case _ => convert(p, null)
          }

        case _ =>
          throw new IllegalArgumentException(s"Parameter ${p.getName} not annotated with either @Secret or @Parameter")
      }
    })
    try {
      method.invoke(this, args: _*).asInstanceOf[FunctionResponse]
    }
    catch {
      case ite: InvocationTargetException => throw ite.getCause
    }
  }

  /**
    * Convert the parameter value based on the type of the parameter.
    */
  private def convert(param: java.lang.reflect.Parameter, avalue: Any): AnyRef = {
    avalue match {
      case o: String => (param.getType, o) match {
        case (p, "") if p == classOf[Int] => new Integer(0)
        case (p, _)  if p == classOf[Int] => o.toInt.asInstanceOf[AnyRef]
        case (p, "") if p == classOf[Integer] => new Integer(0)
        case (p, _)  if p == classOf[Integer] => Integer.parseInt(o).asInstanceOf[AnyRef]
        case (p, "") if p == classOf[Boolean] => false.asInstanceOf[AnyRef]
        case (p , _) if p == classOf[Boolean] => o.toBoolean.asInstanceOf[AnyRef]
        case (p, "") if p == classOf[Double] => 0d.asInstanceOf[AnyRef]
        case (p, _) if p == classOf[Double] => o.toDouble.asInstanceOf[AnyRef]
        case (p, "") if p == classOf[java.lang.Double] => 0d.asInstanceOf[AnyRef]
        case (p, _) if p == classOf[java.lang.Double] => java.lang.Double.parseDouble(o).asInstanceOf[AnyRef]
        case (p, "") if p == classOf[Float] => 0f.asInstanceOf[AnyRef]
        case (p, _) if p == classOf[Float] => o.toFloat.asInstanceOf[AnyRef]
        case (p, "") if p == classOf[java.lang.Float] => 0f.asInstanceOf[AnyRef]
        case (p, _) if p == classOf[java.lang.Float] => java.lang.Float.parseFloat(o).asInstanceOf[AnyRef]
        case (p, _) if p == classOf[String] => o
      }
      case _ => avalue.asInstanceOf[AnyRef]
    }
  }
}
