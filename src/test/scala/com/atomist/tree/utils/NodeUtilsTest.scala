package com.atomist.tree.utils

import com.atomist.graph.GraphNode
import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by rod on 2/23/17.
  */
class NodeUtilsTest extends FlatSpec with Matchers {

  import NodeUtils._

  it should "work with Java collection" in {
    val gn = new ProjectMutableView(EmptyArtifactSource())
    hasNoArgMethod[java.util.List[_]](gn, "files") should be (true)
  }

  it should "work with no such method" in {
    val gn = new ProjectMutableView(EmptyArtifactSource())
    hasNoArgMethod[java.util.List[_]](gn, "xxx") should be (false)
  }

  it should "match type (String)" in {
    val gn = new ProjectMutableView(EmptyArtifactSource())
    hasNoArgMethod[String](gn, "name") should be (true)
  }

  it should "not match type (GraphNode)" in {
    val gn = new ProjectMutableView(EmptyArtifactSource())
    hasNoArgMethod[GraphNode](gn, "name") should be (false)
  }

  it should "not match type (Int)" in {
    val gn = new ProjectMutableView(EmptyArtifactSource())
    hasNoArgMethod[Int](gn, "name") should be (false)
  }

  it should "match type (Int)" in {
    val gn = new ProjectMutableView(ParsingTargets.SpringIoGuidesRestServiceSource)
    val f = gn.files.get(0)
    hasNoArgMethod[Int](f, "lineCount") should be (true)
  }

  it should "not match type (Int to String)" in {
    val gn = new ProjectMutableView(ParsingTargets.SpringIoGuidesRestServiceSource)
    val f = gn.files.get(0)
    hasNoArgMethod[String](f, "lineCount") should be (false)
  }

}
