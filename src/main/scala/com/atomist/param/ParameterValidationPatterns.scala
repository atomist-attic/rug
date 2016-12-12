package com.atomist.param

object ParameterValidationPatterns {

  // These regular expressions are used by the Java regular expression engine
  // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
  // and the JavaScript regular expression engine
  // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions
  // so make sure any special characters are valid in both.  For example, you can use
  // \w but not \p{Alpha} (Java only).

  val MatchAll = "^.*$"
  val ProjectName = "^[-\\w.]+$"
  val JavaIdentifier = "^[A-Za-z_$][\\w$]*$"
  val JavaPackage = "^(?:(?:[A-Za-z_$][\\w$]*\\.)*[A-Za-z_$][\\w$]*)*$"
  val JavaClass = JavaIdentifier
  val GroupName = "^(?:[A-Za-z_][\\w]*\\.)*[-A-Za-z_][-\\w]*$"
  val ArtifactId = "^[a-z][-a-z0-9_]*$"

  val RubyClass = "^[A-Z][A-Za-z0-9_]*$"
  val RubyIdentifier = "^[A-Za-z_][A-Za-z0-9_]*$"

  // True semantic version http://semver.org
  val Version = "^v?(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:-(?:[1-9]\\d*|[-A-Za-z\\d]*[-A-Za-z][-A-Za-z\\d]*)(?:\\.(?:[1-9]\\d*|[-A-Za-z\\d]*[-A-Za-z][-A-Za-z\\d]*))*)?(?:\\+[-A-Za-z\\d]+(?:\\.[-A-Za-z\\d]+)*)?$"

  val NonNegativeInteger = "^(?:0|[1-9]\\d*)$"
  val Url = "^(?:https?|ftp)://[^\\s]+$"

  // TODO fix this
  val Port = NonNegativeInteger

  val Uuid = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
}
