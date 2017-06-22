package com.atomist.rug.kind.java

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaVerifier._
import com.atomist.source._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class JavaProjectMutableViewTest extends FlatSpec with Matchers {

  import JavaTypeUsageTest.NewSpringBootProject

  it should "handle javaFileCount" in {
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)
    jpv.javaFileCount should be > 1
  }

  it should "refuse to move package with invalid name" in {
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)
    val oldPackage = "com.atomist.test1"
    an[IllegalArgumentException] should be thrownBy jpv.renamePackage(oldPackage, "bogus.1.packagename")
  }

  it should "move package without subpackages" in {
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val oldPackage = "com.atomist.test1"
    jpv.packages.asScala.map(_.name).contains(oldPackage) shouldBe true
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    val newPackage = "com.whatever"
    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    val unchanged = jpv.currentBackingObject.allFiles.filter(f => f.name.endsWith(".java")).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }

  it should "move package with subpackages" in {
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val oldPackage = "com.atomist"
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    val newPackage = "com.whatever"
    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    jpv.currentBackingObject.allFiles.exists(_.content.contains("com.whatever.test1")) shouldBe true

    val unchanged = jpv.currentBackingObject.allFiles.filter(f => f.name.endsWith(".java")).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }

  // Attempt to reproduce problem from Neo editor
  it should "move added package with subpackages" in {
    val editorBackingAs = SimpleFileBasedArtifactSource(
      StringFileArtifact("src/main/java/com/foo/Bar.java",
        """
          |package com.foo;
          |public class Bar {
          |}
        """.stripMargin),
      StringFileArtifact("src/main/java/com/foo/baz/Baz.java",
        """
          |package com.foo.baz;
          |public class Baz {
          |}
        """.stripMargin)
    )

    val pmv = new ProjectMutableView(editorBackingAs, NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val copiedInDir = "src/main/java/com/foo"

    jpv.copyEditorBackingFilesPreservingPath(copiedInDir)

    val oldPackage = "com.foo"
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    val newPackage = "com.whatever"
    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    jpv.currentBackingObject.allFiles.exists(_.content.contains(newPackage)) shouldBe true
    jpv.directoryExists(copiedInDir) shouldBe false
    assert(jpv.currentBackingObject.findDirectory(copiedInDir).isDefined === false)

    val unchanged = jpv.currentBackingObject.allFiles.filter(f => f.name.endsWith(".java")).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }

  it should "move added package with subpackages into existing package" in {
    val editorBackingAs = SimpleFileBasedArtifactSource(
      StringFileArtifact("src/main/java/com/foo/Bar.java",
        """
          |package com.foo;
          |public class Bar {
          |}
        """.stripMargin),
      StringFileArtifact("src/main/java/com/foo/baz/Baz.java",
        """
          |package com.foo.baz;
          |public class Baz {
          |}
        """.stripMargin)
    )

    val pmv = new ProjectMutableView(editorBackingAs, NewSpringBootProject, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val copiedInDir = "src/main/java/com/foo"

    jpv.copyEditorBackingFileOrFail("src/main/java/com/foo/Bar.java")

    jpv.copyEditorBackingFilesPreservingPath(copiedInDir + "/baz")

    val oldPackage = "com.foo"
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    // The new package
    val newPackage = "com.atomist.test1"
    jpv.renamePackage(oldPackage + ".baz", newPackage + ".newbaz")

    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    jpv.currentBackingObject.allFiles.exists(_.content.contains(newPackage)) shouldBe true
    jpv.directoryExists(copiedInDir) shouldBe false
    jpv.currentBackingObject.findDirectory(copiedInDir) shouldBe empty

    val unchanged = jpv.currentBackingObject.allFiles.filter(f => f.name.endsWith(".java")).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }

  it should "update Java string constants on package move" in {
    val projectToUse = NewSpringBootProject + StringFileArtifact("src/main/java/com/foo/bar/Baz.java",
      """
        |package com.foo.bar;
        |
        |@SomeAnnotation("com.atomist.test1.Thing")
        |public class Baz {
        |}
      """.stripMargin)
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), projectToUse, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val oldPackage = "com.atomist"
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    val newPackage = "com.whatever"
    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    jpv.currentBackingObject.allFiles.exists(_.content.contains("com.whatever.test1")) shouldBe true

    val unchanged = jpv.currentBackingObject.allFiles.filter(f => f.name.endsWith(".java")).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file still references old package name ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }

  it should "update properties file reference on package move" in {
    val newFile = StringFileArtifact("src/main/resources/application.properties",
      """
        |the_class=com.atomist.test1.Whatever
      """.stripMargin)
    updateOtherFileRefOnPackageMove(newFile)
  }

  it should "update XML file reference on package move" in {
    val newFile = StringFileArtifact("src/main/resources/beans.xml",
      """
        |<node>com.atomist.test1.Whatever</node>
      """.stripMargin)
    updateOtherFileRefOnPackageMove(newFile)
  }

  it should "update YAML file reference on package move" in {
    val newFile = StringFileArtifact("src/main/resources/thing.yml",
      """
        |thing: com.atomist.test1
      """.stripMargin)
    updateOtherFileRefOnPackageMove(newFile)
  }

  private  def updateOtherFileRefOnPackageMove(newFile: FileArtifact): Unit = {
    val projectToUse = NewSpringBootProject + newFile
    val pmv = new ProjectMutableView(new EmptyArtifactSource(""), projectToUse, DefaultAtomistConfig)
    val jpv = new JavaProjectMutableView(pmv)

    val oldPackage = "com.atomist"
    jpv.currentBackingObject.allFiles.exists(_.content.contains(oldPackage)) shouldBe true

    val newPackage = "com.whatever"
    jpv.renamePackage(oldPackage, newPackage)
    assert(jpv.dirty === true)

    jpv.currentBackingObject.allFiles.exists(_.content.contains("com.whatever.test1")) shouldBe true

    import com.atomist.rug.kind.core.ProjectType._
    val unchanged = jpv.currentBackingObject.allFiles.filter(f => !f.path.equals(ProvenanceFilePath)).find(_.content.contains(oldPackage))
    if (unchanged.nonEmpty) {
      fail(s"Offending file still references old package name ${unchanged.get.path}\n${unchanged.get.content}")
    }
    verifyJavaIsWellFormed(jpv.currentBackingObject)
  }
}
