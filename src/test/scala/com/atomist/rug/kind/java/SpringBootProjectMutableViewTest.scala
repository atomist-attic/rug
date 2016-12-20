package com.atomist.rug.kind.java

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource
import org.scalatest.FlatSpec

import JavaVerifier._

class SpringBootProjectMutableViewTest extends FlatSpec {

  import JavaTypeUsageTest._

  it should "confirm is Spring Boot project" in {
    val sbp = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))
    sbp.isSpringBoot should be(true)
  }

  it should "annotate Spring Boot class" in {
    val sbp = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))
    sbp.annotateBootApplication("com.foo", "Bar")
    val appClass = sbp.currentBackingObject.findFile("src/main/java/com/atomist/test1/Test1Application.java").get
    appClass.content.contains("@Bar") should be(true)
    appClass.content.contains("import com.foo.Bar") should be(true)
    verifyJavaIsWellFormed(sbp.currentBackingObject)
  }

  it should "find Spring Boot application class simple name" in {
    val sbp = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))
    sbp.applicationClassSimpleName should be ("Test1Application")
    verifyJavaIsWellFormed(sbp.currentBackingObject)
  }

  it should "find Spring Boot application class FQN" in {
    val sbp = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))
    sbp.applicationClassFQN should be ("com.atomist.test1.Test1Application")
    verifyJavaIsWellFormed(sbp.currentBackingObject)
  }

  it should "find Spring Boot application package" in {
    val sbp = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))
    sbp.applicationClassPackage should be ("com.atomist.test1")
    verifyJavaIsWellFormed(sbp.currentBackingObject)
  }
  
}
