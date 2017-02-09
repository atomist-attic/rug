package com.atomist.rug.test

import com.atomist.rug.InvalidRugTestScenarioName
import com.atomist.util.scalaparsing.SimpleLiteral
import com.atomist.rug.parser.{ParsedRegisteredFunctionPredicate, WrappedFunctionArg}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TestScriptParserRugTest extends FlatSpec with Matchers {

  val parser = RugTestParser

  it should "parse computations" in {
    val f = InlineFileSpec("src/main/java/Dog.java", "class Dog {}")
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |let cat = "Cat"
         |
         |given
         |   ${f.path} = "${f.content}"
         |
         | Rename old_class="Dog", new_class = cat
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.name === scenarioName)
    assert(test.computations.size === 1)
    assert(test.computations.head.name === "cat")
  }

  it should "parse single file assertion" in
    parseSingleFileAssertion("src/main/java/Dog.java")

  it should "default debug flag to false" in {
    val scenario = parseSingleFileAssertion("src/main/java/Dog.java")
    assert(scenario.debug === false)
  }

  it should "set debug flag explicitly to false" in {
    val scenario = parseSingleFileAssertion("src/main/java/Dog.java", Some(false))
    assert(scenario.debug === false)
  }

  it should "set debug flag explicitly to true" in {
    val scenario = parseSingleFileAssertion("src/main/java/Dog.java", Some(true))
    assert(scenario.debug === true)
  }

  it should "parse single file assertion with unusual characters besides space in filename" in {
    val strangeContent = Set("-", "%", ".", "$", "_", "!", "*")
    strangeContent.foreach(c => parseSingleFileAssertion(s"src/${c}main/java/Dog.java"))
  }

  private  def parseSingleFileAssertion(filepath: String, debug: Option[Boolean] = None): TestScenario = {
    val f = InlineFileSpec(filepath, "class Dog {}")
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |${debug.map(f => s"debug=$f").getOrElse("")}
         |
         |given
         |   ${f.path} = "${f.content}"
         |
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "${filepath.replace("Dog", "Cat")}" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.name === scenarioName)
    assert(test.computations.size === 0)
    assert(test.givenFiles.fileSpecs === Seq(f))
    assert(test.outcome.assertions === Seq(PredicateAssertion(ParsedRegisteredFunctionPredicate("contentEquals",
      Seq(WrappedFunctionArg(SimpleLiteral(filepath.replace("Dog", "Cat"))),
        WrappedFunctionArg(SimpleLiteral("class Cat {}"))
      )))))
    test
  }

  it should "parse load file" in
    parseLoadFile()

  it should "accept open when" in
    parseLoadFile(true)

  private  def parseLoadFile(useWhen: Boolean = false) {
    val f = LoadedFileSpec("src/main/java/Dog.java", "resources/Dog.java")
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |given
         |   ${f.path} from resources/Dog.java
         |
         |${if (useWhen) "when" else ""}
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.name === scenarioName)
    assert(test.givenFiles.fileSpecs === Seq(f))
    assert(test.outcome.assertions === Seq(PredicateAssertion(ParsedRegisteredFunctionPredicate("contentEquals",
      Seq(WrappedFunctionArg(SimpleLiteral("src/main/java/Cat.java")),
        WrappedFunctionArg(SimpleLiteral("class Cat {}")))))
    ))
  }

  it should "parse load files" in {
    val f = FilesUnderFileSpec("resources")
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |given
         |   files under resources
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.name === scenarioName)
    assert(test.givenFiles.fileSpecs === Seq(f))
    val poa = test.args
    poa.parameterValues.size should be >= 2
    poa.paramValue("old_class") should equal("Dog")
    poa.paramValue("new_class") should equal("Cat")

    assert(test.outcome.assertions === Seq(PredicateAssertion(ParsedRegisteredFunctionPredicate("contentEquals",
      Seq(WrappedFunctionArg(SimpleLiteral("src/main/java/Cat.java")),
        WrappedFunctionArg(SimpleLiteral("class Cat {}")))))
    ))
  }

  it should "parse empty archive" in {
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |given
         |   Empty
         |
         |# We just need valid content to parse. Obviously this wouldn't work as there ARE no files
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.givenFiles.fileSpecs === Seq(EmptyArchiveFileSpec))
  }

  it should "parse load archive root" in {
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |given
         |   ArchiveRoot
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.givenFiles.fileSpecs === Seq(ArchiveRootFileSpec))
  }

  it should "parse file load assertion" in {
    val f = InlineFileSpec("src/main/java/Dog.java", "class Dog {}")
    val scenarioName = "Class rename should work"
    val prog =
      s"""
         |scenario $scenarioName
         |
         |given
         |   ${f.path} = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.name === scenarioName)
    assert(test.givenFiles.fileSpecs === Seq(f))
    assert(test.outcome.assertions === Seq(PredicateAssertion(ParsedRegisteredFunctionPredicate("contentEquals",
      Seq(WrappedFunctionArg(SimpleLiteral("src/main/java/Cat.java")),
        WrappedFunctionArg(SimpleLiteral("class Cat {}")))))
    ))
  }

  it should "parse multiple file assertions" in {
    val f = InlineFileSpec("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Foobar
         |
         |given
         |   ${f.path} = "${f.content}"
         |   ${f.path} = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
         |  and contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.givenFiles.fileSpecs === Seq(f, f))
    assert(test.outcome.assertions.size === 2)
    //    test.outcome.assertions should equal(Seq(PredicateAssertion(
    //      ParsedRegisteredFunctionPredicate("contentEquals",
    //        Seq(StringLiteralFunctionArg("src/main/java/Cat.java"),
    //          StringLiteralFunctionArg("class Cat {}")))),
    //      ParsedRegisteredFunctionPredicate("contentEquals",
    //        Seq(StringLiteralFunctionArg("src/main/java/Cat2.java"),
    //          StringLiteralFunctionArg("class Cat2 {}")))
    //    ))
  }

  it should "accept project do step as given" is pending

  it should "accept project predicates as assertions" in {
    val f = InlineFileSpec("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Class rename should work for Dogs and Cats
         |
         |given
         |   ${f.path} = "${f.content}"
         |   ${f.path} = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  fileCount = 1
         |  # and fileHasContent "src/main/java/Cat.java" "class Cat {}"
         |  # and fileHasContent "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.size === 1)
    val test = tp.head
    assert(test.givenFiles.fileSpecs === Seq(f, f))
    //    test.outcome.assertions should equal(Seq(PredicateAssertion(
    //      ParsedRegisteredFunctionPredicate("contentEquals",
    //        Seq(StringLiteralFunctionArg("src/main/java/Cat.java"),
    //          StringLiteralFunctionArg("class Cat {}")))),
    //      ParsedRegisteredFunctionPredicate("contentEquals",
    //        Seq(StringLiteralFunctionArg("src/main/java/Cat2.java"),
    //          StringLiteralFunctionArg("class Cat2 {}")))
    //    )
    //    )
  }

  it should "accept NoChange keyword" in {
    val prog =
      """
        |scenario Foobar
        |
        |given
        |   test.txt = "foo"
        |
        | Rename old_class="Dog", new_class = "Cat"
        |
        |then
        |  NoChange
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.head.outcome.assertions === Seq(NoChangeAssertion))
  }

  it should "accept NotApplicable keyword" in {
    val prog =
      """
        |scenario Foobar
        |
        |given
        |   Empty
        |
        | Rename old_class="Dog", new_class = "Cat"
        |
        |then
        |  NotApplicable
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.head.outcome.assertions === Seq(NotApplicableAssertion))
  }

  it should "accept ShouldFail keyword" in {
    val prog =
      """
        |scenario Foobar
        |
        |given
        |   test.txt = "foo"
        |
        | Rename old_class="Dog", new_class = "Cat"
        |
        |then
        |  ShouldFail
      """.stripMargin

    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.head.outcome.assertions === Seq(ShouldFailAssertion))
  }

  // TODO "and" gets run into preceding "v".
  // this could be a bug in parser combinators
  it should "parse handle single letter identifier use in assertions" in pendingUntilFixed {
    val prog =
      """
        |scenario New project should pass smoke test
        |
        |let project_name = "foo"
        |let new_package = "com.foo.bar"
        |let v = "0.0.1"
        |
        |let pomPath = "pom.xml"
        |let readmePath = "README.md"
        |
        |given
        |  ArchiveRoot
        |
        |NewSpringBootRestMicroserviceProject
        |    name = project_name,
        |    description = "And now for something completely different",
        |    group_id = "somegroup",
        |    version = v,
        |    new_class = "MyTest"
        |
        |then
        |  fileExists "Dockerfile"
        |  and fileContains "Dockerfile" { project_name + "-" + v + ".jar" }
        |  and fileExists "src/main/java/com/foo/bar/MyTestApplication.java"
        |  and fileContains pomPath name
        |  and fileContains pomPath v
        |  and fileContains readmePath project_name
      """.stripMargin
    val tp = parser.parse(StringFileArtifact("x.rt", prog))
    assert(tp.head.outcome.assertions.size === 6)
  }

  it.should("allow operations in the given clause").in {
    val prog =
      """scenario This Editor should be idempotent
        |
        |given
        |  ArchiveRoot
        |  UpgradeToBeginnerProgram
        |
        |when
        |  UpgradeToBeginnerProgram
        |
        |then
        |  NoChange""".stripMargin
    val parsed = parser.parse(StringFileArtifact("IdempotencyTest.rt", prog))
    val myTest = parsed.head
    assert(myTest.givenInvocations.size === 1)
  }

  it should "allow multiple operations in the given clause" in {
    val prog =
      """scenario This Editor should be idempotent
        |
        |given
        |  ArchiveRoot
        |  UpgradeToBeginnerProgram
        |  UpgradeToBeginnerProgram some_arg=banana
        |
        |when
        |  UpgradeToBeginnerProgram
        |
        |then
        |  NoChange""".stripMargin
    val parsed = parser.parse(StringFileArtifact("IdempotencyTest.rt", prog))
    val myTest = parsed.head
    assert(myTest.givenInvocations.size === 2)
  }

  it should "disallow empty scenario name" in {
    val prog =
      """scenario
        |
        |given
        |  ArchiveRoot
        |
        |then
        |  NoChange""".stripMargin

    assertThrows[InvalidRugTestScenarioName] {
      parser.parse(StringFileArtifact("InvalidScenarioName.rt", prog))
    }
  }
}
