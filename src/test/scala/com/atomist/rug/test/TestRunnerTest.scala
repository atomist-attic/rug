package com.atomist.rug.test

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.ProjectOperationArchiveReader
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.parser.ParserCombinatorRugParser
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TestRunnerTest extends FlatSpec with Matchers {

  import com.atomist.rug.InterpreterRugPipeline._

  val testRunner = new TestRunner

  it should "fail when not finding editor" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Foobar
         |debug = true
         |
         |let new_class = "Cat"
         |
         |given
         |   ${f.path} = "${f.content}"
         |
         |Rename old_class="Dog", new_class = new_class
         |
         |then
         |  # fileCount 1
         |  contentEquals "src/main/java/Cat.java" "class Cat {}"
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), Nil)
    executedTests.tests.size should be(1)
    executedTests.tests.head.passed should be(false)
  }

  it should "test a generator with parameters" in {
    val generator = StringFileArtifact(".atomist/editors/OneFileGenerator.rug",
      """generator OneFileGenerator
        |
        |param contents: ^.*$
        |
        |with File f
        |   do setContent contents
      """.
        stripMargin)
    val theOneFile = StringFileArtifact("happy.txt", "Joy Joy")
    val rugArchive = SimpleFileBasedArtifactSource(theOneFile, generator)
    val parsedGenerator: Seq[ProjectOperation] = new ProjectOperationArchiveReader().findOperations(rugArchive, None, Nil).generators

    val generatorTest = StringFileArtifact(".atomist/tests/OneFileGenerator.rt",
      """scenario SomethingIsGenerated
        |
        |given
        |  OneFileGenerator contents="Maximize developer productivity through automation & comprehension"
        |
        |then
        |  fileExists "happy.txt"
        |  and fileContains "happy.txt" "automation & comprehension"
      """.stripMargin)

    val parsedTest = RugTestParser.parse(generatorTest)
    val executedTests = testRunner.run(parsedTest, rugArchive, parsedGenerator)

    executedTests.tests.size should be(1)
    val testResult = executedTests.tests.head
    testResult.passed should be(true)
  }

  it should "test a generator" in {
    val generator = StringFileArtifact(".atomist/editors/OneFileGenerator.rug",
      """generator OneFileGenerator
        |
        |with Project p
        |   do eval { print("I am so happy") }
      """.stripMargin)
    val theOneFile = StringFileArtifact("happy.txt", "Joy Joy")
    val rugArchive = SimpleFileBasedArtifactSource(theOneFile, generator)
    val parsedGenerator: Seq[ProjectOperation] = new ProjectOperationArchiveReader().findOperations(rugArchive, None, Nil).generators

    val generatorTest = StringFileArtifact(".atomist/tests/OneFileGenerator.rt",
      """scenario SomethingIsGenerated
        |
        |given
        |  OneFileGenerator
        |
        |then
        |  fileExists "happy.txt"
      """.stripMargin)

    val parsedTest = RugTestParser.parse(generatorTest)
    val executedTests = testRunner.run(parsedTest, rugArchive, parsedGenerator)

    executedTests.tests.size should be(1)
    val testResult = executedTests.tests.head
    testResult.passed should be(true)
  }

  it should "pass with passing editor and file created inline" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  fileCount = 1
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin)
  }

  it should "pass with passing editor and file created inline with JavaScript expression in predicate" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  fileCount = { 1 }
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin)
  }

  it should "pass with passing editor and file created inline with JavaScript expression in predicate referencing implicit variable" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  fileCount = { 1 }
         |  and { result.fileContains("src/main/java/Cat.java", "class Cat") }
      """.stripMargin)
  }

  it should "pass with passing editor and file loaded from backing ArtifactSource" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |given
         |   ${f.path} from "resources/foobar"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         | #dumpAll
         |
         |then
         |  fileCount = 1
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin, rugAs = new SimpleFileBasedArtifactSource("", StringFileArtifact("resources/foobar", f.content))
    )
  }

  it should "pass with passing editor and file loaded using 'files under' from backing ArtifactSource" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |given
         |   files under "resources"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         | #dumpAll
         |
         |then
         |  fileCount = 1
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin, rugAs = new SimpleFileBasedArtifactSource("", StringFileArtifact("resources/" + f.path, f.content))
    )
  }

  it should "pass with passing editor using computations" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |let cat = "Cat"
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = cat
         |
         |then
         |  fileCount = 1
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin)
  }

  it should "pass with dumpAll as part of assertions" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |let cat = "Cat"
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = cat
         |
         |then
         |  fileCount = 1
         |  and dumpAll
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin)
  }

  it should "pass with uses for a namespaced project editor" in {
    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    shouldPass(
      s"""
         |scenario Foobar
         |
         |uses testnamespace.Rename
         |
         |let cat = "Cat"
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = cat
         |
         |then
         |  fileCount = 1
         |  and dumpAll
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin, Some("testnamespace"))
  }

  private def shouldPass(prog: String, namespace: Option[String] = None, rugAs: ArtifactSource = EmptyArtifactSource("")) = {
    val edProg =
      """
        |editor Rename
        |with JavaType c when name = "Dog"
        |do rename "Cat"
      """.stripMargin

    //    val as =
    //      new SimpleFileBasedArtifactSource(DefaultRugArchive,
    //        StringFileArtifact(defaultFilenameFor(input), input))

    val rp = new DefaultRugPipeline()

    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(edProg), edProg))
    val eds = rp.create(as, namespace)
    val testProg = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(testProg, rugAs, eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
    }
  }

  it should "fail with unresolvable namespaced project editor" in {
    val edProg =
      """
        |editor Rename
        |with JavaType c when name = "Dogxxxx"
        |do rename "Cat"
      """.stripMargin
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact("editor/LineCommenter.rug", edProg))
    val eds = new DefaultRugPipeline().create(as, Some("testnamespace"))

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Foobar
         |
         |uses testnamespaceWRONG.Rename
         |
         |let cat = "Cat"
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = cat
         |
         |then
         |  fileCount = 1
         |  and dumpAll
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if !t.passed =>
      // Ok
    }
  }

  it should "fail with failing editor" in {
    val edProg =
      """
        |editor Rename
        |with JavaType c when name = "Dogxxxx"
        |do rename "Cat"
      """.stripMargin
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact("editor/LineCommenter.rug", edProg))
    val eds = new DefaultRugPipeline().create(as, None)

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Foobar
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  fileCount = 1
         |  and fileContains "src/main/java/Cat.java" "class Cat"
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if !t.passed =>
      // Ok
    }
  }

  it should "pass with no change if test scenario wants that" in {
    val edProg =
      """
        |editor Rename
        |with JavaType c when name = "Dogxxxx"
        |do rename "Cat"
      """.stripMargin

    val rp = new DefaultRugPipeline()
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(edProg), edProg))
    val eds = rp.create(as, None)

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Class rename should work for dogs and cats
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  NoChange
      """.stripMargin

    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

  it should "fail if test scenario wants that" in {
    val edProg =
      """
        |editor Rename
        |with JavaType c when name = "Dog"
        |do fail "This is bad"
      """.stripMargin

    val rp = new DefaultRugPipeline()
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(edProg), edProg))
    val eds = rp.create(as, None)

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Class rename should work for dogs and cats
         |debug = true
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | Rename old_class="Dog", new_class = "Cat"
         |
         |then
         |  ShouldFail
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head.eventLog.input.shouldBe(defined)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

  it should "verify fail on missing parameters" in {
    val edProg =
      """
        |editor Rename
        |param old_class: @java_class
        |with JavaType c when name = old_class
        |do rename "foo"
      """.stripMargin
    val rp = new DefaultRugPipeline()
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(edProg), edProg))
    val eds = rp.create(as, None)

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Class rename should work for dogs and cats
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | # missing parameter
         | Rename new_class = "Cat"
         |
         |then
         |  MissingParameters
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

  it should "verify fail on invalid parameters" in {
    val edProg =
      """
        |editor Rename
        |param old_class: @java_class
        |with JavaType c when name = old_class
        |do rename "foo"
      """.stripMargin
    val rp = new DefaultRugPipeline()
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(edProg), edProg))
    val eds = rp.create(as, None)

    val f = StringFileArtifact("src/main/java/Dog.java", "class Dog {}")
    val prog =
      s"""
         |scenario Class rename should work for dogs and cats
         |
         |given
         |   "${f.path}" = "${f.content}"
         |
         | # invalid parameter
         | Rename old_class = "Dogx.", new_class = "Cat"
         |
         |then
         |  InvalidParameters
      """.stripMargin
    val test = RugTestParser.parse(StringFileArtifact("x.rt", prog))
    val executedTests = testRunner.run(test, EmptyArtifactSource(""), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

  it should "test edit README" in
    testEditReadMe(true)

  it should "test edit README omitting default parameter" in
    testEditReadMe(false)

  private def testEditReadMe(includeSecondParameter: Boolean) = {
    val prog =
      """
        |editor UpdateReadme
        |
        |param name: @any
        |
        |@default 'Boy Wizard'
        |param description: @any
        |
        |with File f when { f.name().contains(".md") } begin
        |	do replace "{{name}}" name
        |	do replace "{{description}}" description
        |end
      """.stripMargin
    val (name, description) = ("Harry Potter", "Boy Wizard")
    val scenario =
      s"""
         |scenario UpdateReadme should update README.md 2
         |
         |given
         |	ArchiveRoot
         |
         |	UpdateReadme name = "$name" ${if (includeSecondParameter) ", description = '" + description + "'" else ""}
         |
         |then
         |  # TODO need computations
         |  fileExists "README.md"
         |	 and fileContains "README.md" "Harry Potter"
         |	 and fileContains "README.md" "Boy Wizard"
      """.stripMargin
    val rp = new DefaultRugPipeline()
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(prog), prog))
    val eds = rp.create(as, None)
    val readme = StringFileArtifact("README.md",
      """
        |# {{name}}
        |
        |## {{description}}
      """.stripMargin)
    val test = RugTestParser.parse(StringFileArtifact("x.rt", scenario))
    val executedTests = testRunner.run(test, new SimpleFileBasedArtifactSource("", readme), eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

  it should "copy file from editor backing repo" in {
    val prog =
      """
        |editor AddDocumentation
        |
        |with Project begin
        |
        |  do copyEditorBackingFileOrFail "test.txt" "test.txt"
        |
        |end
        |
      """.stripMargin
    val scenario =
      s"""
         |scenario Should copy readme
         |
         |given
         |	Empty
         |
         |AddDocumentation
         |
         |then
         |  fileExists "test.txt"
      """.stripMargin
    val readme = StringFileArtifact("test.txt", "Some pearls of wisdom")
    val editorBackingArchive = new SimpleFileBasedArtifactSource("", Seq(readme, StringFileArtifact("editors/AddDocumentation.rug", prog)))
    val eds = new DefaultRugPipeline().create(editorBackingArchive, None, Nil)
    val test = RugTestParser.parse(StringFileArtifact("x.rt", scenario))
    val executedTests = testRunner.run(test, editorBackingArchive, eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }

}

class MoreTestRunnerTest extends FlatSpec with Matchers {
  val testRunner = new TestRunner

  it should "pass the assertion when a precondition restricts an editor from running" in {
    val prog =
      """
        |editor DoSomething
        |
        |precondition IsMaven
        |
        |with Project begin
        |
        |  do copyEditorBackingFileOrFail "test.txt" "test.txt"
        |
        |end
        |
        |predicate IsMaven
        |  with Pom
        |
      """.
        stripMargin
    val scenario
    =
    """
      |scenario Should not copy readme for empty project
      |
      |given
      |	Empty
      |
      |DoSomething
      |
      |then
      |  NotApplicable
    """.stripMargin
    val readme
    = StringFileArtifact("test.txt", "Some pearls of wisdom")
    val
    editorBackingArchive = new SimpleFileBasedArtifactSource("", Seq(readme, StringFileArtifact("editors/DoSomething.rug", prog)))
    val eds = new DefaultRugPipeline().create(editorBackingArchive, None, Nil)
    val test = RugTestParser.parse(StringFileArtifact("x.rt", scenario))
    val executedTests = testRunner.run(test, editorBackingArchive, eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }
}

class EvenMoreTestRunnerTest extends FlatSpec with Matchers {
  val testRunner = new TestRunner

  it should "pass the assertion when preconditions restrict an editor from running" in {
    val prog =
      """
        |editor DoSomething
        |
        |precondition IsOk
        |precondition IsMaven
        |
        |with Project begin
        |
        |  do copyEditorBackingFileOrFail "test.txt" "test.txt"
        |
        |end
        |
        |predicate IsMaven
        |  with Pom
        |
        |predicate IsOk
        |  with Project
        |
      """.stripMargin
    val scenario =
      s"""
         |scenario Should not copy readme for empty project
         |
         |given
         |  Empty
         |
         |DoSomething
         |
         |then
         |  NotApplicable
      """.stripMargin
    val readme = StringFileArtifact("test.txt", "Some pearls of wisdom")
    val editorBackingArchive = new SimpleFileBasedArtifactSource("", Seq(readme, StringFileArtifact("editors/DoSomething.rug", prog)))
    val eds = new DefaultRugPipeline().create(editorBackingArchive, None, Nil)
    val test = RugTestParser.parse(StringFileArtifact("x.rt", scenario))
    val executedTests = testRunner.run(test, editorBackingArchive, eds)
    executedTests.tests.size should be(1)
    executedTests.tests.head match {
      case t if t.passed =>
      // Ok
    }
  }
}
