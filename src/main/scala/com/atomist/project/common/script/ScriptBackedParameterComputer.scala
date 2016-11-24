package com.atomist.project.common.script

import com.atomist.project.common.support.ParameterComputer
import com.atomist.util.script.ScriptBacked

trait ScriptBackedParameterComputer
  extends ParameterComputer
    with ScriptBacked
