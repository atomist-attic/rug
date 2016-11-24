package com.atomist.project.common

import com.atomist.source.ArtifactSourceCreationException

class InvalidParametersException(msg: String)
  extends ArtifactSourceCreationException(msg)

class MissingParametersException(msg: String)
  extends InvalidParametersException(msg)

/**
  * One or more parameters is invalid.
  *
  * @param msg detailed message
  */
class IllformedParametersException(msg: String)
  extends InvalidParametersException(msg)
