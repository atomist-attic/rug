package com.atomist.rug.kind.java

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug._
import com.atomist.rug.kind.java.JavaVerifier._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import com.atomist.source.file.ClassPathArtifactSource
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

object JavaTypeUsageTest extends Matchers {

  val NewSpringBootProject: ArtifactSource =
    ClassPathArtifactSource.toArtifactSource("./springboot1")

  val JavaAndText: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("pom.xml", "<maven></maven"),
      StringFileArtifact("/src/main/java/Dog.java",
        """
          |import java.util.Set;
          |import com.foo.Bar;
          |import com.someone.ComFooBar;
          |import com.someone.FooBar;
          |
          |@Bar
          |class Dog {
          |
          |   @ComFooBar
          |   private String stringField;
          |
          |   public Dog() {}
          |
          |   public Dog(String stringField) {
          |     this.stringField = stringField;
          |   }
          |
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
                      as: ArtifactSource, poa: Map[String,String]): ModificationAttempt = {

    //println(ArtifactSourceUtils.prettyListFiles(program))
    val progAs = TypeScriptBuilder.compileWithModel(program)
    val eds = SimpleJavaScriptProjectOperationFinder.find(progAs)
    val pe = eds.editors.head
    pe.modify(as, SimpleParameterValues(poa))
  }
}

class JavaTypeUsageTest extends FlatSpec with Matchers with LazyLogging {

  import JavaTypeUsageTest._

  it should "find boot package" in {
    TestUtils.editorInSideFile(this, "PackageFinder.ts").modify(NewSpringBootProject) match {
      case nmn: NoModificationNeeded => // Ok
       // Ok
      case _ => ???
    }
  }

  it should "annotate class using function" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java1/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    annotateClass(program)
  }

  it should "annotate class going straight to class without enclosing JavaSource" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java2/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    annotateClass(program)
  }

  it should "add an annotation with properties to class" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java3/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val result = executeJava(program, "editors/ClassAnnotated.ts")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import org.junit.jupiter.api.extension.ExtendWith;")
    f.content should include("@ExtendWith(value = SpringExtension.class)")
  }

  it should "remove annotation from class" in {
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java4/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val result = executeJava(program, "editors/ClassAnnotated.ts")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.foo.Bar;")
    f.content shouldNot include("@Bar")
  }

  it should "remove one annotation only from class" in {
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java5/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val result = executeJava(program, "editors/ClassAnnotated.ts")
    val f = result.findFile("src/test/java/SpringBootJunit5ApplicationTests.java").get

    f.content.lines.size should be > 0
    f.content shouldNot include("@RunWith")
    f.content should include("@SpringBootTest")
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
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java6/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.ts", as)
    result.allFiles.foreach(f =>
      logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content should include("package com.atomist;")
  }

  it should "repackage class and verify explicitly importing users are updated" in pendingUntilFixed {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java7/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val as = new SimpleFileBasedArtifactSource("", Seq(dog, cat, squirrel))

    val result = executeJava(program,"editors/ClassAnnotated.ts",  as)
    result.allFiles.foreach(f => logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content.contains("package com.atomist;") should be(true)

    // Should now import Dog
    f.content should include("import com.atomist.Dog;")
  }

  it should "rename class and verify name" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java8/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.ts", as)

    val f = result.findFile("src/main/java/com/foo/bar/Dingo.java").get
    assert(result.findFile(dog.path).isDefined === false)
    f.content should include("class Dingo")
  }

  it should "verify usages of renamed class are updated" is pending

  private  def annotateClass(program: ArtifactSource): Unit = {
    val result = executeJava(program, "editors/ClassAnnotated.ts")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("@FooBar")
  }

  it should "add import" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java9/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program, "editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get
    f.content.lines.size should be > 0
    f.content should include("import java.util.List")
  }

  it should "add and remove import" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java10/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program, "editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content shouldNot include("import java.util.List")
  }

  it should "remove an import" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java11/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program, "editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content shouldNot include("import java.util.Set")
  }

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

    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java12/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val r = executeJava(program, "editors/ClassAnnotated.ts", as)
    val f = r.findFile(impl.path).get
    f.content.lines.size should be > 0
    f.content shouldNot include(s"import $pkg.$ann")
    f.content should include(s"@$ann")
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

    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java13/ClassExtended.ts").withPathAbove(".atomist/editors")

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

    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java14/ClassExtended.ts").withPathAbove(".atomist/editors")

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
    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java15/CommentClassesWithLongNames.ts").withPathAbove(".atomist/editors")
    assert(program.totalFileCount === 1)
    val r = executeJava(program, "editors/ClassAnnotated.ts", as)

    val unupdatedNotRelevantFile = r.findFile(notRelevantFile.path).get
    unupdatedNotRelevantFile shouldEqual notRelevantFile

    val updatedImpl = r.findFile(impl.path).get
    updatedImpl.content should include(newHeader)

    val newHeader1 = "It appears that Alan would prefer more concise class names"
    val program1 =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java16/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val r1 = executeJava(program1,"editors/ClassAnnotated.ts", as)

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

    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java17/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val r = executeJava(program,"editors/ClassAnnotated.ts", as)

    val unchangedImpl = r.findFile(impl.path).get
    unchangedImpl.content contentEquals impl.content should be (true)

    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")
  }

  it should "distinguish abstract classes" in pendingUntilFixed { /* broken in TypeScript conversion */
    val interfaceFile = StringFileArtifact("src/main/java/Absquatulated.java", "public interface Absquatulated {}")
    val abstractFile = StringFileArtifact("src/main/java/AbstractAbsquatulator.java", "public abstract class AbstractAbsquatulator implements Absquatulated {}")
    val concreteFile = StringFileArtifact("src/main/java/Absquatulator.java", "public class Absquatulator extends AbstractAbsquatulator {}")
    val as = new SimpleFileBasedArtifactSource("", Seq(interfaceFile, abstractFile, concreteFile))

    val program =
      ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java18/AbstractClass.ts").withPathAbove(".atomist/editors")

    val r = executeJava(program, "editors/AbstractClass.ts", as)

    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")

    val updatedAbstractClass = r.findFile(abstractFile.path).get
    updatedAbstractClass.content should include(s"import com.foo.bar.Baz;")
    updatedAbstractClass.content should include("@Baz")

    val unchangedConcrete = r.findFile(concreteFile.path).get
    unchangedConcrete.content contentEquals concreteFile.content should be (true)
  }

  it should "allow access to project" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java19/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  // TODO issue is commit is not being called...apparently proxying doesn't go
  // all the way down
  it should "annotate constructor" in pendingUntilFixed {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java20/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get
    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "annotate method" in {
    val program = ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java21/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get
    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "remove annotation from method" in pendingUntilFixed { /* broken in TypeScript conversion */
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java22/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content shouldNot include("@FooBar")
  }

  // TODO TypeScript breakage
  it should "annotate field" in pendingUntilFixed {
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java23/ClassAnnotated.ts").withPathAbove(".atomist/editors")
    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get
    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "remove annotation from field" in pendingUntilFixed { /* broken in TypeScript conversion */
    val program =ClassPathArtifactSource.toArtifactSource("com/atomist/rug/kind/java24/ClassAnnotated.ts").withPathAbove(".atomist/editors")

    val r = executeJava(program,"editors/ClassAnnotated.ts")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.ComFooBar;")
    f.content shouldNot include("@ComFooBar")
  }
}
