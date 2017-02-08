package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.ConsoleMatchListener
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine, PathExpressionParser}
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

  val Exceptions =
    """
      |/*
      | * C# Program to Demonstrate IndexOutOfRange Exception
      | */
      |using System;
      |using System.Collections.Generic;
      |using System.Linq;
      |using System.Text;
      |namespace differnce
      |{
      |    class arrayoutofindex
      |    {
      |        public void calculatedifference()
      |        {
      |            int difference=0;
      |            int [] number= new int[5] {1,2,3,4,5};
      |            try
      |            {
      |                for (int init =1; init <=5; init++)
      |                {
      |                    difference= difference -  number[init];
      |                }
      |                Console.WriteLine("The difference of the array is:" + difference);
      |            }
      |            catch (IndexOutOfRangeException e)
      |            {
      |                Console.WriteLine(e.Message);
      |            }
      |            //catch
      |            //{
      |            //    Console.WriteLine("Catch it")
      |            //}
      |        }
      |    }
      |    class classmain
      |    {
      |        static void Main(string [] args)
      |    {
      |        arrayoutofindex obj = new arrayoutofindex();
      |        obj.calculatedifference();
      |        Console.ReadLine();
      |    }
      |}
      |}
    """.stripMargin

  val HelloWorldSources =
    SimpleFileBasedArtifactSource(StringFileArtifact("src/hello.cs", HelloWorld))

  def helloWorldProject = new ProjectMutableView(EmptyArtifactSource(), HelloWorldSources)

  def projectWithBogusCSharp = new ProjectMutableView(EmptyArtifactSource(),
    HelloWorldSources + StringFileArtifact("bogus.cs", "And this is nothing like C#"))

  def exceptionProject =
    SimpleFileBasedArtifactSource(StringFileArtifact("src/exception.cs", Exceptions))

}

class CSharpFileTypeTest extends FlatSpec with Matchers {

  import CSharpFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine

  val csFileType = new CSharpFileType

  it should "ignore ill-formed file without error" in {
    val cs = new CSharpFileType
    val csharps = cs.findAllIn(projectWithBogusCSharp)
    // Should have silently ignored the bogus file
    csharps.size should be(1)
  }

  it should "parse hello world" in {
    val csharps = csFileType.findAllIn(helloWorldProject)
    csharps.size should be(1)
  }

  it should "parse hello world and write out correctly" in {
    val parsed = csFileType.fileToRawNode(StringFileArtifact("HelloWorld.cs", HelloWorld), Some(ConsoleMatchListener)).get
    val parsedValue = parsed.value
    withClue(s"Unexpected content: [$parsedValue]") {
      parsedValue should equal(helloWorldProject.files.get(0).content)
    }
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val csharps = csFileType.findAllIn(helloWorldProject)
    csharps.size should be(1)
    csharps.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        content should equal(helloWorldProject.files.get(0).content)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "/src//CSharpFile()"
    val rtn = ee.evaluate(helloWorldProject, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "find specification exception class" in {
    val csharps: Option[Seq[TreeNode]] = csFileType.findAllIn(new ProjectMutableView(EmptyArtifactSource(), exceptionProject))
    csharps.size should be(1)
    val csharpFileNode = csharps.get.head.asInstanceOf[MutableContainerMutableView]

    val expr = "//specific_catch_clause//class_type[@value='IndexOutOfRangeException']"
    ee.evaluate(csharpFileNode, PathExpressionParser.parseString(expr), DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
    }

    csharpFileNode.value should equal(Exceptions)
  }

  it should "find file that catches exception class" in {
    val project = new ProjectMutableView(EmptyArtifactSource(), exceptionProject)
    val expr = "/src//*[CSharpFile()//specific_catch_clause//class_type[@value='IndexOutOfRangeException']]"
    ee.evaluate(project, PathExpressionParser.parseString(expr), DefaultTypeRegistry) match {
      case Right(Seq(fileCatchingIndexOutOfRange: FileArtifactBackedMutableView)) =>
        fileCatchingIndexOutOfRange.path should equal(exceptionProject.allFiles.head.path)
    }
  }
}
