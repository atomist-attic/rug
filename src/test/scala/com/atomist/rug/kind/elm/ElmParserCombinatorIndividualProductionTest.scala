package com.atomist.rug.kind.elm

import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels.{ElmFunctionApplication, ElmInfixFunctionApplication, StringConstant}
import org.scalatest.{FlatSpec, Matchers}

class ElmParserCombinatorIndividualProductionTest extends FlatSpec with Matchers {

  it should "parse a string constant" in {
    val input = """ "Foo" """
    val result = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.stringConstant, input)
    result match {
      case sc: StringConstant => sc.s should be ("Foo")
    }
  }

  it should "parse case statement" in {
    val input =
      """
        |case msg of
        |        Increment ->
        |            model
        |
        |$$      Noop ->
        |            model
      """.stripMargin
    val cs = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.caseExpression, input)
    cs.clauses.size should be (2)
  }

  it should "parse this function call" in {
    val input=
      """(Html.App.program
            { init = init
            , subscriptions = subscriptions
            , update = update
            , view = view
            })"""

    val e = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.expression, input)
    e match {
      case fa: ElmFunctionApplication => fa.parameters.size should be(1)
    }
  }

  it should "parse ifs" in {
    val input =
      """ if 7 == 15 then
        |     "foo"
        |$$ else
        |     "bar"
      """.stripMargin

    val e = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.expression, input)
  }

  it should "accept a move to the left before a curly brace " in {
    val input = "{ model | x = foo $$ }"
    val e = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.RecordExpressions.recordSuchThat, input)
  }

  it should "accept a move to the left before a close paren " in {
    val input = """(Css.asPairs
                  |                [ Css.position Css.absolute
                  |                , Css.left (Css.px (toFloat model.lastClick.x))
                  |                , Css.top (Css.px (toFloat model.lastClick.y))
                  |                ]
                  |$$            )""".stripMargin
    val e = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.parentheticalExpression, input)
  }

  it should "parse infix ops and depth of field access on rhs of record value assignment" in {
    val input = "{ model | x = (x * 100) // model.windowSize.width }"
    val e = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.RecordExpressions.recordSuchThat, input)
  }

}
