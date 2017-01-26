package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class CSharpFileTypeTest extends FlatSpec with Matchers {

  it should "handle ill-formed file correctly" is pending

  it should "parse hello world" in {
    val hello =
      """
        |// A Hello World! program in C#.
        |using System;
        |namespace HelloWorld
        |{
        |    class Hello
        |    {
        |        static void Main()
        |        {
        |            Console.WriteLine("Hello World!");
        |
        |            // Keep the console window open in debug mode.
        |            Console.WriteLine("Press any key to exit.");
        |            Console.ReadKey();
        |        }
        |    }
        |}
      """.stripMargin
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("hello.cs", hello))
    val pmv = new ProjectMutableView(EmptyArtifactSource(), as)
    val cs = new CSharpFileType
    cs.findAllIn(pmv).size should be (1)
  }

}
