package com.atomist.rug.kind.xml

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class XmlMutableViewTest extends FlatSpec with Matchers {

  lazy val pom = JavaTypeUsageTest.NewSpringBootProject.findFile("pom.xml").get

  "XmlMutableView" should "add a new block as a child of another block" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val newNodeContent = "<plugin><groupId>com.atomist</groupId><artifactId>our-great-plugin</artifactId></plugin>"

    val newNodeName = "plugin"

    xv.addOrReplaceNode("/project/build/plugins", """/project/build/plugins/plugin/artifactId [text() = "our-great-plugin"]""", newNodeName, newNodeContent)

    assert(xv.dirty === true)

    checkCommentStillPresent(xv, "Add GIT commit information to the info endpoint")

    xv.content.contains("<artifactId>our-great-plugin</artifactId>") should be (true)

    val secondNewNodeContent = "<dependency><groupId>com.atomist</groupId><artifactId>an-atomist-artifact</artifactId><scope>test</scope></dependency>"

    val secondNewNodeName = "dependency"

    xv.addOrReplaceNode("/project/dependencies", """/project/dependencies/dependency/artifactId [text() = "an-atomist-artifact"]""", secondNewNodeName, secondNewNodeContent)

    assert(xv.dirty === true)

    checkCommentStillPresent(xv, "Add GIT commit information to the info endpoint")

    xv.content.contains("<artifactId>an-atomist-artifact</artifactId>") should be (true)
  }

  it should "report if an element is present according to xpath" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val validXPath = "//project/dependencies"
    val invalidXPath = "//project/stuff"

    xv.contains(validXPath) should be (true)

    xv.contains(invalidXPath) should be (false)
  }

  it should "get a value from an element with text content" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val xpathToElementWithTextValue = "//project/groupId"

    xv.getTextContentFor(xpathToElementWithTextValue) should be ("atomist")
  }

  it should "set a value on an element with text content" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val xpathToElementWithTextValue = "/project/groupId"

    xv.setTextContentFor(xpathToElementWithTextValue, "donny")

    assert(xv.dirty === true)

    xv.getTextContentFor(xpathToElementWithTextValue) should be ("donny")
  }

  it should "add or replace an existing node with a new node" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val replacementNode = "project.build.sourceEncoding"
    val xPathToParentNode = s"/project/properties"
    val newValue = "newvalue"
    val newContent = s"<$replacementNode>$newValue</$replacementNode>"
    val fullXPathToNode = s"$xPathToParentNode/$replacementNode"

    xv.addOrReplaceNode(xPathToParentNode, fullXPathToNode, replacementNode, newContent)

    assert(xv.dirty === true)

   xv.getTextContentFor(fullXPathToNode) should be (newValue)

    val newNode = "project.build.sourceEncoding.dummy"
    val newValue2 = "anothernewvalue"
    val newContent2 = s"<$newNode>$newValue2</$newNode>"
    val fullXPathToAddedNode = s"$xPathToParentNode/$newNode"

    xv.addOrReplaceNode(xPathToParentNode, fullXPathToAddedNode, newNode, newContent2)

    assert(xv.dirty === true)

    xv.getTextContentFor(fullXPathToAddedNode) should be (newValue2)
  }

  it should "delete the specified node" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val replacementNode = "project.build.sourceEncoding"
    val xPathToParentNode = s"/project/properties"
    val fullXPathToNode = s"$xPathToParentNode/$replacementNode"

    xv.deleteNode(fullXPathToNode)

    assert(xv.dirty === true)

    xv.getTextContentFor(fullXPathToNode) should be ("")
  }

  it should "delete the specific node among many peers" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val fullXPathToNode = "/project/dependencies/dependency/artifactId[text()='spring-boot-starter-actuator']/.."

    xv.deleteNode(fullXPathToNode)

    assert(xv.dirty === true)

    xv.getTextContentFor(fullXPathToNode) should be ("")
  }

  it should "replace an existing node when an XPath condition is met" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val nodeToReplaceXpathSelector = "/project/dependencies/dependency/artifactId[text()='spring-boot-starter-actuator']/.."
    val xPathToPlaceToInsertContent = "/project/dependencies"
    val nodeName = "dependency"
    val newNodeContent = """<dependency><groupId>atomist</groupId><artifactId>atomistartifact</artifactId><scope>test</scope></dependency>"""

    xv.addOrReplaceNode(xPathToPlaceToInsertContent, nodeToReplaceXpathSelector, nodeName, newNodeContent)

    assert(xv.dirty === true)

    xv.content.contains("<artifactId>atomistartifact</artifactId>") should be (true)
    xv.content.contains("<artifactId>spring-boot-starter-actuator</artifactId>") should be (false)
  }

  it should "add a new node when an XPath condition is not met" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val nodeToReplaceXpathSelector = "/project/dependencies/dependency/artifactId[text()='spring-boot-starter-web-DUMMY']/.."
    val xPathToPlaceToInsertContent = "/project/dependencies"
    val nodeName = "dependency"
    val newNodeContent = """<dependency><groupId>atomist</groupId><artifactId>atomistartifact</artifactId><scope>test</scope></dependency>"""

    xv.addOrReplaceNode(xPathToPlaceToInsertContent, nodeToReplaceXpathSelector, nodeName, newNodeContent)

    assert(xv.dirty === true)

    xv.content.contains("<artifactId>atomistartifact</artifactId>") should be (true)
    xv.content.contains("<artifactId>spring-boot-starter-web</artifactId>") should be (true)
  }

  it should "replace the right child element when many are available" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val artifactId = "git-commit-id-plugin"
    val groupId = "pl.project13.maven"

    val newArtifactId = "atomist-build-plugin"
    val newGroupId = "atomistgroup"

    val nodeToReplaceXpathSelector = s"/project/build/plugins/plugin/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/.."
    val xpathOfParent = s"/project/build/plugins"
    val replacementNode = "plugin"
    val replacementContent = s"""<plugin><groupId>$newGroupId</groupId><artifactId>$newArtifactId</artifactId></plugin>"""
    val xpathSelectorReplacement = s"/project/build/plugins/plugin/artifactId[text()='$newArtifactId' and ../groupId[text() = '$newGroupId']]"

    xv.contains(nodeToReplaceXpathSelector) should be (true)
    xv.contains(xpathSelectorReplacement) should be (false)

    xv.addOrReplaceNode("/project/build/plugins", nodeToReplaceXpathSelector, replacementNode, replacementContent)

    xv.contains(xpathSelectorReplacement) should be (true)
    xv.contains(nodeToReplaceXpathSelector) should be (false)
  }

  it should "successfully execute a combinatorial selection" in {
    val xv = new XmlMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    val validCombinationXPath = s"/project/dependencies/dependency/artifactId[text()='spring-boot-starter-web' and ../groupId[text() = 'org.springframework.boot']]"
    val invalidCombinationXPath = s"/project/dependencies/dependency/artifactId[text()='spring-boot-starter-webXXX' and ../groupId[text() = 'org.springframework.boot']]"
    val invalidCombinationXPath2 = s"/project/dependencies/dependency/artifactId[text()='spring-boot-starter-web' and ../groupId[text() = 'org.springframework.bootXXX']]"

    xv.contains(validCombinationXPath) should be (true)
    xv.contains(invalidCombinationXPath) should be (false)
    xv.contains(invalidCombinationXPath2) should be (false)
  }

  def checkCommentStillPresent(xv: XmlMutableView, comment: String): Boolean = {
    xv.content.contains(comment)
  }
}
