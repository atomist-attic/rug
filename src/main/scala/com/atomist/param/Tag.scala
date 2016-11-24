package com.atomist.param

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

/**
  * Tag for a project delta.
  */
case class Tag @JsonCreator()(@JsonProperty("name") name: String,
                              @JsonProperty("description") description: String)
