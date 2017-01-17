package com.atomist.param

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

case class AllowedValue @JsonCreator()(@JsonProperty("value") value: String,
                                       @JsonProperty("display_name") displayName: String)
