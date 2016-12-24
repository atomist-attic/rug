package com.atomist.tree.content.project

import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}

trait NameValuePair {

  def name: String

  def value: String

  def description: String

  override def equals(that: Any): Boolean = EqualsBuilder.reflectionEquals(this, that)

  override def hashCode(): Int = HashCodeBuilder.reflectionHashCode(this)
}

case class SimpleNameValuePair(name: String, value: String, description: String) extends NameValuePair

object NameValuePair {
  def apply(name: String, value: String, description: String = ""): NameValuePair =
    SimpleNameValuePair(name, value, description)
}

trait ConfigValue extends NameValuePair {

  /** URL convention for source */
  def source: String

  /**
    * Profile, for example in Spring
    */
  def profile: String
}

case class SimpleConfigValue(
                              name: String,
                              value: String,
                              source: String,
                              profile: String = "",
                              description: String = ""
                            )
  extends ConfigValue

/**
  * Note that we can figure out the hierarchy from the value names
  */
trait Configuration {

  def configurationValues: Seq[ConfigValue]

  def configurationValue(name: String): Option[ConfigValue] = configurationValues.find(cv => name.equals(cv.name))
}

class SimpleConfiguration(_configuration: Seq[ConfigValue])
  extends Configuration {

  override val configurationValues: Seq[ConfigValue] = _configuration
}
