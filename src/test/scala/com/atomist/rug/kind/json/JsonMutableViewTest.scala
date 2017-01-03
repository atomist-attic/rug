package com.atomist.rug.kind.json

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.PathExpressionEngine
import com.atomist.tree.{ContainerTreeNode, MutableTreeNode}
import org.scalatest.{FlatSpec, Matchers}

class JsonMutableViewTest extends FlatSpec with Matchers {

  import JsonParserTest._
  import com.atomist.tree.pathexpression.PathExpressionParser._

  it should "parse and find node in root" in {
    val f = StringFileArtifact("glossary.json", simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv)
    j.nodeType should be (Set("Json"))
    j.childrenNamed("glossary").size should be(1)
  }

  it should "support path find" in {
    val ee = new PathExpressionEngine
    val expr = "/glossary/GlossDiv/title"
    val f = StringFileArtifact("glossary.json", simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv)
    val rtn = ee.evaluate(j, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
    rtn.right.get.head.asInstanceOf[ContainerTreeNode].childrenNamed("STRING").head.value should be ("S")
  }

  it should "update path find" in {
    val ee = new PathExpressionEngine
    val expr = "/glossary/GlossDiv/GlossList/GlossEntry/GlossSee"
    val f = StringFileArtifact("glossary.json", simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv)
    val rtn = ee.evaluate(j, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
    val target = rtn.right.get.head.asInstanceOf[ContainerTreeNode].childrenNamed("STRING").head.asInstanceOf[MutableTreeNode]
    target.value should be ("markup")
    target.update("XSLT")
    j.value should equal(simple.replace("\"markup", "\"XSLT"))
  }

}
