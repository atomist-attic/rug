package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpression, PathExpressionEngine, PathExpressionParser}
import org.scalatest.{FlatSpec, Matchers}

class CSharpFileTypeTest extends FlatSpec with Matchers {

  val ee: ExpressionEngine = new PathExpressionEngine

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

  it should "handle ill-formed file correctly" is pending

  it should "parse hello world" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("hello.cs", hello))
    val pmv = new ProjectMutableView(EmptyArtifactSource(), as)
    val cs = new CSharpFileType
    val csharps = cs.findAllIn(pmv)
    csharps.size should be (1)
  }

  it should "find hello world using path expression" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("src/main/hello.cs", hello))
    val pmv = new ProjectMutableView(EmptyArtifactSource(), as)
    val expr = "/src//CSharpFile()"
    val rtn = ee.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

}
