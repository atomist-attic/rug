package com.atomist.rug.kind.yaml

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object YmlMutableViewTest {

  val yaml =
    """
      |name:
      |    test1
      |
      |description:
      |   A template of profound illustrative power.
    """.stripMargin

  val yaml2 =
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

  import YmlMutableViewTest._

  val y = StringFileArtifact("info.yml", yaml)

  val as = new SimpleFileBasedArtifactSource("name",
    Seq(
      y
    )
  )

  it should "find key in simple yaml" in {
    val simpleYaml =
      """
         |name:
         |  test1
         |
         |description:
         |  A template of profound illustrative power.
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.valueOf("name") should be("test1")
  }

  it should "set key in simple yaml" in {
    val simpleYaml =
      """
        |name:
        |    test1
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.updateKey("name", "Theresa")
    assert(yv.dirty === true)
    assert("Theresa".r.findAllIn(yv.content).toList.size === 1)
  }

  it should "set key in simple yaml without affecting other keys" in {
    val simpleYaml =
      """
        |# Some comment
        |name:
        |    test1
        |# Some other comment
        |other:
        |    test1 # Yet Another Comment!
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.updateKey("name", "Theresa")
    assert(yv.dirty === true)
    assert("Theresa".r.findAllIn(yv.content).toList.size === 1)
  }

  it should "set keys in simple yaml maintaining comments" in {
    val firstComment = "# Some comment"
    val secondComment = "# Some other comment"
    val thirdComment = "# Yet Another Comment!"
    val simpleYaml =
      s"""
         |$firstComment
         |name:
         |    test1
         |$secondComment
         |other:
         |    test1 $thirdComment
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.updateKey("name", "Theresa")
    assert(yv.dirty === true)
    assert("Theresa".r.findAllIn(yv.content).toList.size === 1)
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
    val simpleYaml =
      s"""
         |$firstComment
         |name:
         |    test1
         |$secondComment
         |other:
         |    test1 $thirdComment
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", simpleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.updateKey("name", "Theresa")
    assert(yv.dirty === true)
    assert("Theresa".r.findAllIn(yv.content).toList.size === 1)
    yv.content.contains(firstComment)
    yv.content.contains(secondComment)
    yv.content.contains(thirdComment)
  }

  it should "find first key in multiple document yaml" in {
    val doubleYaml =
      """
        |---
        |name: test1
        |description: A template of profound illustrative power.
        |---
        |name: test2
        |description: Ignored less powerful template.
      """.stripMargin
    val yv = new YmlMutableView(StringFileArtifact("info.yml", doubleYaml),
      new ProjectMutableView(EmptyArtifactSource(""), as))
    yv.valueOf("name") should be("test1")
  }

}
