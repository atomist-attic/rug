package com.atomist.rug.parser

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.TestUtils._
import com.atomist.rug.{BadRugSyntaxException, DefaultRugPipeline, InvalidRugAnnotationValueException}
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.util.SaveAllDescendantsVisitor
import com.atomist.util.scalaparsing._
import org.scalatest.{FlatSpec, Matchers}

object CommonRugParserTest {

  val EqualsLiteralStringInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with File f
       | when name = "thing"
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLetStringInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |let checkFor = "thing"
       |
       |with File f
       | when name = checkFor
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLiteralStringInPredicatesWithParam: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |param what: ^.*$$
       |
       |with File f
       | when name = "thing"
       |
       |do
       | append what
      """.stripMargin

  val InvokeOtherOperationWithSingleParameter: String =
    s"""
       |editor Triplet
       |
       |@tag "java"
       |@tag "spring"
       |param javaThing: @java_package
       |
       |Foobar
      """.stripMargin

  val WellKnownRegexInParameter: String =
    s"""
       |editor Triplet
       |
       |param javaThing: @java_identifier
       |
       |Foobar
      """.stripMargin

  val EqualsJavaScriptBlockInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with File f
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
        |with File f when name = "foooo" do setPath "doesn't matter"
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
        |with Project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val rp = ri.parse(prog).head
    assert(rp.runs.size === 1)
    assert(rp.runs.head.args.head.parameterName === Some("num"))
  }

  it should "allow alias to be omitted and default correctly" in {
    val prog =
      """
        |editor Caspar
        |
        |with Project
        |do
        |  replace "Dog" "Cat"
      """.stripMargin
    val rp = ri.parse(prog).head
    assert(rp.withs.size === 1)
    assert(rp.withs.head.alias === "Project")
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
    assert(rp.withs.size === 1)
    assert(rp.withs.head.alias === "project")
  }

  it should "allow arguments to be specified in JavaScript" in {
    val prog =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param replacementText: ^.*$
        |
        |with file f
        |  do regexpReplace { 'regex\s*with\s*"quotes"' } replacementText
      """.stripMargin
    val rp = ri.parse(prog).head
    assert(rp.actions.size === 1)
    val vis = new SaveAllDescendantsVisitor
    rp.accept(vis, 0)
    vis.descendants collect {
      case b: JavaScriptBlock => b.content === """regex\s*with\s*"quotes""""
    }
  }

  it should "parse a complicated regular expression" in {
    val prog =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param replacementText: ^.*$
        |
        |with file f
        |  do regexpReplace "regex\\s*with\\s*\"quotes[^\"]+\"" { "\"" + replacementText + "\"" }
      """.stripMargin
    val rp = ri.parse(prog).head
    assert(rp.actions.size === 1)
    val vis = new SaveAllDescendantsVisitor
    rp.accept(vis, 0)
    vis.descendants collect {
      case l: SimpleLiteral[String @unchecked] => l.value === """regex\s*with\s*"quotes[^"]+""""
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
        |with File f
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
        |with File f
        | when isJava = "thing"
        |
        |do
        | append "foobar"
      """.stripMargin
    )

  private  def parseLiteralStringInPredicates(prog: String) {
    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
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
        |with File f
        | when isJava = "thing" and someFunction
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
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
        |with Project p
        | when
        | fileCount = 1
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.predicate match {
      case AndExpression(a: EqualsExpression, b: AndExpression) =>
        a.b.isInstanceOf[Literal[Int@unchecked]] should be(true)
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
        |with Project p
        | when
        | fileCount = 1
        |  and fileHasContent "src/main/java/Cat.java" ("class Cat {}")
        |  and fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.predicate match {
      case AndExpression(a: EqualsExpression, b: AndExpression) =>
        a.b.isInstanceOf[Literal[Int@unchecked]] should be(true)
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
        |with Project p
        | when
        | fileCount = 1
        |begin
        |  do fileHasContent "src/main/java/Cat.java" (append "class Cat {}" "cat")
        |  do fileHasContent "src/main/java/Cat.java" "class Cat {}"
        |end
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.doSteps.head match {
      case fd: FunctionDoStep =>
        assert(fd.function === "fileHasContent")
        assert(fd.args.size === 2)
        fd.args(1) match {
          case WrappedFunctionArg(p: ParsedRegisteredFunctionPredicate, _) =>
            assert(p.args.size === 2)
          case _ => ???
        }
      case _ => ???
    }
  }

  it should "parse comparison with false" in {
    val prog =
      """
        |editor Triplet
        |
        |with Project p
        | when
        | contains = false
        |
        |do
        | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.predicate match {
      case EqualsExpression(a, b) =>
        b.isInstanceOf[Literal[Boolean@unchecked]] should be(true)
    }
  }

  it should "resolve well-known regex in parameter" in {
    val parsed = ri.parse(WellKnownRegexInParameter).head
    assert(parsed.parameters.size === 1)
    assert(parsed.parameters.head.getPattern === DefaultIdentifierResolver.knownIds("java_class"))
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
    assert(parsed.parameters.size === 1)
    assert(parsed.parameters.head.getPattern === DefaultIdentifierResolver.knownIds("java_class"))
  }

  it should "expose parameter tags" in {
    val prog = InvokeOtherOperationWithSingleParameter

    val parsed = ri.parse(prog).head
    assert(parsed.parameters.size === 1)
    parsed.parameters.head.getTags.map(t => t.name) should equal(List("java", "spring"))
  }

  it should "expose parameter min and max values" in {
    val name = "AnActualValidClassName"
    val prog =
      s"""
         |editor Triplet
         |
         |@minLength 6
         |@maxLength 32
         |@default "$name"
         |param java_thing: @java_class
         |
       |Foobar
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.parameters.size === 1)
    val p = parsed.parameters.head
    assert(p.getMinLength === 6)
    assert(p.getMaxLength === 32)
    assert(p.getDefaultValue === name)
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
    assert(parsed.runs.size === 1)
    val r = parsed.runs.head
    assert(r.name === "Foobar")
    assert(r.args === Seq(
      WrappedFunctionArg(SimpleLiteral("foo"), Some("argA")),
      WrappedFunctionArg(SimpleLiteral("bar"), Some("argB"))))
  }

  it should "reject bogus regex in parameter" in {
    val prog =
      s"""editor Triplet
         |
         |param nonsense: @this_is_bollocks
         |
         |run Foobar
      """.stripMargin

    an[BadRugSyntaxException] should be thrownBy ri.parse(prog).head
  }

  it should "parse = javascript block in predicate" in {
    val parsed = ri.parse(EqualsJavaScriptBlockInPredicate).head
    assert(parsed.withs.size === 1)
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
         |with File f
         | when isJava = otherFunction
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
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
         |with File f
         | when "other" = otherFunction
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.predicate match {
      case eq: EqualsExpression =>
      
      case _ => ???
    }
  }

  it should "parse run" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with File f
         | when isJava
         |do
         | append "foobar"
         |
         |EditorA
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.actions.size === 2)
    parsed.actions.head.isInstanceOf[With] should be(true)
    parsed.actions(1) match {
      case r: RunOtherOperation => assert(r.name === "EditorA")
      case _ => ???
    }
  }

  it should "not permit function names with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with File f
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
         |with File f
         | when isJava
         |
         |do
         | Append "foobar"
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy ri.parse(prog)
  }

  it should "not permit parameter names with upper case" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |param Bar: ^.*$$
         |
         |with File f
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
         |with File f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy (ri.parse(prog))
  }

  it should "parse geberator" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |generator Triplet
         |
         |with File f
         | when isJava = "thing"
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.publishedName === Some("Triplet"))
  }

  it should "pick up multiple @tags annotations on operation" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |@tag "Foo"
         |@tag "Bar"
         |generator Triplet
         |
         |with File f
         | when isJava = "thing"
         |
         |do
         | append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.tags === Seq("Foo", "Bar"))
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
         |with File f
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
        |with File f
        | when isJava = "thing" and someFunction
        |
        |do
        | f.append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.doSteps.head match {
      case dds: FunctionDoStep =>
        assert(dds.function === "append")
        assert(dds.target === Some("f"))
      case _ => ???
    }
  }

  it should "accept nested . notation in from" in {
    val prog =
      """
        |editor Triplet
        |
        |with File f when f.name.length = 32
        |  do f.append "foobar"
      """.stripMargin

    val parsed = ri.parse(prog).head
    assert(parsed.withs.size === 1)
    parsed.withs.head.predicate match {
      case cp: ComparisonPredicate =>
        cp.a match {
          case p: ParsedRegisteredFunctionPredicate =>
            assert(p.function === "name")
            assert(p.target === Some("f"))
            assert(p.pathBelow === Seq("length"))
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
        |with Project
        |  do addFile name=".gitignore" content="elm-stuff\ntarget"
      """.stripMargin
    val p = ri.parse(prog).head
    val comp = p.computations.head
    comp.te match {
      case sl: SimpleLiteral[String@unchecked] =>
        assert(sl.value === "elm-stuff\ntarget")
    }
  }

  it should "in a static content string, treat \\n as newline in a file" in {
    val prog =
      """
        |editor AddGitIgnore
        |
        |let foo = "elm-stuff\ntarget"
        |
        |with Project
        |  do addFile name=".gitignore" content="elm-stuff\ntarget"
      """.stripMargin
    updateWith(prog)
  }

  it should "parse Python style comment lines" in {
    val prog =
      """
        |# my comment
        |editor PythonCommentsAreNice
        |
        |with File f
        | when name = "thing" # oh look another comment
        |
        |do
        | append "foobar"
      """.stripMargin
    ri.parse(prog)
  }

  private  def updateWith(prog: String): Unit = {
    val filename = "test.txt"
    val as = new SimpleFileBasedArtifactSource("name", Seq(StringFileArtifact(filename, "some content")))
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    val r = doModification(pas, as, EmptyArtifactSource(""), SimpleParameterValues.Empty)
    val f = r.findFile(".gitignore").get
    f.content.contains(s"elm-stuff${System.lineSeparator()}target") should be(true)
  }
}
