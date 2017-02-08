package com.atomist.param

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty, JsonSetter}

import scala.collection.mutable.ListBuffer

class Parameter @JsonCreator()(@JsonProperty("name") val name: String) {

  @JsonProperty
  var description: String = ""

  /**
    * Default value for this parameter. Empty if there is no default.
    */
  @JsonProperty("default_value")
  private var defaultValue: String = ""

  /**
    * Reference to another property name.
    */
  @JsonProperty("default_ref")
  private var defaultRef: String = _

  /**
    * Regular expression used to validate this parameter.
    */
  @JsonProperty
  private var pattern: String = ParameterValidationPatterns.MatchAny

  /**
    * Description of what valid input looks like. This can be
    * displayed to the user if validation using the pattern property fails.
    */
  @JsonProperty("valid_input_description")
  private var validInputDescription: String = "String value"

  @JsonProperty
  private var required: Boolean = true

  /**
    * Should we display this to users or is it purely for machines?
    */
  @JsonProperty
  private var displayable: Boolean = true

  /**
    * Returned to identify parameter.
    */
  @JsonProperty
  private val tags = new ListBuffer[Tag]

  @JsonProperty("max_length")
  private var maxLength: Int = -1

  @JsonProperty("min_length")
  private var minLength: Int = -1

  @JsonProperty("display_name")
  private var displayName: String = _

  @JsonProperty("allowed_values")
  private val allowedValues = new ListBuffer[AllowedValue]

  def getName: String = name

  def getDescription: String = description

  @JsonSetter
  def describedAs(description: String): this.type = {
    this.description = description
    this
  }

  def getDefaultValue: String = defaultValue

  def hasDefaultValue: Boolean = defaultValue != null && !"".equals(defaultValue)

  def setDefaultValue(defaultValue: String): this.type = {
    this.defaultValue = defaultValue
    this
  }

  def getDefaultRef: String = defaultRef

  def hasDefaultRef: Boolean = defaultRef != null && !"".equals(defaultRef)

  def setDefaultRef(defaultRef: String): this.type = {
    this.defaultRef = defaultRef
    this
  }

  def getPattern: String = pattern

  def setPattern(pattern: String): this.type = {
    this.pattern = pattern
    this
  }

  def getValidInputDescription: String = validInputDescription

  def setValidInputDescription(validInputDescription: String): this.type = {
    this.validInputDescription = validInputDescription
    this
  }

  def isRequired: Boolean = required

  def setRequired(required: Boolean): this.type = {
    this.required = required
    this
  }

  def isDisplayable: Boolean = displayable

  def setDisplayable(displayable: Boolean): this.type = {
    this.displayable = displayable
    this
  }

  def getTags: ListBuffer[Tag] = tags.filterNot(_ == null)

  def tagWith(tag: Tag): this.type = {
    tags += tag
    this
  }

  def addTags(tags: Seq[Tag]): this.type = {
    this.tags ++= tags
    this
  }

  def getMaxLength: Int = maxLength

  def setMaxLength(maxLength: Int): this.type = {
    this.maxLength = maxLength
    this
  }

  def getMinLength: Int = minLength

  def setMinLength(minLength: Int): this.type = {
    this.minLength = minLength
    this
  }

  def getDisplayName: String = displayName

  def setDisplayName(displayName: String): this.type = {
    this.displayName = displayName
    this
  }

  def getAllowedValues: ListBuffer[AllowedValue] = allowedValues.filterNot(_ == null)

  def setAllowedValues(allowedValues: Seq[AllowedValue]): this.type = {
    this.allowedValues.clear()
    this.allowedValues ++= allowedValues
    this
  }

  def withAllowedValue(allowedValue: AllowedValue): this.type = {
    allowedValues += allowedValue
    this
  }

  def withAllowedValue(name: String, displayName: String): this.type = {
    withAllowedValue(AllowedValue(name, displayName))
    this
  }

  /** If obj is a String:
    * -  If there are allowedValues, return true if value is in the list of allowedValues
    * -  If allowedValues is empty, return true if value meets the minimum and maximum
    *    length requirements and it satisfies the parameter validation regular
    *    expression.
    * Otherwise return false.
    * If obj is not a String, return false.
    *
    * @param obj value to be checked
    * @return true is the obj is a valid value, false otherwise.
    */
  def isValidValue(obj: Any): Boolean = obj match {
    case s: String =>
      if (allowedValues.nonEmpty) allowedValues.exists(_.value == s)
      else ((minLength < 0 || s.length >= minLength) &&
        (maxLength < 0 || s.length <= maxLength) &&
        pattern.r.findAllMatchIn(s).nonEmpty)
    case _ => false
  }

  override def toString: String = s"Parameter{name='$name', description='$description', " +
    s"defaultValue='$defaultValue', defaultRef='$defaultRef', pattern='$pattern', " +
    s"validInputDescription='$validInputDescription', required=$required, " +
    s"displayable=$displayable, tags=$tags, maxLength=$maxLength, minLength=$minLength, " +
    s"displayName='$displayName', allowedValues=$allowedValues}"
}

object Parameter {

  def apply(name: String): Parameter = new Parameter(name)

  def apply(name: String, pattern: String): Parameter = new Parameter(name).setPattern(pattern)
}
