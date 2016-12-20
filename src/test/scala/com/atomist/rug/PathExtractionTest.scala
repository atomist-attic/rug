package com.atomist.rug

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, ProjectEditor, SuccessfulModification}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests extract from syntax
  */
class PathExtractionTest extends FlatSpec with Matchers {

  it should "extract simple value" in {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
         |editor First
         |
         |let f = $(src//[name='application.properties'])
         |
         |with f
         |  do append "foo=bar"
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head
    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        f.content should equal("foo=bar")
    }
  }

  it should "follow path to node and change type" in {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
         |editor First
         |
         |let m = $(/src/main/java/com/example/->JavaType[name='DemoApplication']/[type='method']).name
         |
         |with File when path = "src/main/resources/application.properties"
         |  do append m
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head
    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        f.content.contains("main") should be (true)
    }
  }

  it should "self or descend into type" in pendingUntilFixed {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
        |editor First
        |
        |let m = $(/src/main/java//*:java.class[name='DemoApplication']/[type='method']).name
        |
        |with File when path = "src/main/resources/application.properties"
        |  do append m
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head
    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        f.content.contains("main") should be (true)
    }
  }

//  it should "save node value and move on" in {
//    val project = ParsingTargets.NewStartSpringIoProject
//    val prog =
//      """
//        |editor First
//        |
//        |let m = `src/main/java//{java.class}$[name='DemoApplication']/[type='method']`
//        |let name = m.
//        |
//        |with m begin
//        |  do eval { print ("****** " + m) }
//        |  do eval { print("-----" + m.name()) }
//        |end
//      """.stripMargin
//    val rp = new DefaultRugPipeline
//    val ed = rp.createFromString(prog).head
//    // Check it works OK with these parameters
//    ed.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments.Empty)
//    //    match {
//    //      case sm: NoModificationNeeded =>
//    ////        val f = sm.result.findFile("src/main/resources/application.properties").get
//    ////        println(f.content)
//    ////        f.content should equal("foo=bar")
//    //    }
//  }

  /*

  it should "extract simple content using ANDed predicate" in {
    val contentFile = StringFileArtifact("content.txt", "The quick brown fox jumped over the lazy dog")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project, s"let r = from file f when path = '${contentFile.path}' and true return content",
      contentFile.content, "mynewfile.txt")
  }

  it should "extract simple content using default not firing" in {
    val contentFile = StringFileArtifact("content.txt", "The quick brown fox jumped over the lazy dog")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project, s"let r = from file f when path = '${contentFile.path}' return content default 'foobar'",
      contentFile.content, "mynewfile.txt")
  }

  it should "extract simple content falling back to default firing" in {
    val expectedContent = "foobar"
    extract(new EmptyArtifactSource(""),
      s"let r = from file f when path = 'doesnot.exist' return content default '$expectedContent'",
      expectedContent, "some/path/to/mynewfile.txt")
  }

  it should "extract simple content using grammar" in {
    val contentFile = StringFileArtifact("content.txt", "Mick Jagger is a singer")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      """
        |let job = <
        |  NAME : [A-Z][a-z]+;
        |  ROLE : [a-zA-Z]+;
        |  sentence : name=NAME 'is a' role=ROLE;
        |>
        |let r = from file f from job j return valueOf "role"
      """.stripMargin,
      "singer", "mynewfile.txt")
  }

  it should "extract simple content using dot notation" in pendingUntilFixed {
    val contentFile = StringFileArtifact("content.txt", "Mick Jagger is a singer")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      """
        |let job = <
        |  NAME : \w+\s\w+;
        |  ROLE : \w+;
        |  sentence : name=NAME 'is a' role=ROLE
        |>
        |let r = from file f return job.role
      """.stripMargin,
      "singer", "mynewfile.txt")
  }

  it should "extract simple content using nested grammar" in {
    val contentFile = StringFileArtifact("content.txt", "Mick is a singer")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      """
        |let job = <
        |  NAME : [A-Z][a-z]+;
        |  ROLE : [a-z]+;
        |  sentence : name=NAME 'is a' role=ROLE;
        |>
        |let r = from file f when true from job j return valueOf "role"
      """.stripMargin,
      "singer", "mynewfile.txt")
  }

  // This is a problem because of whitespace skipping
  it should "extract simple content using nested grammar including space" in pendingUntilFixed {
    val contentFile = StringFileArtifact("content.txt", "Mick Jagger is a singer")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      """
        |let job = <
        |  NAME : [a-zA-Z ]+;
        |  ROLE : [a-zA-Z]+;
        |  sentence : name=NAME 'is a' role=ROLE;
        |>
        |let r = from file f when true from job j return valueOf "role"
      """.stripMargin,
      "singer", "mynewfile.txt")
  }

  it should "extract simple content using grammar with multiple match" in pendingUntilFixed {
    val contentFile = StringFileArtifact("content.txt",
      """
        |Mick Jagger is a singer.
        |Keith Richards is a guitarist.
      """.stripMargin)
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      s"""
         |let job = <
         |  name ::= \\w+\\s\\w+
         |  role ::= \\w+
         |  sentence ::= <name> is a <role>
         |>
         |let r = from grammar(job) g return valueOf "role"
       """.stripMargin,
      "singer", "mynewfile.txt")
  }

  it should "extract simple content with nested from" in {
    val contentFile = StringFileArtifact("Dog.java", "class Dog {}")
    val contentFile2 = StringFileArtifact("Squirrel.java", "class Squirrel {}\nclass Porpoise {}")
    val project = new SimpleFileBasedArtifactSource("", Seq(contentFile, contentFile2))
    extract(project,
      s"let r = from java.source j when typeCount = 1 from java.class c return name",
      "Dog", "mynewfile.txt")
  }

  it should "extract invokes method with path" in {
    val contentFile = StringFileArtifact("content.txt", "The quick brown fox jumped over the lazy dog")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      s"let r = from file f when path = '${contentFile.path}' return (f.content.replaceAll 'T' 't' )",
      contentFile.content.toLowerCase(), "mynewfile.txt")
  }
  */

  /*
  it should "extract editor info from rug files" in pendingUntilFixed {
    val prog =
      """
        |reviewer EditorExtractor
        |
        |let editorDef = <
        |  editor : 'editor' name=JAVA_IDENTIFIER
        |>
        |
        |with File f when { f.name().endsWith(".rug") }
        | with editorDef e begin
        |   do eval {print(e)}
        |   do majorProblem name
        | end
      """.stripMargin
    val rp = new DefaultRugPipeline

    val project = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("editor/FooEditor.rug",
        """
          |editor Foo
          |
          |with File f when name = "thisDoesNotExist"
          | do append "this is utter bollocks"
        """.stripMargin))

    val ed = rp.createFromString(prog).head
    // Check it works OK with these parameters
    val rr = ed.asInstanceOf[ProjectReviewer].review(project, SimpleProjectOperationArguments.Empty)
    rr.comments.size should be(1)
  }

  it should "extract filename from single file" in {
    val contentFile = StringFileArtifact("content.txt", "The quick brown fox jumped over the lazy dog")
    val project = new SimpleFileBasedArtifactSource("", contentFile)
    extract(project,
      s"let r = from file f return name",
      "content.txt", "mynewfile.txt")
  }

  it should "extract filename from multiple files" in {
    val contentFile1 = StringFileArtifact("content1.txt", "The quick brown fox jumped over the lazy dog")
    val contentFile2 = StringFileArtifact("content2.txt", "The quick brown fox jumped over the lazy dog")
    val project = new SimpleFileBasedArtifactSource("", Seq(contentFile1, contentFile2))
    val newFile = "mynewfile.txt"

    val expectedResult = "2"
    val edName = "ExtractAndWriteNames"
    val prog =
      s"""
         |editor $edName
         |
         |let names = from file f return name
         |
         |with names n begin
         |  do println { names.toString() }
         |  do println { n.toString() }
         |end
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).find(_.name.equals(edName)).getOrElse(
      fail(s"Expected editor $edName not found")
    )

    ed.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments.Empty) match {
      case nmn: NoModificationNeeded =>
    }
  }
  */

}
