package com.atomist.rug.kind.javascript

import com.atomist.model.content.text.{MutableContainerTreeNode, TreeNodeUtils}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptParserTest extends FlatSpec with Matchers with LazyLogging {

  lazy val parser = new JavaScriptParser

  import JavaScriptParserTest._

  def stringShowingIndices(s: String) = {
    val z = s.zipWithIndex
    "len=" + s.length + " :"  + z.mkString(",")
  }

  it should "parse simplest file and write out unchanged" in pendingUntilFixed {
    val tree = parseAndVerifyValueCanBeWrittenOutUnchanged(simple)
    logger.debug(TreeNodeUtils.toShortString(tree))
  }

  // TODO this is shared with Python test, can probably make it common
  private def parseAndVerifyValueCanBeWrittenOutUnchanged(prog: String): MutableContainerTreeNode = {
    if (prog.lines.exists(_.trim.startsWith("|")))
      fail(s"Probably a test error. Did you forget to call stripMargin?")
    val parsed = parser.parse(prog)
    logger.debug(TreeNodeUtils.toShortString(parsed))

    val actual = parsed.value.trim
    val expected = prog.trim

    val comp = s"Result: --------------\n[$actual]\nExpected: -----------\n[$expected]"
    withClue(comp) {
      actual should equal(expected)
    }
    parsed
  }
}

object JavaScriptParserTest {

  val simple =
    """
      |var x = 5;
      |var y = 6;
      |var z = x + y;
      |document.getElementById("demo").innerHTML = z;
    """.stripMargin
}
