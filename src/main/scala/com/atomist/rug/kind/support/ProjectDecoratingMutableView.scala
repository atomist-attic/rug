package com.atomist.rug.kind.support

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.ExportFunction
import com.atomist.source.ArtifactSource

/**
  * Extended by classes that can decorate a ProjectMutableView, adding
  * more export methods, but preserving correct update behavior.
  * These an even be nested, e.g. ProjectMutableView <- SpringProjectMutableView <- SpringBootProjectMutableView
  */
abstract class ProjectDecoratingMutableView(pmv: ProjectMutableView)
  extends ProjectMutableView(pmv.rugAs, pmv.currentBackingObject) {
  /**
    * Take care to update the wrapped object and make it update its own parent.
    * This is a common pattern with decorator style functionality. Otherwise
    * the context knows nothing about our new decorator classes.
    */
  override def commit(): Unit = {
    pmv.updateTo(this.currentBackingObject)
    pmv.commit()
  }

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = pmv.value

  /**
    * Subclasses can call this to update the state of this object
    */
  override def updateTo(newBackingObject: ArtifactSource): Unit = {
    super.updateTo(newBackingObject)
    pmv.updateTo(newBackingObject)
  }
}
