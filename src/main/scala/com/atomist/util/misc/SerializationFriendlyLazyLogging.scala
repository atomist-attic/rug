package com.atomist.util.misc

import java.beans.Transient

import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Use as an alternative to TypeSafe LazyLogging to avoid
  * blowing up JSON and other serialization.
  */
trait SerializationFriendlyLazyLogging {

  @Transient
  @JsonIgnore
  private lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

}
