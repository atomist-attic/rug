package com.atomist.rug.spi

import java.lang.reflect.Method

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
          m.getGenericReturnType.toString,
          m.getDeclaringClass,
          a.example() match {
            case "" => None
            case ex => Some(ex)
          })
      })

  private def extractExportedParametersAndDocumentation(m: Method): Array[TypeParameter] = {
    m.getParameters.map(p =>
      if (p.isAnnotationPresent(classOf[ExportFunctionParameterDescription])) {
        val annotation = p.getDeclaredAnnotation(classOf[ExportFunctionParameterDescription])
        TypeParameter(annotation.name(), p.getParameterizedType.toString, Some(annotation.description()))
      } else
        TypeParameter(p.getName, p.getParameterizedType.toString, None)
    )
  }

}
