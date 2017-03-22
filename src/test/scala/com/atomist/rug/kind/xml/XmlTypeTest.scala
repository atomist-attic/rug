package com.atomist.rug.kind.xml

import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import org.scalatest.{FlatSpec, Matchers}
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}

class XmlTypeTest extends FlatSpec with Matchers {

  it should "find all the XML files in a project" in {
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject)
    val xt = new XmlType
    xt.findAllIn(pmv) match {
      case Some(xs) => assert(xs.size === 4)
      case x => fail(s"no XMLs found, found $x instead")
    }
  }

  it should "find XML in a .xml file" in {
    val xmlFile = StringFileArtifact("src/resources/royalty.xml", """<prince>Rogers Nelson</prince>""")
    val fmv = new FileMutableView(xmlFile, null)
    val xt = new XmlType
    xt.findAllIn(fmv) match {
      case Some(xs) => assert(xs.size === 1)
      case x => fail(s"no XML, found $x")
    }
  }

  it should "not find XML in a .sh file" in {
    val xmlFile = StringFileArtifact("src/resources/royalty.sh", "#!/bin/sh\necho Prince Rogers Nelson\n")
    val fmv = new FileMutableView(xmlFile, null)
    val xt = new XmlType
    xt.findAllIn(fmv) match {
      case None =>
      case x => fail(s"found XML $x when it should not have")
    }
  }
}
