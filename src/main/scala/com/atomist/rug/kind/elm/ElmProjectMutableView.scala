package com.atomist.rug.kind.elm

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.support.ProjectDecoratingMutableView

/**
  * Decorated view of whole project
  */
class ElmProjectMutableView(pmv: ProjectMutableView)
  extends ProjectDecoratingMutableView(pmv) {

}
