package com.atomist.rug.kind.service

import java.util.{List => JList}

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop.{UserServices, jsPathExpressionEngine}
import com.atomist.rug.spi.{ExportFunction, MutableView, ViewSupport}
import com.atomist.source.ArtifactSource

import scala.collection.JavaConverters._

/**
  * Tree node for services in a given context
  *
  * @param rugAs         current backing rug archive
  * @param serviceSource context
  * @param atomistConfig atomist configuration in effect
  */
class ServicesMutableView(rugAs: ArtifactSource,
                          val serviceSource: ServiceSource,
                          val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends ViewSupport[ServiceSource](serviceSource, null)
    with UserServices {

  private val pathExpressionEng = new jsPathExpressionEngine(serviceSource)

  @ExportFunction(readOnly = true, description = "Use to execute path expression")
  override def pathExpressionEngine: jsPathExpressionEngine =
    pathExpressionEng

  override def childNodeTypes: Set[String] = childNodeNames

  override def nodeName: String = "services"

  override def tags: Set[String] = Set("services")

  override val childNodeNames: Set[String] = Set("service")

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case "service" =>
      serviceSource.services.map(s =>
        new ServiceMutableView(this, rugAs, s, atomistConfig)
      )
    case _ => ???
  }

  @ExportFunction(readOnly = true, description = "Services")
  def services: JList[ServiceMutableView] =
    serviceSource.services.map(s =>
      new ServiceMutableView(this, rugAs, s, atomistConfig)
    ).asJava
}
