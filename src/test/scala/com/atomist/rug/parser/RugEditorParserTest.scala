package com.atomist.rug.parser

import com.atomist.util.scalaparsing.{JavaScriptBlock, Literal}
import com.atomist.rug._
import org.scalatest.{FlatSpec, Matchers}

class RugEditorParserTest extends FlatSpec with Matchers {

  val ri = new ParserCombinatorRugParser

  it should "parse simplest program" in
    simplestProgram("'Description'")

  it should "handle double quoted string description" in {
    simplestProgram(""" "This is my description" """)
  }

  it should "handle multiline string description" in {
    simplestProgram(" \"\"\"description\"\"\" ")
  }

  private def simplestProgram(description: String = ""): RugProgram = {
    val prog =
      s"""
         |@description $description
         |editor RemoveEJB
         |
         |with file f
         | when isJava;
         |
         |do
         | append "a" "26";
    """.stripMargin
    ri.parse(prog).head
  }

  it should "handle JavaScript expression in file filter" in {
    val prog =
      """
        |@description 'JavaScript lives here'
        |editor RemoveEJB
        |
        |with file f
        | when { f.name.endsWith(".java") };
        |
        |do
        | append "a" "26";
      """.stripMargin

    val p = ri.parse(prog).head
    p.withs.head.predicate match {
      case f: ParsedJavaScriptFunction =>
    }
    p.parameters should equal(Nil)
  }

  it should "parse editor precondition" in {
    val prog =
      """
        |editor RemoveEJB
        |
        |precondition Foo
        |
        |with file f
        | when { f.name.endsWith(".java") };
        |
        |do
        | append "a" "26";
      """.stripMargin

    val p = ri.parse(prog).head match {
      case re: RugEditor =>
        re.preconditions.head should equal(Condition("Foo"))
    }
  }

  it should "parse editor postcondition" in {
    val prog =
      """
        |editor RemoveEJB
        |
        |precondition Foo
        |postcondition Bar
        |
        |with file f
        | when { f.name.endsWith(".java") };
        |
        |do
        | append "a" "26";
      """.stripMargin

    val p = ri.parse(prog).head match {
      case re: RugEditor =>
        re.preconditions.headOption should equal(Some(Condition("Foo")))
        re.postcondition should equal(Some(Condition("Bar")))
    }
  }

  it should "parse triple quoted string" in {
    val tripleStringContent = "Careful, there's a beverage here"
    val tripleString = """"""""" + tripleStringContent + """""""""
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |with file f
         | when isJava;
         |
         |do
         | append $tripleString;
      """.stripMargin

    val actions = ri.parse(prog).head

    actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation].args.size should be(1)
    actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation].args.head match {
      case s: WrappedFunctionArg => s.te.asInstanceOf[Literal[String]].value should equal(tripleStringContent)
    }
  }

  it should "parse single line script" in {
    val prog =
      """editor AddFileAndContent param filename: "^.*$" param content: "^.*$" with project p do addFile filename content """.stripMargin
    ri.parse(prog)
  }

  it should "handle single parameter declaration" in {
    val prog =
      """
        |@description 'No JS'
        |editor RemoveEJB
        |
        |param name: ^...$
        |
        |with file f
        | when isJava;
        |
        |do
        | append "a" "26";
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.parameters.size should be(1)
    val p = actions.parameters.head
    p.getName should be("name")
    p.getPattern should be("^...$")
    p.isRequired should be(true)
    p.isDisplayable should be(true)
  }

  it should "handle single parameter declaration with description and default value" in {
    val paramDesc = "description of parameter"
    val defaultVal = "defaultValue"
    val validInput = "Valid GitHub project name"
    val displayName = "Project Name"
    val prog =
      s"""
         |@description "No EJB"
         |editor RemoveEJB
         |
         |@default '$defaultVal'
         |@description '$paramDesc'
         |@validInput '$validInput'
         |@displayName '$displayName'
         |@hide
         |param name: ^.*$$
         |
         |with file f
         | when isJava;
         |
         |do
         | append "a" "26";
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.parameters.size should be(1)
    val p = actions.parameters.head
    p.getName should be("name")
    p.getPattern should be("^.*$")
    p.getDescription should be(paramDesc)
    p.getDefaultValue should be(defaultVal)
    p.getValidInputDescription should be(validInput)
    p.isDisplayable should be(false)
    p.isRequired should be(false)
    p.getDisplayName should be(displayName)
  }

  it should "handle single parameter declaration and use in do step" in {
    val prog =
      """
        |@description "Death to EJBs"
        |editor RemoveEJB
        |
        |param name: ^.*$
        |
        |with file f
        | when isJava;
        |
        |do
        | append "a" name;
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.parameters.size should be(1)
    val p = actions.parameters.head
    p.getName should be("name")
    p.getPattern should be("^.*$")
    p.isRequired should be(true)
  }

  it should "handle single optional parameter declaration" in
    simpleProgram(Nil)

  it should "handle int do arg" in {
    val actions = simpleProgram(Seq("1"))
    val step = actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation]
    step.args.size should equal(1)
    step.args.head.isInstanceOf[WrappedFunctionArg] should be(true)
  }

  it should "handle double do arg" in pendingUntilFixed {
    val actions = simpleProgram(Seq("1.9"))
    val step = actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation]
    step.args.size should equal(1)
    step.args.head.isInstanceOf[WrappedFunctionArg] should be(true)
  }

  it should "handle String literal do arg" in {
    val actions = simpleProgram(Seq("\"whatev\""))
    val step = actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation]
    step.args.size should equal(1)
    step.args.head match {
      case l: WrappedFunctionArg => l.te.asInstanceOf[Literal[String]].value.shouldEqual("whatev")
    }
  }

  it should "handle JavaScript do arg" in {
    val actions = simpleProgram(Seq("{ 42 + 63 }"))
    val step = actions.withs.head.doSteps.head.asInstanceOf[FunctionInvocation]
    step.args.size should equal(1)
    step.args.head match {
      case WrappedFunctionArg(te: JavaScriptBlock, _) =>
    }
  }

  private def simpleProgram(doArgs: Seq[String]): RugProgram = {
    val prog =
      s"""
         |@description "Something"
         |editor RemoveEJB
         |
         |@optional param name: ^.*$$
         |
         |with file f
         | when isJava
         |
         |do
         | append ${doArgs.mkString(" ")};
      """.stripMargin

    val progs = ri.parse(prog)
    progs.size should be(1)
    val actions = progs.head
    actions.parameters.size should be(1)
    val p = actions.parameters.head
    p.getName should be("name")
    p.getPattern should be("^.*$")
    p.isRequired should be(false)
    actions
  }

  it should "compute parameter from string literal" in {
    val p = computeProgram(Seq("a = \"x\""))
    p.computations.size should be(1)
  }

  it should "compute parameter from simple JavaScript expression" in {
    val p = computeProgram(Seq("a = { 42; }"))
    p.computations.size should be(1)
  }

  it should "compute parameter from complex JavaScript expression" in {
    val p = computeProgram(Seq(
      """
        |a =
        |{
        | // println("This is a test");
        | if (true) { }
        |}
      """.stripMargin))
    p.computations.size should be(1)
    p.computations.head
  }

  private def computeProgram(infers: Seq[String]): RugProgram = {
    val prog =
      s"""
         |@description 'Demonstrate computation'
         |editor RemoveEJB
         |
         |@optional param name: ^.*$$
         |
         |let ${infers.mkString("\n")}
         |
         |with file f
         | when isJava;
         |
         |do
         | append something;
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.parameters.size should be(1)
    val p = actions.parameters.head
    actions
  }

  it should "parse file block without predicate" in {
    val prog =
      """
        |@description 'demonstrate AND'
        |editor RemoveEJB
        |
        |with file f
        |do
        | append "\na"
      """.stripMargin

    val actions = ri.parse(prog)
    // actions.parameters should equal (Nil)
  }

  it should "parse file AND block" in {
    val prog =
      """
        |@description 'demonstrate AND'
        |editor RemoveEJB
        |
        |with file f
        | when isJava and isLong
        |
        |do
        | append "\na"
      """.stripMargin

    val actions = ri.parse(prog)
    // actions.parameters should equal (Nil)
  }

  it should "parse file OR block" in {
    val prog =
      """
        |@description 'demonstrate OR'
        |editor RemoveEJB
        |
        |with file f
        | when isJava or isLong;
        |
        |do
        | append "A";
      """.stripMargin

    val actions = ri.parse(prog)
    // TODO verify
    // actions.parameters should equal (Nil)
  }

  it should "parse compound do block" in {
    val prog =
      """
        |@description 'demonstrate OR'
        |editor RemoveEJB
        |
        |with file f
        | when isJava or isLong;
        |
        |begin
        | do append "A";
        | do append "B";
        |end
      """.stripMargin

    val p = ri.parse(prog).head
    p.actions.size should be(1)
    p.withs.head.doSteps.size should be(2)
  }

  it should "handle multiple with blocks" in {
    val prog =
      """
        |@description 'demonstrate OR'
        |editor RemoveEJB
        |
        |with file f
        | when isJava or isLong
        |do
        | append "A";
        |
        |with file f
        | when isPom
        |do
        |  append "A"
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.actions.size should equal(2)
  }

  it should "handle multiple with blocks 2" in {
    val prog =
      """
        |@description "Renames an Elm module"
        |editor Renamer
        |
        |@description "Name of module we're renaming"
        |param old_name: ^[A-Z][\w]+$
        |
        |@description "New name for the module"
        |param new_name: ^[A-Z][\w]+$
        |
        |with elmModule e
        |when { e.name() == old_name}
        |do
        |  setName new_name
        |
        |with elmModule e
        |   when imports old_name
        |do
        |   updateImport old_name new_name
      """.stripMargin
    val rug = ri.parse(prog).head
    rug.actions.size should equal(2)
    rug.withs(1).predicate match {
      case rp: ParsedRegisteredFunctionPredicate =>
        rp.args.size should equal(1)
        rp.args should equal(Seq(IdentifierFunctionArg("old_name")))
    }
  }

  it should "handle nested with block" in {
    val prog =
      """
        |@description 'demonstrate OR'
        |editor RemoveEJB
        |
        |with file f
        | when isJava or isLong
        |
        |with line l
        |  when {true}
        |do
        |  append "A"
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.actions.size should equal(1)
    actions.withs.head.doSteps.size should be(1)
    actions.withs.head.doSteps.head.asInstanceOf[WithDoStep].wth.doSteps.size should be(1)
  }

  it should "handle nested with blocks" in {
    val prog =
      """
        |@description 'demonstrate OR'
        |editor RemoveEJB
        |
        |with file f
        | when isJava or isLong;
        |
        |with line l
        |  when {true};
        |with word w when {true};
        |begin
        | do append "A"
        | do append "B"
        |end
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.actions.size should equal(1)
    actions.withs.head.doSteps.head.asInstanceOf[WithDoStep].wth.doSteps.size should be(1)
    actions.withs.head.doSteps.head.asInstanceOf[WithDoStep].wth.doSteps.head.asInstanceOf[WithDoStep].wth.doSteps.size should be(2)
  }

  it should "find description" in {
    val desc = "demonstrate description"
    val prog =
      s"""
         |@description '$desc'
         |editor RemoveEJB
         |
         |with file f
         | when isJava or isLong;
         |
         |do
         | append "A";
      """.stripMargin
    val actions = ri.parse(prog).head
    actions.description should equal(desc)
  }

  it should "find match alias and kind" in {
    val desc = "demonstrate description"
    val prog =
      s"""
         |@description '$desc'
         |editor RemoveEJB
         |
         | with file f
         |   when isJava or isLong;
         |do
         | append "A";
         |
         | with thing t
         |  when isJava or isLong;
         |do
         | append "A";
      """.stripMargin

    val actions = ri.parse(prog).head
    actions.withs.size should be(2)
    actions.withs.head.alias should be("f")
    actions.withs.head.kind should be("file")
    actions.withs(1).alias should be("t")
    actions.withs(1).kind should be("thing")
  }

  it should "accept comments" in {
    val prog =
      """
        |# This is a comment
        |@description "Thing"
        |editor RemoveEJB
        |
        |# another comment
        |with file f
        |do
        |    # We like comments!
        |    append "x"
      """.stripMargin

    val p = ri.parse(prog)
  }

  it should "parse multiple programs" in {
    val prog =
      """
        |@description 'JavaScript lives here'
        |editor RemoveEJB
        |
        |with file f
        |do
        | append "a" "26";
        |
        |editor Caspar
        |
        |with project p
        |do
        |fail "Shut up, Donny"
      """.stripMargin

    val progs = ri.parse(prog)
    progs.head.name should be("RemoveEJB")
    progs.size should be(2)
    progs(1).name should be("Caspar")
  }

  it should "fail invalid input after a valid editor" in {
    val prog =
      """
        |@description 'JavaScript lives here'
        |editor RemoveEJB
        |
        |with file f;
        |do
        | append "a" "26";
        |
        |editor Caspar
        |
        |this is screwed up
      """.stripMargin
    an[BadRugException] should be thrownBy (ri.parse(prog))
  }

  it should "insist editor has name starting with upper case" in {
    val invalidEditorName = "invalidEditorName"
    val prog =
      s"""
         |@description 'JavaScript lives here'
         |editor $invalidEditorName
         |
         |with file f
         |do
         | append "a" "26"
         |
         |editor Caspar
         |
         |this is screwed up
      """.stripMargin
    try {
      ri.parse(prog)
      fail(s"Invalid editor name '$invalidEditorName' should have been rejected")
    }
    catch {
      case micturation: BadRugSyntaxException =>
    }
  }

  it should "insist whole program is understood" in {
    val prog =
      """
        |@description 'JavaScript lives here'
        |editor Foobar
        |
        |with file f
        |do
        | append "a" "26"
        |
        |what in god's holy name are you blathering about?
      """.stripMargin
    an[BadRugSyntaxException] should be thrownBy ri.parse(prog)
  }
}
