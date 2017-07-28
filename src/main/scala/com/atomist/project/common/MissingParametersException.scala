package com.atomist.project.common

import com.atomist.param.{Parameter, ParameterValue}
import com.atomist.source.ArtifactSourceException

class InvalidParametersException(msg: String)
  extends ArtifactSourceException(msg)

class MissingParametersException(msg: String, parameters: Seq[Parameter])
  extends InvalidParametersException(msg)

/**
  * One or more parameters is invalid.
  *
  * @param msg detailed message
  */
class IllformedParametersException(msg: String, parameters: Seq[ParameterValue])
  extends InvalidParametersException(msg)
