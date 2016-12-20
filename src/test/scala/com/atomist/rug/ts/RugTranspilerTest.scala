package com.atomist.rug.ts

import com.atomist.rug.{RugEditor, RugProgram}
import com.atomist.rug.parser.{ParserCombinatorRugParser, RugParser}
import com.atomist.util.lang.JavaHelpers
import org.scalatest.{FlatSpec, Matchers}

class RugTranspilerTest extends FlatSpec with Matchers {

  import com.atomist.rug.parser.CommonRugParserTest._

  val rugParser: RugParser = new ParserCombinatorRugParser()

  val transpiler = new RugTranspiler(RugTranspilerConfig(), rugParser)

  it should "emit TS for with with literal == string in predicate" in {
    val progs = rugParser.parse(EqualsLiteralStringInPredicate)
    val prog = progs.head
    val ts = transpiler.emit(progs)
    verify(prog, ts)
  }

  it should "emit TS for with with let == string in predicate" in {
    val progs = rugParser.parse(EqualsLetStringInPredicate)
    val prog = progs.head
    val ts = transpiler.emit(progs)
    verify(prog, ts)
  }

  it should "emit TS for with File with == string predicate and param" in {
    val progs = rugParser.parse(EqualsLiteralStringInPredicatesWithParam)
    val prog = progs.head
    val ts = transpiler.emit(progs)
    verify(prog, ts)
  }

  it should "invoke other operation with single parameter" in {
    val progs = rugParser.parse(InvokeOtherOperationWithSingleParameter)
    val prog = progs.head
    val ts = transpiler.emit(progs)
    verify(prog, ts)
  }

  it should "handle multiple predicates" in pending

  it should "handle well-known regex in parameter" in pending

  it should "handle inline JavaScript block" is pending

  private def verify(rug: RugProgram, ts: String): Unit = {
    ts.contains(s"class ${rug.name}") should be (true)
    ts.contains(rug.description) should be (true)
    rug match {
      case ed: RugEditor =>
        ts.contains("implements ProjectEditor") should be (true)
    }

    rug.runs.foreach(roo => {
      // Should bring it in in the constructor
      val varName = JavaHelpers.lowerize(roo.name)
      // Should invoke it in the editor body
      ts.contains(s"""project.editWith("${roo.name}", """) should be (true)
    }
    )
  }

}
