package com.atomist.rug.kind.yml

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.SimpleFunctionInvocationContext
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object Fixtures {

  val yml =
    """
      |name:
      |    test1
      |
      |description:
      |   A template of profound illustrative power.
    """.stripMargin

  val yml2 =
    """
      |name:
      |    test1
      |
      |description:
      |   A template of profound illustrative power.
      |
      |parameters:
      |
      |  - name: number1
      |    required: true
      |    description: A number of great significance.
      |    default-value: 42
      |    pattern: \d+
      |    valid-input-description: A positive number.
      |
      |  - name: param2
      |    required: true
      |    pattern: .*
      |
      |tags:
      |
      | - name: spring
      |   description: Spring Framework
      |
      | - name: spring-boot
      |   description: Spring Boot
      |   parent: spring
      |
    """.stripMargin
}

class YmlMutableViewTest extends FlatSpec with Matchers {

  import Fixtures._

  val y = StringFileArtifact("info.yml", yml)

  val as = new SimpleFileBasedArtifactSource("name",
    Seq(
      y
    )
  )

  it should "find key in simple yml" in {
    val simpleYml =
      """
    name:
      test1

    description:
      A template of profound illustrative power.
      """
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    //    val ic = SimpleFunctionInvocationContext("p", yv, as, null, Map(),
    //      SimpleProjectOperationArguments.Empty, Nil)
    yv.valueOf("name") should be("test1")
  }

  it should "set key in simple yml" in {
    val simpleYml =
      """
        |name:
        |    test1
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    val ic = SimpleFunctionInvocationContext("p", null, yv, as, null, Map(),
      SimpleProjectOperationArguments.Empty, Nil)
    yv.updateKey("name", "Theresa")
    yv.dirty should be(true)
    "Theresa".r.findAllIn(yv.content).toList.size should be(1)
  }

  it should "set key in simple yml without affecting other keys" in {
    val simpleYml =
      """
        |# Some comment
        |name:
        |    test1
        |# Some other comment
        |other:
        |    test1 # Yet Another Comment!
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    val ic = SimpleFunctionInvocationContext("p", null, yv, as, null, Map(),
      SimpleProjectOperationArguments.Empty, Nil)
    yv.updateKey("name", "Theresa")
    yv.dirty should be(true)
    "Theresa".r.findAllIn(yv.content).toList.size should be(1)
  }

  it should "set keys in simple yml maintaining comments" in {
    val firstComment = "# Some comment"
    val secondComment = "# Some other comment"
    val thirdComment = "# Yet Another Comment!"
    val simpleYml =
      s"""
         |$firstComment
         |name:
         |    test1
         |$secondComment
         |other:
         |    test1 $thirdComment
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    val ic = SimpleFunctionInvocationContext("p", null, yv, as, null, Map(),
      SimpleProjectOperationArguments.Empty, Nil)
    yv.updateKey("name", "Theresa")
    yv.dirty should be(true)
    "Theresa".r.findAllIn(yv.content).toList.size should be(1)
    yv.content.contains(firstComment)
    yv.content.contains(secondComment)
    yv.content.contains(thirdComment)
  }

  it should "should respect whitespace" in {
    val firstComment =
      """
        |
        |# Some comment""".stripMargin
    val secondComment = "# Some other comment"
    val thirdComment = "# Yet Another Comment!"
    val simpleYml =
      s"""
         |$firstComment
         |name:
         |    test1
         |$secondComment
         |other:
         |    test1 $thirdComment
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    val ic = SimpleFunctionInvocationContext("p", null, yv, as, null, Map(),
      SimpleProjectOperationArguments.Empty, Nil)
    yv.updateKey("name", "Theresa")
    yv.dirty should be(true)
    "Theresa".r.findAllIn(yv.content).toList.size should be(1)
    yv.content.contains(firstComment)
    yv.content.contains(secondComment)
    yv.content.contains(thirdComment)
  }
}
