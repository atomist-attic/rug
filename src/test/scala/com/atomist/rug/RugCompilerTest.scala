package com.atomist.rug

import com.atomist.param.{Parameter, SimpleParameterValues}
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileMutableView
import com.atomist.rug.parser._
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, LambdaPredicate, RugDrivenProjectEditor}
import com.atomist.source._
import com.atomist.util.scalaparsing.SimpleLiteral
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

object RugCompilerTest extends LazyLogging {

  val JavaAndText: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("pom.xml", "<maven></maven"),
      StringFileArtifact("message.txt", "// I'm talkin' about ethics"),
      StringFileArtifact("/src/main/java/Dog.java",
        """class Dog {}""".stripMargin)
    )
  )

  def show(as: ArtifactSource): Unit = {
    as.allFiles.foreach(f => {
      logger.debug(f.path + "\n" + f.content + "\n\n")
    })
  }
}

class RugCompilerTest extends FlatSpec with Matchers {

  val fr = FixedRugDslFunctionRegistry()

  val compiler: RugCompiler = new DefaultRugCompiler(
    new DefaultEvaluator(fr),
    DefaultTypeRegistry)

  it should "demand referenced kinds exist" in {
    val bogusType = "what_in_gods_holy_name_are_you_blathering_about"

    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("name")
    val program = RugEditor("foobar", None, Nil, "foo bar thingie", Nil, Nil, None, Nil, Nil,
      Seq(With(bogusType, "f", None, fileExpression, Seq(doStep))))

    try {
      compiler.compile(program)
      fail(s"Should have rejected bogus type '$bogusType'")
    }
    catch {
      case ut: UndefinedRugTypeException =>
        ut.getMessage.contains(bogusType) should be(true)
      case _ : Throwable => ???
    }
  }

  it should "demand functions exist on kind" in {
    val absquatulate = "absquatulate"
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep(absquatulate)
    val program = RugEditor("foobar", None, Nil, "foo bar thingie", Nil, Nil, None, Nil, Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))
    try {
      compiler.compile(program)
      fail(s"Should have rejected unknown function '$absquatulate' on file f")
    }
    catch {
      case ut: UndefinedRugFunctionsException =>
        ut.getMessage.contains(absquatulate) should be(true)
        ut.getMessage.contains("File") should be(true)
      case _: Throwable => ???
    }
  }

  it should "demand do step functions exist in simplest program" in {
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("absquatulate")
    val program = RugEditor("foobar", None, Nil, "foo bar", Nil, Nil, None, Nil, Nil,
      Seq(With("file", "f", None, fileExpression, Seq(doStep))))

    an[BadRugException] should be thrownBy (
      compiler compile program
      )
  }

  it should "insist on identifiers being declared" in {
    FixedRugDslFunctionRegistry(
      Map(
        "absquatulate" -> new LambdaPredicate[FileMutableView]("absquatulate", f => true)
      
      )
    )

    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("absquatulate", None, Seq(IdentifierFunctionArg("missing")))

    val program = RugEditor("foobar", None, Nil, "foo bar", Nil, Nil, None, Nil, Nil,
      Seq(With("file", "f", None, fileExpression, Seq(doStep))))

    an[BadRugException] should be thrownBy (
      compiler compile program
      )
  }

  it should "recognize identifiers used have been declared" in {
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("name", None, Seq(IdentifierFunctionArg("p1")))
    val program = RugEditor("foobar", None, Nil, "Thingie", Nil, Nil, None, Seq(new Parameter("p1")), Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))
    compiler compile program
  }

  it should "return verify name" in {
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("name")
    val program = RugEditor("foobar", None, Nil, "something goes here", Nil, Nil, None, Nil, Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))
    val pe = compiler.compile(program)
    assert(pe.name === program.name)
  }

  it should "expose correct parameters" in {
    val p1 = new Parameter("something")
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("path")
    val program = RugEditor("foobar", None, Nil, "Something goes here", Nil, Nil, None, Seq(p1), Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))

    val pe = compiler compile program
    assert(pe.parameters.size === 1)
    assert(pe.parameters.head === p1)
  }

  it should "use supplied editor name" in {
    val name = "someName"
    val pe = simpleEditor(name)
    assert(pe.name === name)
  }

  it should "expose no parameters unless declared" in {
    val name = "someName"
    val pe = simpleEditor(name)
    assert(pe.parameters.size === 0)
  }

  private  def simpleEditor(name: String): ProjectEditor = {
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("name")
    val program = RugEditor(name, None, Nil, name, Nil, Nil, None, Nil, Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))
    compiler.compile(program).asInstanceOf[ProjectEditor]
  }

  it should "perform modify without exception" in {
    val extraText = "\nI'm talkin' about ethics"
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("append", None, Seq(WrappedFunctionArg(SimpleLiteral(extraText))))
    val program = RugEditor("foobar", None, Nil, "description goes here", Nil, Nil, None, Nil, Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))

    val pe = compiler.compile(program).asInstanceOf[ProjectEditor]

    // Now try the editor
    //show(RugCompilerTest.JavaAndText)
    val r = pe.modify(RugCompilerTest.JavaAndText, SimpleParameterValues.Empty)
    r match {
      case sm: SuccessfulModification =>
        //show(sm.result)
        val f = sm.result.findFile("src/main/java/Dog.java").get
        f.content.endsWith(extraText) should be(true)
      case _ => ???
    }
  }

  it should "expose tags" in {
    val fileExpression = ParsedRegisteredFunctionPredicate("isJava")
    val doStep = FunctionDoStep("append", None, Seq(WrappedFunctionArg(SimpleLiteral("whatever"))))
    val tags = Seq("spring", "java")

    val program = RugEditor("foobar", None, tags, "description goes here", Nil, Nil, None, Nil, Nil,
      Seq(With("File", "f", None, fileExpression, Seq(doStep))))

    val pe = compiler.compile(program).asInstanceOf[RugDrivenProjectEditor]
    pe.tags.map(t => t.name) should equal(tags)
  }
}
