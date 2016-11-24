package com.atomist.rug.kind.python3

import com.atomist.model.content.text.{MutableTerminalTreeNode, ParsedMutableContainerTreeNode}
import com.atomist.scalaparsing.CommonTypesParser
import com.atomist.source.StringFileArtifact

/**
  * Parse a Python requirements.txt file.
  * We can update in place.
  */
object RequirementsTxtParser extends CommonTypesParser {

  /** White space honoring C style block comments and Python style # line comments */
  override protected val whiteSpace =
  ("""(\s|""" + HashLineComment + ")+").r

  private def packageName =
    mutableTerminalNode("package-name", """[a-zA-Z][a-zA-Z0-9_\-]*""".r)

  private def requirementSpecifier: Parser[RequirementSpecifier] = ("==" | ">=") ^^ {
    case "==" => EqualsRequirementSpecifier
    case ">=" => GreaterOrEqualRequirementSpecifier
    case x => throw new IllegalArgumentException(s"Invalid requirement specifier [$x]")
  }

  // TODO there must be some limits on this
  private def version: Parser[MutableTerminalTreeNode] = mutableTerminalNode("version", ".*".r)

  private def requirement: Parser[Requirement] = packageName ~ opt(requirementSpecifier ~ version) ^^ {
    case pn ~ Some(rs ~ ver) =>
      new Requirement(pn, Some(rs), Some(ver))
    case pn ~ None =>
      // User did not specify the version
      new Requirement(pn, None, None)
  }

  private def requirementsStructure: Parser[Requirements] = rep1(positionedStructure(requirement)) ^^ {
    case l => new Requirements(l)
  }

  private def requirements: Parser[Requirements] = positionedStructure(requirementsStructure)

  def parseFile(content: String): Requirements = {
    val reqs = parseTo(StringFileArtifact("requirements.txt", content),
      phrase(requirements))
    reqs.pad(content, topLevel = true)
    reqs
  }

  def parseLine(line: String): Requirement = {
    val req = parseTo(StringFileArtifact("input", line),
      phrase(positionedStructure(requirement)))
    req.pad(line)
    req
  }
}

/**
  * Requirement. Updateable.
  * @param packageName package name to bring in
  * @param requirementSpecifier optional version specifier
  * @param version optional version
  */
class Requirement(
                   val packageName: MutableTerminalTreeNode,
                   val requirementSpecifier: Option[RequirementSpecifier],
                   val version: Option[MutableTerminalTreeNode])
  extends ParsedMutableContainerTreeNode("requirement") {

  appendField(packageName)
  version.map(appendField(_))
}

class Requirements(
                    private var _requirements: Seq[Requirement])
  extends ParsedMutableContainerTreeNode("requirements") {

  appendFields(_requirements)

  def requirements: Seq[Requirement] = _requirements

  /**
    * Add the given requirement
    * @param line Line to add, such as "foo==1.2.1"
    */
  def add(line: String) = {
    val requirement: Requirement =
      RequirementsTxtParser.parseLine(line)
    _requirements = _requirements :+ requirement
    appendField(requirement)
  }

  def update(packageName: String, newVersion: String): Unit = {
    // TODO what should happen if the package isn't imported etc.
    // currently we just blow up, which probably isn't what's desired
    val theRelevantRequirement = requirements.find(_.packageName.value.equals(packageName)).getOrElse(
      throw new UnsupportedOperationException("Do something sensible here")
    )

    theRelevantRequirement.version match {
      case Some(v) => v.update(newVersion)
      case None => ??? // Do something sensible here. It won't be known as a field
    }
  }

}

sealed trait RequirementSpecifier

object EqualsRequirementSpecifier extends RequirementSpecifier

object GreaterOrEqualRequirementSpecifier extends RequirementSpecifier