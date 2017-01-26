package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpression, PathExpressionEngine, PathExpressionParser}
import org.scalatest.{FlatSpec, Matchers}

object CSharpFileTypeTest {

  val HelloWorld =
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

  val HelloWorldSources =
    SimpleFileBasedArtifactSource(StringFileArtifact("src/hello.cs", HelloWorld))

  val HelloWorldProject = new ProjectMutableView(EmptyArtifactSource(), HelloWorldSources)

  val ProjectWithBogusCSharp = new ProjectMutableView(EmptyArtifactSource(),
    HelloWorldSources + StringFileArtifact("bogus.cs", "And this is nothing like C#"))


}

class CSharpFileTypeTest extends FlatSpec with Matchers {

  import CSharpFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine

  it should "ignore ill-formed file without error" in {
    val cs = new CSharpFileType
    val csharps = cs.findAllIn(ProjectWithBogusCSharp)
    // Should have ignored the bogus file
    csharps.size should be (1)
  }

  it should "parse hello world" in {
    val cs = new CSharpFileType
    val csharps = cs.findAllIn(HelloWorldProject)
    csharps.size should be (1)
  }

  it should "find hello world using path expression" in {
    val expr = "/src//CSharpFile()"
    val rtn = ee.evaluate(HelloWorldProject, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

}
