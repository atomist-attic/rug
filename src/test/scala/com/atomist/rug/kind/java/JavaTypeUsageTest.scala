package com.atomist.rug.kind.java

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug._
import com.atomist.rug.kind.java.JavaVerifier._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import com.atomist.source.file.ClassPathArtifactSource.toArtifactSource
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

object JavaTypeUsageTest extends Matchers {

  val NewSpringBootProject: ArtifactSource =
    toArtifactSource("./springboot1")

  val JavaAndText: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("pom.xml", "<maven></maven"),
      StringFileArtifact("/src/main/java/Dog.java",
        """
          |import java.io.Serializable;
          |import java.util.Set;
          |import com.foo.Bar;
          |import com.someone.ComFooBar;
          |import com.someone.FooBar;
          |
          |/**
          | * Class comment.
          | */
          |@Bar
          |class Dog
          |  implements
          |    Serializable,
          |    Cloneable {
          |
          |   @ComFooBar
          |   private String stringField;
          |
          |   /**
          |     * No-arg constructor.
          |     */
          |   public Dog() {}
          |
          |   public Dog(String stringField) {
          |     this.stringField = stringField;
          |   }
          |
          |   /**
          |    * Bark.
          |    */
          |   @FooBar
          |   public void bark() {
          |   }
          |}""".stripMargin),
      StringFileArtifact("src/test/java/SpringBootJunit5ApplicationTests.java",
        """
          |import org.junit.runner.RunWith;
          |import org.springframework.boot.test.context.SpringBootTest;
          |import org.springframework.test.context.junit4.SpringRunner;
          |import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
          |
          |@RunWith(SpringRunner.class)
          |@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
          |public class SpringBootJunit5ApplicationTests {
          |}""".stripMargin)
    )
  )

  val SrcFile = "src/main/java/Dog.java"

  def executeJava(program: ArtifactSource, rugPath: String, as: ArtifactSource = JavaAndText): ArtifactSource = {
    val r = attemptToModify(program, rugPath, as, Map[String, String]())
    r match {
      case sm: SuccessfulModification =>
        verifyJavaIsWellFormed(sm.result)
        sm.result
      case x => fail(s"Unexpected: $x")
    }
  }

  def attemptToModify(program: ArtifactSource,
                      rugPath: String,
                      as: ArtifactSource, poa: Map[String, String]): ModificationAttempt = {
    // println(ArtifactSourceUtils.prettyListFiles(program))
    val progAs = TypeScriptBuilder.compileWithModel(program)
    val eds = RugArchiveReader(progAs)
    val pe = eds.editors.head
    pe.modify(as, SimpleParameterValues(poa))
  }
}

class JavaTypeUsageTest extends FlatSpec with Matchers with LazyLogging {

  import JavaTypeUsageTest._

  "Java type" should "annotate class using function" in {
    annotateClass(getProgram("com/atomist/rug/kind/java1/ClassAnnotated.ts"))
  }

  it should "annotate class going straight to class without enclosing JavaSource" in {
    annotateClass(getProgram("com/atomist/rug/kind/java2/ClassAnnotated.ts"))
  }

  it should "add an annotation with properties to class" in {
    val program = toArtifactSource("com/atomist/rug/kind/java3/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    annotateClass(program, "import org.junit.jupiter.api.extension.ExtendWith;", "@ExtendWith(value = SpringExtension.class)")
  }

  it should "remove annotation from class" in {
    val program = getProgram("com/atomist/rug/kind/java4/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include("import com.foo.Bar;")
      f.content shouldNot include("@Bar")
    }).getOrElse(fail("File not found"))
  }

  it should "remove one annotation only from class" in {
    val program = getProgram("com/atomist/rug/kind/java5/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile("src/test/java/SpringBootJunit5ApplicationTests.java").map(f => {
      f.content.lines.size should be > 0
      f.content shouldNot include("@RunWith")
      f.content should include("@SpringBootTest")
    }).getOrElse(fail("File not found"))
  }

  val dog = StringFileArtifact(
    "src/main/java/com/foo/bar/Dog.java",
    """
      |package com.foo.bar;
      |class Dog {}
    """.stripMargin
  )

  val cat = StringFileArtifact(
    "src/main/java/com/foo/bar/Cat.java",
    """
      |package com.foo;
      |import com.foo.bar.Dog;
      |class Cat {
      | // Doesn't need to be refactored
      | private Dog dog = null;
      |
      |}
    """.stripMargin
  )

  val squirrel = StringFileArtifact(
    "src/main/java/com/foo/Squirrel.java",
    """
      |package com.foo;
      |class Squirrel {
      | // Needs to be refactored
      | private Dog dog = null;
      |
      |}
    """.stripMargin
  )

  it should "repackage class and verify name and path" in {
    val program = getProgram("com/atomist/rug/kind/java6/ClassAnnotated.ts")

    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.ts", as)
    result.allFiles.foreach(f => logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content should include("package com.atomist;")
  }

  it should "repackage class and verify explicitly importing users are updated" in pendingUntilFixed {
    val program = getProgram("com/atomist/rug/kind/java7/ClassAnnotated.ts")

    val as = new SimpleFileBasedArtifactSource("", Seq(dog, cat, squirrel))

    val result = executeJava(program, "editors/ClassAnnotated.ts", as)
    result.allFiles.foreach(f => logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content.contains("package com.atomist;") should be(true)

    // Should now import Dog
    f.content should include("import com.atomist.Dog;")
  }

  it should "rename class and verify name" in {
    val program = getProgram("com/atomist/rug/kind/java8/ClassAnnotated.ts")
    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.ts", as)
    val f = result.findFile("src/main/java/com/foo/bar/Dingo.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content should include("class Dingo")
  }

  it should "verify usages of renamed class are updated" is pending

  it should "add import" in {
    val program = getProgram("com/atomist/rug/kind/java9/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include("import java.util.List")
    }).getOrElse(fail("File not found"))
  }

  it should "add and remove import" in {
    val program = getProgram("com/atomist/rug/kind/java10/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content shouldNot include("import java.util.List")
    }).getOrElse(fail("File not found"))
  }

  it should "remove an import" in {
    val program = getProgram("com/atomist/rug/kind/java11/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content shouldNot include("import java.util.Set")
    }).getOrElse(fail("File not found"))
  }

  // TODO this needs a fix in addImport implementation. Not to do with DSL -> TypeScript
  it should "not add import for annotation added to class in same package" in pendingUntilFixed {
    val impl = StringFileArtifact("src/main/java/Absquatulator.java",
      """
        |package com.foo;
        |
        |public final class Absquatulator {
        |
        |}
      """.stripMargin)
    val as = new SimpleFileBasedArtifactSource("", Seq(impl))

    val (pkg, ann) = ("com.foo", "Baz")

    val program = getProgram("com/atomist/rug/kind/java12/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(impl.path).map(f => {
      f.content.lines.size should be > 0
      f.content shouldNot include(s"import $pkg.$ann")
      f.content should include(s"@$ann")
    }).getOrElse(fail("File not found"))
  }

  it should "act on class that extends given superclass" in {
    val parentFile = StringFileArtifact("src/main/java/NotRelevant.java", "public class NotRelevant {}")
    val childFile = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator extends NotRelevant {
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(childFile, parentFile))

    val (pkg, ann) = ("com.foo", "Baz")

    val program = getProgram("com/atomist/rug/kind/java13/ClassExtended.ts")

    val r = executeJava(program, "editors/ClassExtended.ts", as)
    val unupdatedParentFile = r.findFile(parentFile.path).get
    unupdatedParentFile shouldEqual parentFile

    val updatedChildFile = r.findFile(childFile.path).get
    updatedChildFile.content should include(s"import $pkg.$ann")
    updatedChildFile.content should include(s"@$ann")
  }

  it should "act on class that extends given abstract superclass" in {
    val parentFile = StringFileArtifact("src/main/java/NotRelevant.java", "public abstract class NotRelevant {}")
    val childFile = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public final class VeryCleverAbsquatulator extends NotRelevant {
        |}
      """.stripMargin)
    val as = new SimpleFileBasedArtifactSource("", Seq(childFile, parentFile))

    val (pkg, ann) = ("com.foo", "Baz")

    val program = getProgram("com/atomist/rug/kind/java14/ClassExtended.ts")
    val r = executeJava(program, "editors/ClassExtended.ts", as)

    val unupdatedParentFile = r.findFile(parentFile.path).get
    unupdatedParentFile shouldEqual parentFile

    val updatedChildFile = r.findFile(childFile.path).get
    updatedChildFile.content should include(s"import $pkg.$ann;")
    updatedChildFile.content should include(s"@$ann")
  }

  it should "add headers to files with long class names" in {
    val notRelevantFile = StringFileArtifact("src/main/java/NotRelevant.java", "public class NotRelevant {}")
    val impl = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator implements Absquatulator {
        |
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(notRelevantFile, impl))

    val newHeader = "It appears that Rod still likes long names"
    val program = getProgram("com/atomist/rug/kind/java15/CommentClassesWithLongNames.ts")
      .withPathAbove(".atomist/editors")
    assert(program.totalFileCount === 1)
    val r = executeJava(program, "editors/ClassAnnotated.ts", as)

    val unupdatedNotRelevantFile = r.findFile(notRelevantFile.path).get
    unupdatedNotRelevantFile shouldEqual notRelevantFile

    val updatedImpl = r.findFile(impl.path).get
    updatedImpl.content should include(newHeader)

    val newHeader1 = "It appears that Alan would prefer more concise class names"
    val program1 = getProgram("com/atomist/rug/kind/java16/ClassAnnotated.ts")

    val r1 = executeJava(program1, "editors/ClassAnnotated.ts", as)

    val unupdatedNotRelevantFile1 = r1.findFile(notRelevantFile.path).get
    unupdatedNotRelevantFile1 shouldEqual notRelevantFile

    val updatedImpl1 = r1.findFile(impl.path).get
    updatedImpl1.content should include(newHeader1)
    updatedImpl1.content shouldNot include(newHeader)
  }

  it should "distinguish interfaces from classes" in {
    val interfaceFile = StringFileArtifact("src/main/java/Absquatulator.java", "public interface Absquatulator {}")
    val impl = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator implements Absquatulator {
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(interfaceFile, impl))
    val program = getProgram("com/atomist/rug/kind/java17/ClassAnnotated.ts")
    val r = executeJava(program, "editors/ClassAnnotated.ts", as)

    val unchangedImpl = r.findFile(impl.path).get
    unchangedImpl.content contentEquals impl.content should be(true)

    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")
  }

  it should "distinguish abstract classes" in {
    val interfaceFile = StringFileArtifact("src/main/java/Absquatulated.java", "public interface Absquatulated {}")
    val abstractFile = StringFileArtifact("src/main/java/AbstractAbsquatulator.java", "public abstract class AbstractAbsquatulator implements Absquatulated {}")
    val concreteFile = StringFileArtifact("src/main/java/Absquatulator.java", "public class Absquatulator extends AbstractAbsquatulator {}")
    val as = new SimpleFileBasedArtifactSource("", Seq(interfaceFile, abstractFile, concreteFile))

    val program = getProgram("com/atomist/rug/kind/java18/AbstractClass.ts")
    val r = executeJava(program, "editors/AbstractClass.ts", as)
    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")

    val updatedAbstractClass = r.findFile(abstractFile.path).get
    updatedAbstractClass.content should include(s"import com.foo.bar.Baz;")
    updatedAbstractClass.content should include("@Baz")

    val unchangedConcrete = r.findFile(concreteFile.path).get
    unchangedConcrete.content contentEquals concreteFile.content should be(true)
  }

  it should "allow access to project" in {
    annotateClass(getProgram("com/atomist/rug/kind/java19/ClassAnnotated.ts"))
  }

  it should "annotate constructor" in {
    annotateClass(getProgram("com/atomist/rug/kind/java20/ClassAnnotated.ts"))
  }

  it should "annotate method" in {
    annotateClass(getProgram("com/atomist/rug/kind/java21/ClassAnnotated.ts"))
  }

  it should "remove annotation from method" in {
    val program = getProgram("com/atomist/rug/kind/java22/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include("import com.someone.FooBar;")
      f.content shouldNot include("@FooBar")
    }).getOrElse(fail("File not found"))
  }

  it should "annotate field" in {
    annotateClass(getProgram("com/atomist/rug/kind/java23/ClassAnnotated.ts"))
  }

  it should "remove annotation from field" in {
    val program = getProgram("com/atomist/rug/kind/java24/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include("import com.someone.ComFooBar;")
      f.content shouldNot include("@ComFooBar")
    }).getOrElse(fail("File not found"))
  }

  it should "annotate class" in {
    val program = getProgram("com/atomist/rug/kind/java25/ClassAnnotated.ts")
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include("@MyAnnotation")
      // println(f.content)
    }).getOrElse(fail("File not found"))
  }

  private def getProgram(editor: String) = {
    toArtifactSource(editor).withPathAbove(".atomist/editors")
  }

  private def annotateClass(program: ArtifactSource,
                            importStr: String = "import com.someone.FooBar;",
                            annotationStr: String = "@FooBar"): Unit =
    executeJava(program, "editors/ClassAnnotated.ts").findFile(SrcFile).map(f => {
      f.content.lines.size should be > 0
      f.content should include(importStr)
      f.content should include(annotationStr)
    }).getOrElse(fail("File not found"))
}
