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

  val jsonParser = new JsonParser


  it should "parse and find node in root" in {
    val f = StringFileArtifact("glossary.json", Simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv, jsonParser.parse(f.content).get)
    j.nodeTags.contains("Json") should be (true)
    assert(j.childrenNamed("glossary").size === 1)
  }

  it should "support path find" in {
    val ee = new PathExpressionEngine
    val expr = "/glossary/GlossDiv/title"
    val f = StringFileArtifact("glossary.json", Simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv, jsonParser.parse(f.content).get)
    val rtn = ee.evaluate(j, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
    assert(rtn.right.get.head.asInstanceOf[ContainerTreeNode].childrenNamed("STRING").head.value === "S")
  }

  it should "update path find" in {
    val ee = new PathExpressionEngine
    val expr = "/glossary/GlossDiv/GlossList/GlossEntry/GlossSee"
    val f = StringFileArtifact("glossary.json", Simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val j = new JsonMutableView(f, pmv, jsonParser.parse(f.content).get)
    val rtn = ee.evaluate(j, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
    val target = rtn.right.get.head.asInstanceOf[ContainerTreeNode].childrenNamed("STRING").head.asInstanceOf[MutableTreeNode]
    assert(target.value === "markup")
    target.update("XSLT")
    assert(j.value === Simple.replace("\"markup", "\"XSLT"))
  }

  it should "find descendant in project" in {
    val ee = new PathExpressionEngine
    val expr = "/src/main/resources//Json()//GlossSee"
    val f = StringFileArtifact("src/main/resources/glossary.json", Simple)
    val proj = SimpleFileBasedArtifactSource(f)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj)
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
    val x = rtn.right.get.head.asInstanceOf[ContainerTreeNode]
    assert(x.nodeName === "GlossSee")
    val target = x.childrenNamed("STRING").head.asInstanceOf[MutableTreeNode]
    assert(target.value === "markup")
    target.update("XSLT")
    //j.value should equal(Simple.replace("\"markup", "\"XSLT"))
  }

}
