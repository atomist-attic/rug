package com.atomist.rug.kind.properties

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class PropertiesMutableViewTest extends FlatSpec with Matchers {

  lazy val propertiesFile = JavaTypeUsageTest.NewSpringBootProject.findFile("src/main/resources/application.properties").get

  "PropertiesMutableView" should "get an existing valid property" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    propertiesView.getValue("server.port") should be ("8080")
    propertiesView.dirty equals false
  }

  it should "fail to get a property that doesn't exist" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    propertiesView.getValue("server.portlet") should be ("")
    propertiesView.dirty equals false
  }

  it should "set a property that exists" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val preExistingPropertyKey = "server.port"
    val newValueToBeSet = "8181"
    propertiesView.setProperty(preExistingPropertyKey, newValueToBeSet)
    propertiesView.dirty equals true
    propertiesView.getValue(preExistingPropertyKey) should be (newValueToBeSet)
  }

  it should "add a property that does not exist" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val newPropertyKey = "server.portlet"
    val newValueToBeSet = "8181"
    propertiesView.setProperty(newPropertyKey, newValueToBeSet)
    propertiesView.dirty equals true
    propertiesView.getValue(newPropertyKey) should be (newValueToBeSet)
  }

  it should "find and report that it contains a specific key" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    val keyToSearchFor = "server.port"

    propertiesView.containsKey(keyToSearchFor) should be (true)
    propertiesView.dirty equals false
  }

  it should "find and report that it contains a specific value" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    val valueToSearchFor = "8080"

    propertiesView.containsValue(valueToSearchFor) should be (true)
    propertiesView.dirty equals false
  }

  it should "provide access to a list of keys" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val keys = propertiesView.keys
    assert(keys.size === 4)
    propertiesView.dirty equals false
  }

  it should "add two new properties and format correctly" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

    val keys = propertiesView.keys
    assert(keys.size === 4)
    propertiesView.dirty equals false

    val newPropertyKey1 = "abc"
    val newPropertyValue1 = "123"
    val newPropertyKey2 = "def"
    val newPropertyValue2 = "456"

    propertiesView.setProperty(newPropertyKey1, newPropertyValue1)
    propertiesView.setProperty(newPropertyKey2, newPropertyValue2)
    assert(propertiesView.keys.size === 6)
    propertiesView.dirty equals true

    propertiesView.getValue(newPropertyKey1) should be (newPropertyValue1)
    propertiesView.getValue(newPropertyKey2) should be (newPropertyValue2)
  }
}
