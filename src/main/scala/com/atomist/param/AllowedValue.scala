package com.atomist.param

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

case class AllowedValue @JsonCreator()(@JsonProperty("name") name: String,
                                       @JsonProperty("display_name") displayName: String)

