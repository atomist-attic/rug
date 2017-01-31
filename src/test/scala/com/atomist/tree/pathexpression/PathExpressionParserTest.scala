package com.atomist.tree.pathexpression

import com.atomist.rug.BadRugSyntaxException
import org.scalatest.{FlatSpec, Matchers}

class PathExpressionParserTest extends FlatSpec with Matchers {

  val pep = PathExpressionParser

  it should "parse a bare root node" in {
    val pe = "/"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.isEmpty should be(true)
  }

  it should "failed to parse an unanchored path expression" in {
    val pe = "big/lebowski"
    an[BadRugSyntaxException] should be thrownBy pep.parsePathExpression(pe)
  }

  it should "parse a child axis" in {
    val pe = "/child::src"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    ls.axis should be (Child)
    ls.predicateToEvaluate should be (TruePredicate)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse an abbreviated child axis with node name" in {
    val pe = "/src"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    ls.axis should be(Child)
    ls.predicateToEvaluate should be(TruePredicate)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "an abbreviated child axis should be equivalent to an explicit child axis" in {
    val pe = "/child::src"
    val parsed = pep.parsePathExpression(pe)
    val pe1 = "/src"
    val parsed1 = pep.parsePathExpression(pe1)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    parsed1.locationSteps.size should be(1)
    val ls1 = parsed.locationSteps.head
    ls1.axis should be(ls.axis)
    ls1.predicateToEvaluate should be(ls.predicateToEvaluate)
    ls1.test should be(ls.test)
  }

  it should "parse a descendant axis" in {
    val pe = "/descendant::src"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    ls.axis should be (Descendant)
    ls.predicateToEvaluate should be (TruePredicate)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse an abbreviated descendant axis with node name" in {
    val pe = "//src"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    ls.axis should be(Descendant)
    ls.predicateToEvaluate should be(TruePredicate)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "an abbreviated descendant axis should be equivalent to an explicit descendant axis" in {
    val pe = "/descendant::src"
    val parsed = pep.parsePathExpression(pe)
    val pe1 = "//src"
    val parsed1 = pep.parsePathExpression(pe1)
    parsed.locationSteps.size should be(1)
    val ls = parsed.locationSteps.head
    parsed1.locationSteps.size should be(1)
    val ls1 = parsed.locationSteps.head
    ls1.axis should be(ls.axis)
    ls1.predicateToEvaluate should be(ls.predicateToEvaluate)
    ls1.test should be(ls.test)
  }

  it should "parse a node object type" in {
    val pe = "/Issue()"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    val ls = parsed.locationSteps.head
    ls.axis should be(Child)
    ls.predicateToEvaluate should be(TruePredicate)
    ls.test should be(NodesWithTag("Issue"))
  }

  it should "parse an index predicate" in {
    val pe = "/dude[4]"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    val ls = parsed.locationSteps.head
    ls.axis should be(Child)
    ls.predicateToEvaluate match {
      case p@IndexPredicate(4) =>
      case x => fail(s"predicate did not match expected type: $x")
    }
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("dude")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse a simple predicate" in {
    val pe = "/dude[@size='large']"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    val ls = parsed.locationSteps.head
    ls.axis should be(Child)
    ls.predicateToEvaluate match {
      case np: PropertyValuePredicate =>
        np.property should be ("size")
        np.expectedValue should be ("large")
      case x => fail(s"predicate did not match expected type: $x")
    }
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("dude")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse into json" in {
    val pe = "/*[@name='elm-package.json']/Json()/summary"
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (3)
  }

  it should "parse a property name axis specifier" in {
    val pe = """/Issue()[@state='open']/belongsTo::Repo()[@name='rug-cli']"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (2)
    parsed.locationSteps(1).axis match {
      case NavigationAxis("belongsTo") =>
    }
  }

  it should "parse a property name axis specifier using double quoted strings" in {
    val pe = """/Issue()[@state="open"]/belongsTo::Repo()[@name="rug-cli"]"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (2)
    parsed.locationSteps(1).axis match {
      case NavigationAxis("belongsTo") =>
    }
  }

  it should "parse predicate to understandable repo" in {
    val pe = """/Issue()[@state='open']/belongsTo::Repo()[@name='rug-cli']"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (2)
    parsed.locationSteps.head.predicates.head match {
      case PropertyValuePredicate("state", "open") =>
    }
  }

  it should "parse nested predicate" in {
    val pe = """/Issue()[@state='open'][/belongsTo::Repo()[@name='rug-cli']]"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    parsed.locationSteps.head.predicates(1) match {
      case _: NestedPathExpressionPredicate =>
    }
  }

  it should "parse an optional predicate" in {
    val pe = """/Push()[contains::Commit()]?"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    parsed.locationSteps.head.predicates.size should be (1)
    parsed.locationSteps.head.predicates.head match {
      case _: OptionalPredicate =>
    }
  }

  it should "parse required and optional predicates" in {
    val pe = """/Push()[@name='brainiac'][contains::Commit()]?"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    parsed.locationSteps.head.predicates.size should be (2)
    parsed.locationSteps.head.predicates.count {
      case _: OptionalPredicate => true
      case _ => false
    } should be (1)
  }

  it should "parse optional and required predicates" in {
    val pe = """/Push()[contains::Commit()]?[@name='brainiac']"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    parsed.locationSteps.head.predicates.size should be (2)
    parsed.locationSteps.head.predicates.count {
      case _: OptionalPredicate => true
      case _ => false
    } should be (1)
  }

  it should "parse a nested optional predicate" in {
    val pe = """/Push()[contains::Commit()[belongsTo::Repo()]?]"""
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (1)
    parsed.locationSteps.head.predicates.size should be (1)
    parsed.locationSteps.head.predicates.head match {
      case npep: NestedPathExpressionPredicate =>
        val npepSteps = npep.expression.locationSteps
        npepSteps.size should be (1)
        npepSteps.head.predicates.size should be (1)
        npepSteps.head.predicates.head match {
          case _: OptionalPredicate =>
        }
    }
  }

  it should "parse a complicate path expression" in {
    val pe = """/Build()
               |[on::Repo()/channel::ChatChannel()]
               |[triggeredBy::Push()/contains::Commit()/author::GitHubId()/hasGithubIdentity::Person()/hasChatIdentity::ChatId()]
               |/hasBuild::Commit()/author::GitHubId()/hasGithubIdentity::Person()/hasChatIdentity::ChatId()""".stripMargin
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (5)
    parsed.locationSteps.head.predicates.size should be (2)
    parsed.locationSteps.head.predicates.count {
      case _: NestedPathExpressionPredicate => true
      case _ => false
    } should be (2)
  }

  it should "parse a complicate path expression with optional predicates" in {
    val pe = """/Build()
               |[on::Repo()/channel::ChatChannel()]?
               |[triggeredBy::Push()/contains::Commit()/author::GitHubId()/hasGithubIdentity::Person()/hasChatIdentity::ChatId()]?
               |/hasBuild::Commit()/author::GitHubId()/hasGithubIdentity::Person()/hasChatIdentity::ChatId()""".stripMargin
    val parsed = pep.parsePathExpression(pe)
    parsed.locationSteps.size should be (5)
    parsed.locationSteps.head.predicates.size should be (2)
    parsed.locationSteps.head.predicates.count {
      case _: OptionalPredicate => true
      case _ => false
    } should be (2)
  }
}
