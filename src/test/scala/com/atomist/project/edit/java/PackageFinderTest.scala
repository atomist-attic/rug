package com.atomist.project.edit.java

import com.atomist.rug.kind.java.support.{PackageFinder, PackageInfo}
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class PackageFinderTest extends FlatSpec with Matchers {

  it should "find default package in no-package source" in {
    val src = "public class Hello {}"
    PackageFinder.findPackage(src) should equal("")
  }

  it should "find non default package on first line" in {
    val src = "package com.foo.bar;\npublic class Hello {}"
    PackageFinder.findPackage(src) should equal("com.foo.bar")
  }

  it should "find non default package after comment" in {
    val src =
      """
        |/*
        |This is a comment
        |*/
        |
        |package com.foo.bar;
        |
        |public class Hello {
        |}
      """.stripMargin
    PackageFinder.findPackage(src) should equal("com.foo.bar")
  }

  it should "find no packages in empty project" in {
    PackageFinder.packages(new EmptyArtifactSource("")) should be(empty)
  }

  it should "find default package in root" in {
    PackageFinder.packages(
      new SimpleFileBasedArtifactSource("", StringFileArtifact("Hello.java", "public class Hello {}"))
    ) should equal(Seq(PackageInfo("", 1)))
  }

  it should "find explicit package in root" in {
    PackageFinder.packages(
      new SimpleFileBasedArtifactSource("", StringFileArtifact("com/foo/bar/Hello.java", "package com.foo.bar;\npublic class Hello {}"))
    ) should equal(Seq(PackageInfo("com.foo.bar", 1)))
  }

  it should "find and count explicit packages in root" in {
    PackageFinder.packages(
      new SimpleFileBasedArtifactSource("",
        Seq(
          StringFileArtifact("com/foo/bar/Hello.java", "package com.foo.bar;\npublic class Hello {}"),
          StringFileArtifact("com/foo/bar/Hello2.java", "package com.foo.bar;\npublic class Hello2 {}")
        ))
    ) should equal(Seq(PackageInfo("com.foo.bar", 2)))
  }

}
