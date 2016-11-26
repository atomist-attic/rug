package com.atomist.rug.kind.service

import java.util.{List => JList}

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.spi.{ExportFunction, MutableView, ViewSupport}
import com.atomist.source.ArtifactSource

import scala.collection.JavaConverters._

class ServicesMutableView(rugAs: ArtifactSource,
                          val serviceSource: ServiceSource,
                          val atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends ViewSupport[ServiceSource](serviceSource, null) {

  override def childNodeTypes: Set[String] = childNodeNames

  override def nodeName: String = "services"

  override def nodeType: String = "services"

  /**
    * Values that can be passed to children method.
    */
  override val childrenNames: Seq[String] = Seq("service")

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
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
