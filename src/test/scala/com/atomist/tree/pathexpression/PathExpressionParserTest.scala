package com.atomist.tree.pathexpression

import com.atomist.rug.BadRugSyntaxException
import org.scalatest.{FlatSpec, Matchers}

class PathExpressionParserTest extends FlatSpec with Matchers {

  val pep = PathExpressionParser

  it should "parse a bare root node" in {
    val pe = "/"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.isEmpty should be(true)
  }

  it should "failed to parse an unanchored path expression" in {
    val pe = "big/lebowski"
    an[BadRugSyntaxException] should be thrownBy(
      pep.parsePathExpression(pe)
      )
  }

  it should "parse a child axis" in {
    val pe = "/child::src"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    ls.axis should be (Child)
    ls.predicate should be (None)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse an abbreviated child axis with node name" in {
    val pe = "/src"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    ls.axis should be(Child)
    ls.predicate should be(None)
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
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    parsed1.elements.size should be(1)
    val ls1 = parsed.elements.head
    ls1.axis should be(ls.axis)
    ls1.predicate should be(ls.predicate)
    ls1.test should be(ls.test)
  }

  it should "parse a descendant axis" in {
    val pe = "/descendant::src"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    ls.axis should be (Descendant)
    ls.predicate should be (None)
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("src")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

  it should "parse an abbreviated descendant axis with node name" in {
    val pe = "//src"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    ls.axis should be(Descendant)
    ls.predicate should be(None)
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
    parsed.elements.size should be(1)
    val ls = parsed.elements.head
    parsed1.elements.size should be(1)
    val ls1 = parsed.elements.head
    ls1.axis should be(ls.axis)
    ls1.predicate should be(ls.predicate)
    ls1.test should be(ls.test)
  }

  it should "parse a node object type" in {
    val pe = "/Issue()"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be (1)
    val ls = parsed.elements.head
    ls.axis should be(Child)
    ls.predicate should be(None)
    ls.test should be(ObjectType("Issue"))
  }

  it should "parse an index predicate" in {
    val pe = "/dude[4]"
    val parsed = pep.parsePathExpression(pe)
    parsed.elements.size should be (1)
    val ls = parsed.elements.head
    ls.axis should be(Child)
    ls.predicate match {
      case Some(p@IndexPredicate("[4]", 4)) =>
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
    parsed.elements.size should be (1)
    val ls = parsed.elements.head
    ls.axis should be(Child)
    ls.predicate match {
      case Some(p@SimplePredicate("size=large", _)) =>
      case x => fail(s"predicate did not match expected type: $x")
    }
    ls.test match {
      case nnt: NamedNodeTest => nnt.name should be ("dude")
      case x => fail(s"node test is not a NamedNodeTest: $x")
    }
  }

//  it should "parse expression with type jump" in {
//    val pe = "/issue/test1:repo/project/src/main/java//*:file->java.class"
//    val parsed = pep.parsePathExpression(pe)
//  }
}
