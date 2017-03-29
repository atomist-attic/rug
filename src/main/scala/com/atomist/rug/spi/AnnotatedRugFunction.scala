package com.atomist.rug.spi

import java.lang.reflect.Method

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
        val param = Parameter(pa.name(), pa.pattern())
        val defaultValue = pa.defaultValue()
        if (defaultValue != null && defaultValue != "")
          param.setDefaultValue(defaultValue)

        Some(Parameter(pa.name(), pa.pattern()))
      case _ => None
    }
  }).toSeq

  override def run(parameters: ParameterValues): FunctionResponse = {
    val method = functionMethod()
    val args = method.getParameters.map(p => {
      val parameterAnnotation = p.getAnnotation(classOf[com.atomist.rug.spi.annotation.Parameter])
      val secretAnnotation = p.getAnnotation(classOf[com.atomist.rug.spi.annotation.Secret])
      if (parameterAnnotation == null && secretAnnotation != null) {
        convert(p, parameters.paramValue(secretAnnotation.name()))
      } else if (parameterAnnotation != null && secretAnnotation == null && parameterAnnotation.required()) {
        convert(p, parameters.paramValue(parameterAnnotation.name()))
      } else if (parameterAnnotation == null && secretAnnotation == null) {
        throw new IllegalArgumentException(s"Parameter ${p.getName} not annotated with either @Secret or @Parameter")
      } else {
        null
      }
    })
    method.invoke(this, args: _*).asInstanceOf[FunctionResponse]
  }

  /**
    * Convert the parameter value based on the type of the parameter.
    */
  private def convert(param: java.lang.reflect.Parameter, avalue: Any): Object = {
    avalue match {
      case o: String => param.getType match {
        case p if p == classOf[Int] => o.toInt.asInstanceOf[Object]
        case p if p == classOf[Integer] => Integer.parseInt(o).asInstanceOf[Object]
        case p if p == classOf[Boolean] => o.toBoolean.asInstanceOf[Object]
        case p if p == classOf[java.lang.Boolean] => java.lang.Boolean.parseBoolean(o).asInstanceOf[Object]
        case p if p == classOf[Double] => o.toDouble.asInstanceOf[Object]
        case p if p == classOf[java.lang.Double] => java.lang.Double.parseDouble(o).asInstanceOf[Object]
        case p if p == classOf[Float] => o.toFloat.asInstanceOf[Object]
        case p if p == classOf[java.lang.Float] => java.lang.Float.parseFloat(o).asInstanceOf[Object]
        case p if p == classOf[String] => o.asInstanceOf[Object]
      }
      case _ => avalue.asInstanceOf[Object]
    }
  }
}
