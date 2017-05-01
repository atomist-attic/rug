package com.atomist.rug.spi

import java.lang.reflect.{AnnotatedElement, Method}

import org.springframework.util.ReflectionUtils

/**
  * Export @[[ExportFunction]] methods.
  */
object ReflectiveFunctionExport {

  def allExportedOperations(c: Class[_]): Seq[TypeOperation] =
    if (c == null) Nil
    else operations(ReflectionUtils.getAllDeclaredMethods(c))

  def exportedOperations(c: Class[_]): Seq[TypeOperation] =
    if (c == null) Nil
    else operations(c.getDeclaredMethods)

  private def operations(methods: Array[Method]) =
    methods
      .filter(_.getAnnotations.exists(_.isInstanceOf[ExportFunction]))
      .map(m => {
        val a = m.getAnnotation(classOf[ExportFunction])
        val params = extractExportedParametersAndDocumentation(m)
        TypeOperation(m.getName, a.description(),
          a.readOnly(),
          params,
          SimpleParameterOrReturnType.fromJavaType(m.getGenericReturnType),
          m.getDeclaringClass,
          a.example() match {
            case "" => None
            case ex => Some(ex)
          },
          exposeAsProperty = a.exposeAsProperty(),
          exposeResultDirectlyToNashorn = a.exposeResultDirectlyToNashorn())
      })

  private def extractExportedParametersAndDocumentation(m: Method): Array[TypeParameter] = {
    m.getParameters.map(p =>
      if (p.isAnnotationPresent(classOf[ExportFunctionParameterDescription])) {
        val annotation = p.getDeclaredAnnotation(classOf[ExportFunctionParameterDescription])
        TypeParameter(annotation.name(),
          SimpleParameterOrReturnType.fromJavaType(p.getParameterizedType),
          Some(annotation.description()))
      }
      else
        TypeParameter(p.getName,
          SimpleParameterOrReturnType.fromJavaType(p.getParameterizedType),
          None)
    )
  }

}
