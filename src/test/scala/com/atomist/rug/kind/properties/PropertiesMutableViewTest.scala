package com.atomist.rug.kind.properties

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaClassTypeUsageTest
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class PropertiesMutableViewTest extends FlatSpec with Matchers {

  lazy val propertiesFile = JavaClassTypeUsageTest.NewSpringBootProject.findFile("src/main/resources/application.properties").get

  "PropertiesMutableView" should "get an existing valid property" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))

    propertiesView.getValue("server.port") should be ("8080")

    propertiesView.dirty equals false
  }

  it should "fail to get a property that doesn't exist" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))

    propertiesView.getValue("server.portlet") should be ("")

    propertiesView.dirty equals false
  }

  it should "set a property that exists" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))

    val preExistingPropertyKey = "server.port"
    val newValueToBeSet = "8181"
    propertiesView.setProperty(preExistingPropertyKey, newValueToBeSet)
    propertiesView.dirty equals true
    propertiesView.getValue(preExistingPropertyKey) should be (newValueToBeSet)
  }

  it should "add a property that does not exist" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))

    val newPropertyKey = "server.portlet"
    val newValueToBeSet = "8181"
    propertiesView.setProperty(newPropertyKey, newValueToBeSet)
    propertiesView.dirty equals true
    propertiesView.getValue(newPropertyKey) should be (newValueToBeSet)
  }

  it should "find and report that it contains a specific key" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))
    val keyToSearchFor = "server.port"

    propertiesView.containsKey(keyToSearchFor) should be (true)
    propertiesView.dirty equals false
  }

  it should "find and report that it contains a specific value" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))
    val valueToSearchFor = "8080"

    propertiesView.containsValue(valueToSearchFor) should be (true)
    propertiesView.dirty equals false
  }

  it should "provide access to a list of keys" in {
    val propertiesView = new PropertiesMutableView(propertiesFile, new ProjectMutableView(EmptyArtifactSource(""), JavaClassTypeUsageTest.NewSpringBootProject))

    val keys = propertiesView.keys
    keys.size should be (4)
    propertiesView.dirty equals false
  }
}
