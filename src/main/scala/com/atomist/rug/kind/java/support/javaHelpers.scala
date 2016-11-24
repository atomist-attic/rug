package com.atomist.rug.kind.java.support

import com.atomist.param.ParameterValidationPatterns
import com.atomist.source.FileArtifact

/**
  * Convenience methods for working with Java.
  */
object JavaHelpers {

  val isJavaSourceArtifact: FileArtifact => Boolean = f => f.name.endsWith(".java")

  /**
    * Convert a string such as big-thing to a Java class name like BigThing.
    *
    * @param name property named to convert, which may include hyphen or.
    * @return Java class name
    */
  def toJavaClassName(name: String) = {
    require(name != null && name.nonEmpty, s"Java class name must not be empty or null: [$name] was invalid")
    val tokes = name.split("[-\\._]")
    capitalizeTokens(tokes)
  }

  private def toJavaIdentifierPart(c: Char): Char = {
    if (Character.isJavaIdentifierPart(c)) c
    else '_'
  }

  /**
    * Convert a string such as big_thing to a Java variable/field/method name like bigThing.
    * Will guarantee that resulting name is not a reserved word and is a valid Java identifier.
    *
    * @param name name, which may include hyphens or underscore. Useful in JSON binding
    * @return Java variable/field/method name
    */
  def toJavaVariableName(name: String) = {
    require(name != null && name.nonEmpty, "Property name must not be empty or null")
    val cleanName = name.toList.map(c => toJavaIdentifierPart(c)).mkString
    val varName = toCamelizedPropertyName(cleanName)
    if (JavaParserUtils.isReservedWord(varName) || !Character.isJavaIdentifierStart(varName.charAt(0)))
      "m_" + varName
    else varName
  }

  /**
    * Convert a string such as big_thing to a Java property name like bigThing. 
    *
    * @param name name, which may include hyphens or underscore. Useful in JSON binding
    * @return Java property name
    */
  def toCamelizedPropertyName(name: String) = {
    require(name != null && name.nonEmpty, "Property name must not be empty or null")
    val tokes = name.split("[-\\._/]")
    require(tokes.nonEmpty, "Property name must not be empty or null")
    lowerize(tokes.head) + tokes.tail.map(toke => upperize(toke)).mkString
  }

  /**
    * Take a Java property name and produce a delimited String of all lower case.
    *
    * @param javaPropertyName Java property name like bigThing
    * @return delimited string like big_think (delimiter is _ in this example)
    */
  def toLowerCaseDelimited(javaPropertyName: String, delim: String = "_"): String = {
    val chars: Seq[String] = javaPropertyName.map(c =>
      if (c.isUpper) delim + c.toLower else "" + c
    )
    chars.mkString("")
  }

  // Run them together with capitalization. So git-service becomes GitService
  private def capitalizeTokens(tokens: Seq[String]) = {
    tokens.map(toke => upperize(toke)).mkString
  }

  def getterNameToPropertyName(name: String) = {
    if (name.startsWith("get")) lowerize(name.substring(3))
    else if (name.startsWith("is")) lowerize(name.substring(2))
    else name
  }

  def propertyNameToGetterName(name: String) = {
    if (!name.startsWith("get")) "get" + upperize(name)
    else name
  }

  def propertyNameToSetterName(name: String) = {
    if (!name.startsWith("get")) "set" + upperize(name)
    else name
  }

  def stripSuffixIfPresent(name: String, suffix: String) = {
    if (name.endsWith(suffix)) name.dropRight(suffix.length) else name
  }

  def upperize(s: String) = s.length match {
    case 0 | 1 => s.take(1).toUpperCase
    case _ => s(0).toUpper + s.substring(1)
  }

  def lowerize(s: String) = s.length match {
    case 0 | 1 => s.take(1).toLowerCase
    case _ => s(0).toLower + s.substring(1)
  }

  def pathToPackageName(path: String) = path.replace(".", "/")

  def packageNameToPath(pkg: String) = pkg.replace("/", ".")

  def packageFor(classFqn: String): String = {
    classFqn.split("\\.").dropRight(1).mkString(".")
  }

  def isValidPackageName(fqn: String) = {
    fqn matches ParameterValidationPatterns.JavaPackage
  }

  def isValidJavaIdentifier(n: String) = {
    n matches ParameterValidationPatterns.JavaIdentifier
  }
}

object JavaConstants {

  val DefaultBaseSourcePath = "src/main/java"

  val DefaultBaseTestPath = "src/test/java"

  val DefaultBaseTestResources = "src/test/resources"
}

object MavenConstants {

  val PomPath = "pom.xml"
}
