package com.atomist.param

import scala.beans.BeanProperty

case class SimpleParameterValue(@BeanProperty name: String,
                                @BeanProperty value: AnyRef)
  extends ParameterValue
