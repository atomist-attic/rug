package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.ConsoleMatchListener
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
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

  val EmptyClass1 = StringFileArtifact("EmptyClass1.cs",
    """
      |using System;
      |namespace EmptyClass
      |{
      |    class EmptyClass1 {}
      |}
    """.stripMargin)

  val EmptyClass2 = StringFileArtifact("EmptyClass2.cs",
    """
      |
      |using System;
      |namespace EmptyClass
      |{
      |    class EmptyClass2 {}
      |}
    """.stripMargin)

  val SingleMethodInClass = StringFileArtifact("SingleMethodInClass.cs",
    """
      |using System;
      |namespace HelloWorld
      |{
      |    class Hello
      |    {
      |        public string Echo(string message)
      |        {
      |           return message;
      |        }
      |    }
      |}
    """.stripMargin)

  val ManyMethodsInClass = StringFileArtifact("ManyMethodsInClass.cs",
    """
      |using System;
      |namespace HelloWorld
      |{
      |    class Hello
      |    {
      |        public string Echo(string message)
      |        {
      |           return message;
      |        }
      |
      |        public string TwoArgs(string a, string b)
      |        {
      |        }
      |
      |        private string VariableArgs(params string[] names)
      |        {
      |        }
      |    }
      |}
    """.stripMargin)

  val ManyUsingDirectives = StringFileArtifact("ManyUsingDirectives.cs",
    """
      |using System.Text;
      |using static System.Math;
      |
      |namespace HelloWorld
      |{
      |     class Hello {}
      |}
    """.stripMargin)


  val SingleLambdaExpression = StringFileArtifact("SingleLambdaExpression.cs",
    """
      |using System;
      |
      |namespace HelloWorld
      |{
      |     class HelloWorld
      |     {
      |        public void AddOne(int x) {
      |           Func<int, int> addOne = x => x + 1;
      |           return addOne(x);
      |        }
      |     }
      |}
    """.stripMargin)

  val HelloWorldSources =
    SimpleFileBasedArtifactSource(StringFileArtifact("src/hello.cs", HelloWorld))

  def helloWorldProject = new ProjectMutableView(EmptyArtifactSource(), HelloWorldSources)

  def projectWithBogusCSharp = new ProjectMutableView(EmptyArtifactSource(),
    HelloWorldSources + StringFileArtifact("bogus.cs", "And this is nothing like C#"))

  def exceptionProject =
    SimpleFileBasedArtifactSource(StringFileArtifact("src/exception.cs", Exceptions))

  def manyClassesProject =
    new ProjectMutableView(EmptyArtifactSource(),
      new SimpleFileBasedArtifactSource("name",
        Seq(EmptyClass1, EmptyClass2)
      )
    )

  def manyMethodsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(ManyMethodsInClass))

  def singleMethodProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(SingleMethodInClass))

  def manyUsingDirectivesProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(ManyUsingDirectives))

  def singleLambdaExpressionProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(SingleLambdaExpression))
}

class CSharpFileTypeTest extends AbstractTypeUnderFileTest {

  import CSharpFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine

  val typeBeingTested = new CSharpFileType

  it should "ignore ill-formed file without error" in {
    val cs = new CSharpFileType
    val csharps = cs.findAllIn(projectWithBogusCSharp)
    // Should have silently ignored the bogus file
    assert(csharps.size === 1)
  }

  it should "parse hello world" in {
    val csharps = typeBeingTested.findAllIn(helloWorldProject)
    assert(csharps.size === 1)
  }

  it should "parse hello world and write out correctly" in {
    val parsedValue = parseAndPad(StringFileArtifact("HelloWorld.cs", HelloWorld))
    withClue(s"Unexpected content: [$parsedValue]") {
      parsedValue should equal(helloWorldProject.files.get(0).content)
    }
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val csharps = typeBeingTested.findAllIn(helloWorldProject)
    val initialContent = helloWorldProject.files.get(0).content
    assert(csharps.size === 1)
    csharps.head.head match {
      case mtn =>
        val content = helloWorldProject.files.get(0).content
        content should equal(initialContent)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "/src//CSharpFile()"
    val rtn = ee.evaluate(helloWorldProject, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find specification exception class" in {
    val csharps: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(new ProjectMutableView(EmptyArtifactSource(), exceptionProject))
    assert(csharps.size === 1)
    val csharpFileNode = csharps.get.head

    val expr = "//specific_catch_clause//class_type[@value='IndexOutOfRangeException']"
    ee.evaluate(csharpFileNode, PathExpressionParser.parseString(expr), DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
    
    }

    assert(csharpFileNode.value === Exceptions)
  }

  it should "find file that catches exception class" in {
    val project = new ProjectMutableView(EmptyArtifactSource(), exceptionProject)
    val expr = "/src//*[CSharpFile()//specific_catch_clause//class_type[@value='IndexOutOfRangeException']]"
    ee.evaluate(project, PathExpressionParser.parseString(expr), DefaultTypeRegistry) match {
      case Right(Seq(fileCatchingIndexOutOfRange: FileArtifactBackedMutableView)) =>
        assert(fileCatchingIndexOutOfRange.path === exceptionProject.allFiles.head.path)
    }
  }

  it should "map class_definition to Class" in {
    val expr1 = "/File()//CSharpFile()//Class()"
    val rtn1 = expressionEngine.evaluate(manyClassesProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()//CSharpFile()//class_definition()"
    val rtn2 = expressionEngine.evaluate(manyClassesProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map method_declaration to Func" in {
    val expr1 = "/File()/CSharpFile()//Method()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 3)

    val expr2 = "/File()/CSharpFile()//method_declaration()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 3)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map fixed_parameter to Args" in {
    val expr1 = "/File()/CSharpFile()//Method()[/method_member_name/identifier/IDENTIFIER[@value='TwoArgs']]//Args()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()/CSharpFile()//method_declaration()[/method_member_name/identifier/IDENTIFIER[@value='TwoArgs']]//Args()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    val expr3 = "/File()/CSharpFile()//method_declaration()[/method_member_name/identifier/IDENTIFIER[@value='TwoArgs']]//fixed_parameter()"
    val rtn3 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr3), DefaultTypeRegistry)
    assert(rtn3.right.get.size === 2)
  }


  it should "map variable parameter_array to Args" in {
    val expr1 = "/File()/CSharpFile()//Method()[/method_member_name/identifier/IDENTIFIER[@value='VariableArgs']]//VarArgs()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/CSharpFile()//method_declaration()[/method_member_name/identifier/IDENTIFIER[@value='VariableArgs']]//VarArgs()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    val expr3 = "/File()/CSharpFile()//method_declaration()[/method_member_name/identifier/IDENTIFIER[@value='VariableArgs']]//parameter_array()"
    val rtn3 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr3), DefaultTypeRegistry)
    assert(rtn3.right.get.size === 1)

  }

  it should "map lambda_expression to Lambda" in {
    val expr1 = "/File()/CSharpFile()//Lambda()"
    val rtn1 = expressionEngine.evaluate(singleLambdaExpressionProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/CSharpFile()//lambda_expression()"
    val rtn2 = expressionEngine.evaluate(singleLambdaExpressionProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map class_definition and method_declaration to Class and Method" in {
    val expr1 = "/File()/CSharpFile()//Class()//Method()"
    val rtn1 = expressionEngine.evaluate(singleMethodProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/CSharpFile()//class_definition()//method_declaration()"
    val rtn2 = expressionEngine.evaluate(singleMethodProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map using_directive to Using" in {
    val expr1 = "/File()/CSharpFile()//Using()"
    val rtn1 = expressionEngine.evaluate(manyUsingDirectivesProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()/CSharpFile()//using_directive()"
    val rtn2 = expressionEngine.evaluate(manyUsingDirectivesProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }
}
