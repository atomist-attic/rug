package com.atomist.rug.parser

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.TestUtils._
import com.atomist.rug.{BadRugSyntaxException, InvalidRugAnnotationValueException}
import com.atomist.util.scalaparsing._
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object CommonRugParserTest {

  val EqualsLiteralStringInPredicate =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with file f
       | when name = "thing"
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLetStringInPredicate =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |let checkFor = "thing"
       |
       |with file f
       | when name = checkFor
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLiteralStringInPredicatesWithParam =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |param what: ^.*$$
       |
       |with file f
       | when name = "thing"
       |
       |do
       | append what
      """.stripMargin

  val ParamWithMinAndMax =
    s"""
       |editor Triplet
       |
       | @minLength 6
       | @maxLength 32
       | @default "com.foo"
       |param javaThing: @java_class
       |
       |Foobar
      """.stripMargin

  val InvokeOtherOperationWithSingleParameter =
    s"""
       |editor Triplet
       |
       |@tag "java"
       |@tag "spring"
       |param javaThing: @java_class
       |
       |Foobar
      """.stripMargin

  val WellKnownRegexInParameter =
    s"""
       |editor Triplet
       |
       |param javaThing: @java_class
       |
       |Foobar
      """.stripMargin

  val EqualsJavaScriptBlockInPredicate =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with file f
       | when isJava = { "thing" }
       |
       |do
       | append "foobar"
      """.stripMargin

}

import com.atomist.rug.parser.CommonRugParserTest._

/**
  * Tests common to editors and reviewers
  */
class CommonRugParserTest extends FlatSpec with Matchers {

  val ri = new ParserCombinatorRugParser

  it should "parse = literal string in predicates" in
    parseLiteralStringInPredicates(EqualsLiteralStringInPredicate)

  it should "allow other editor definition after run other operation" in pendingUntilFixed {
    val prog =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |let x = 2
        |
        |Other num = x
        |
        |
        |editor Other
        |
        |param num: ^\d+$
        |
        |with file f when name = "foooo" do setPath "doesn't matter"
      """.stripMargin
    ri.parse(prog)
  }

  it should "allow single named parameter to other operation" in {
    val prog =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |let num = 2
        |
        |Other num = num
        |
        |@description "This is a second editor"
        |editor Other
        |
        |param num: ^\d+$
        |
        |with project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val rp = ri.parse(prog).head
    rp.runs.size should be(1)
    rp.runs.head.args.head.parameterName should equal(Some("num"))
  }

  it should "allow alias to be omitted and default correctly" in {
    val prog =
      """
        |editor Caspar
        |
        |with project
        |do
        |  replace "Dog" "Cat"
      """.stripMargin
    val rp = ri.parse(prog).head
    rp.withs.size should be(1)
    rp.withs.head.alias should be("project")
  }

  it should "allow alias to dotted type to be omitted and default correctly" in {
    val prog =
      """
        |editor Caspar
        |
        |with java.project
        |do
        |  replace "Dog" "Cat"
      """.stripMargin
    val rp = ri.parse(prog).head
    rp.withs.size should be(1)
    rp.withs.head.alias should be("project")
  }

  it should "allow actions to be specified in JavaScript" in {
    val prog =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param num: ^\d+$
        |
        |{
        | print(project)
        |}
      """.stripMargin
    val rp = ri.parse(prog).head
    rp.actions.size should be(1)
    rp.actions.head match {
      case ScriptBlockAction(c: JavaScriptBlock) =>
    }
  }

  // Seems to keep \\ escaping
  it should "verify output of backslash escaped strings in Java matches" is pending

  it should "ignore block comments with alpha characters" in
    parseLiteralStringInPredicates(
      """
        |/*
        |  This is a header
        |*/
        |
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |/*
        |  This is logic that
        |  deserves a multiline comment with only
        |  text in it
        |*/
        |
        |with file f
        | when isJava = "thing"
        |
        |do
        | append "foobar"
      """.stripMargin
    )

  it should "ignore block comments with range of characters" in
    parseLiteralStringInPredicates(
      """
        |/*
        |  This is a header
        |*/
        |
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |/*
        |  This is immensely complicated logic that
        |  deserves a multiline comment with only
        |  not only text but lots of <b>examples</b> in it that
        |  look ***** // like all *kinds of \\ &&^&^%&$%$%& things
        |*/
        |
        |with file f
        | when isJava = "thing"
        |
        |do
        | append "foobar"
      """.stripMargin
    )

  private def parseLiteralStringInPredicates(prog: String) {
    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case eq: EqualsExpression =>
    }
  }

  it should "parse equals and functions in predicate" in {
    val prog =
      """
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |with file f
        | when isJava = "thing" and someFunction
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case AndExpression(a: EqualsExpression, b: ParsedRegisteredFunctionPredicate) =>
    }
  }

  it should "parse equals int and functions with arguments in predicate" in {
    val prog =
      """
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |with project p
        | when
        | fileCount = 1
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case AndExpression(a: EqualsExpression, b: AndExpression) =>
        a.b.isInstanceOf[Literal[Int]] should be(true)
        b.a.isInstanceOf[ParsedRegisteredFunctionPredicate] should be(true)
        b.b.isInstanceOf[ParsedRegisteredFunctionPredicate] should be(true)
    }
  }

  it should "permit use of parentheses around simple expression" in {
    val prog =
      """
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |with project p
        | when
        | fileCount = 1
        |  and fileHasContent "src/main/java/Cat.java" ("class Cat {}")
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case AndExpression(a: EqualsExpression, b: AndExpression) =>
        a.b.isInstanceOf[Literal[Int]] should be(true)
        b.a.isInstanceOf[ParsedRegisteredFunctionPredicate] should be(true)
        b.b.isInstanceOf[ParsedRegisteredFunctionPredicate] should be(true)
    }
  }

  it should "permit use of parentheses around function call" in {
    val prog =
      """
        |@description '100% JavaScript free'
        |editor Triplet
        |
        |with project p
        | when
        | fileCount = 1
        |begin
        |  do fileHasContent "src/main/java/Cat.java" (append "class Cat {}" "cat")
        |  do fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |end
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.doSteps.head match {
      case fd: FunctionDoStep =>
        fd.function should equal("fileHasContent")
        fd.args.size should be(2)
        fd.args(1) match {
          case WrappedFunctionArg(p: ParsedRegisteredFunctionPredicate, _) =>
            p.args.size should be(2)
        }
    }
  }

  it should "parse comparison with false" in {
    val prog =
      """
        |editor Triplet
        |
        |with project p
        | when
        | contains = false
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case EqualsExpression(a, b) =>
        b.isInstanceOf[Literal[Boolean]] should be(true)
    }
  }

  it should "resolve well-known regex in parameter" in {
    val parsed = ri.parse(WellKnownRegexInParameter).head
    parsed.parameters.size should be(1)
    parsed.parameters.head.getPattern should be(DefaultIdentifierResolver.knownIds("java_class"))
  }

  it should "resolve well-known regex in parameter with comment on same line" in {
    val prog =
      s"""
         |editor Triplet
         |
         |param javaThing: @java_class # and this should be ignored
         |
         |Foobar
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.parameters.size should be(1)
    parsed.parameters.head.getPattern should be(DefaultIdentifierResolver.knownIds("java_class"))
  }

  it should "expose parameter tags" in {
    val prog = InvokeOtherOperationWithSingleParameter

    val parsed = ri.parse(prog).head
    parsed.parameters.size should be(1)
    parsed.parameters.head.getTags.map(t => t.name) should equal(List("java", "spring"))
  }

  it should "expose parameter min and max values" in {
    val prog = ParamWithMinAndMax

    val parsed = ri.parse(prog).head
    parsed.parameters.size should be(1)
    parsed.parameters.head.getMinLength should be(6)
    parsed.parameters.head.getMaxLength should be(32)
    parsed.parameters.head.getDefaultValue should equal("com.foo")
  }

  it should "expose parameter bounds" in {
    val prog =
      s"""
         |editor Triplet
         |
         |param javaThing: [ "yes","no" ]
         |
         |Foobar
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.parameters.size should be(1)
    parsed.parameters.head.getAllowedValues.map(_.name).toList should equal(Seq("yes", "no"))
  }

  it should "accept local arguments for run operation" in {
    val prog =
      s"""
         |editor Triplet
         |
         |param javaThing: @java_class
         |
         |Foobar argA = "foo", argB = "bar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.runs.size should equal(1)
    val r = parsed.runs.head
    r.name should equal("Foobar")
    r.args should equal(Seq(
      WrappedFunctionArg(SimpleLiteral("foo"), Some("argA")),
      WrappedFunctionArg(SimpleLiteral("bar"), Some("argB")))
    )
  }

  it should "reject bogus regex in parameter" in {
    val prog =
      s"""
         |editor Triplet
         |
         |param nonsense: @this_is_bollocks
         |
         |run Foobar
      """.stripMargin

    an[BadRugSyntaxException] should be thrownBy ri.parse(prog).head
  }

  it should "parse = javascript block in predicate" in {
    val parsed = ri.parse(EqualsJavaScriptBlockInPredicate).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case eq: EqualsExpression =>
    }
  }

  it should "parse = function in predicates" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava = otherFunction
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case eq: EqualsExpression =>
    }
  }

  it should "parse literal n left with = in predicates" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when "other" = otherFunction
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case eq: EqualsExpression =>
    }
  }

  it should "parse run" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava
         |do
         | append "foobar"
         |
         |EditorA
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.actions.size should be(2)
    parsed.actions.head.isInstanceOf[With] should be(true)
    parsed.actions(1) match {
      case r: RunOtherOperation => r.name should equal("EditorA")
    }
  }

  it should "not permit function names with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when IsJava
         |
         |do
         | append "foobar"
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy (ri.parse(prog))
  }

  it should "not permit function names in do block with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava
         |
         |do
         | Append "foobar"
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy (ri.parse(prog))
  }

  it should "not permit parameter names with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |param Bar: ^.*$$
         |
         |with file f
         | when isJava;
         |
         |do
         | append "foobar";
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy (ri.parse(prog))
  }

  it should "not permit computed names with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |compute
         |  Bar = "bad identifier name"
         |
         |with file f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy (ri.parse(prog))
  }

  it should "parse @generator annotation on operation without name override" in {
    val prog =
      s"""
         |@generator
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava = "thing"
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.publishedName should be(Some("Triplet"))
  }

  it should "parse @generator annotation on operation with name override" in {
    val publishedName = "FooBar666"
    val prog =
      s"""
         |@generator "$publishedName"
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava = "thing"
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.publishedName should be(Some(publishedName))
  }

  it should "pick up multiple @tags annotations on operation with name override" in {
    val publishedName = "FooBar666"
    val prog =
      s"""
         |@generator "$publishedName"
         |@description '100% JavaScript free'
         |@tag "Foo"
         |@tag "Bar"
         |editor Triplet
         |
         |with file f
         | when isJava = "thing"
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.publishedName should be(Some(publishedName))
    parsed.tags should equal(Seq("Foo", "Bar"))
  }

  it should "reject @publish annotation on parameters" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |@generator
         |param foo: ^.*$$
         |
         |with file f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    an[InvalidRugAnnotationValueException] should be thrownBy (ri.parse(prog))
  }

  it should "parse . notation in do" in {
    val prog =
      """
        |editor Triplet
        |
        |with file f
        | when isJava = "thing" and someFunction
        |
        |do
        | f.append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.doSteps.head match {
      case dds: FunctionDoStep =>
        dds.function should equal("append")
        dds.target should equal(Some("f"))
    }
  }

  it should "accept nested . notation in from" in {
    val prog =
      """
        |editor Triplet
        |
        |with file f when f.name.length = 32
        |  do f.append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    parsed.withs.size should be(1)
    parsed.withs.head.predicate match {
      case cp: ComparisonPredicate =>
        cp.a match {
          case p: ParsedRegisteredFunctionPredicate =>
            p.function should equal("name")
            p.target should equal(Some("f"))
            p.pathBelow should equal(Seq("length"))
        }
    }
  }

  it should "in a static content string, treat \\n as newline" in pendingUntilFixed {
    val prog =
      """
        |editor AddGitIgnore
        |
        |let foo = "elm-stuff\ntarget"
        |
        |with project
        |  do addFile name=".gitignore" content="elm-stuff\ntarget"
      """.stripMargin
    val p = ri.parse(prog).head
    val comp = p.computations.head
    comp.te match {
      case sl: SimpleLiteral[String] =>
        sl.value should equal("elm-stuff\ntarget")
    }
  }

  it should "in a static content string, treat \\n as newline in a file" in {
    val prog =
      """
        |editor AddGitIgnore
        |
        |let foo = "elm-stuff\ntarget"
        |
        |with project
        |  do addFile name=".gitignore" content="elm-stuff\ntarget"
      """.stripMargin
    updateWith(prog)
  }

  private def updateWith(prog: String): Unit = {
    val filename = "test.txt"
    val as = new SimpleFileBasedArtifactSource("name", Seq(StringFileArtifact(filename, "some content")))
    val r = doModification(prog, as, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Seq.empty))
    val f = r.findFile(".gitignore").get
    f.content.contains("elm-stuff\ntarget") should be(true)
  }
}
