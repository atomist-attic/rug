package com.atomist.rug.kind.grammar

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class MicrogrammarTypeUsageTest extends FlatSpec with Matchers {

  it should "modify with simple grammar" in {
    val filename = "people.txt"
    val prog =
      """
         |editor Turn21
         |
         |let ageGrammar = <
         |   NAME : [A-Z][a-z]+;
         |   AGE : [0-9]+;
         |   expr : name=NAME 'was aged' age=AGE;
         |>
         |
         |with file f when name = "people.txt"
         |	with ageGrammar g begin
         |   do set "age" "21"
         |   do set "name" { g.valueOf("name").toUpperCase() }
         | end
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename,
        """
          |David was aged 14.
          |Alice was aged 25.
          |Zhou was aged 41.
          |This is a load of unrelated bollocks, but Janaki was aged 14
          |and Greg isn't relevant.
        """.stripMargin))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content.contains("ALICE was aged 21") should be(true)
        df.content.contains("DAVID was aged 21") should be(true)
        df.content.contains("ZHOU was aged 21") should be(true)
        df.content.contains("JANAKI was aged 21") should be(true)
    }
  }

  val scalaMethodG4 =
    """
      | IDENTIFIER : [a-zA-Z][a-zA-Z0-9]*;
      | LPAREN : '(';
      | RPAREN : ')';
      | param : name=IDENTIFIER ':' type=IDENTIFIER;
      | params : param (',' param)*;
      | method : 'def' name=IDENTIFIER LPAREN params? RPAREN ':' type=IDENTIFIER;
    """.stripMargin

  it should "modify with grammar with rep" in {
    val filename = "Test.scala"
    val prog =
      s"""
         |editor Turn21
         |
         |let scalaMethod = <
         |   $scalaMethodG4
         |>
         |
         |with file f when name = "$filename"
         |	with scalaMethod m begin
         |   #do set "name" "bar"
         |  do set "name" { m.valueOf("name").toUpperCase() }
         | end
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val oldContent = """
                     |def foo(): Unit
                     |
                     |def bar(): Int
                     |
                     |def twoParams(a: Int, b: String): Unit
                   """.stripMargin
    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename, oldContent))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content.contains("def FOO") should be(true)
        df.content.contains("def TWOPARAMS") should be(true)
        df.content should equal (oldContent.replace("foo", "FOO").replace("bar", "BAR").replace("twoParams", "TWOPARAMS"))
    }
  }

  it should "modify with grammar with nested with" in {
    val filename = "Test.scala"
    val prog =
      s"""
         |editor Turn21
         |
         |let scalaMethod = <
         |   $scalaMethodG4
         |>
         |
         |with file f when name = "$filename"
         |	with scalaMethod m begin
         |     with params prms
         |        with param p
         |          do set "name" { p.valueOf("name").toUpperCase() }
         | end
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename,
        """
          |def bar(a: Int, b: String): Int
        """.stripMargin))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content.contains("A: Int") should be(true)
        df.content.contains("B: String") should be(true)
        // Shouldn't have updated a's anywhere else
        df.content.contains("bAr(") should be(false)
    }
  }

  it should "modify with grammar with nested with and empty collection" in {
    val filename = "Test.scala"
    val prog =
      s"""
         |editor Turn21
         |
         |let scalaMethod = <
         |   $scalaMethodG4
         |>
         |
         |with file f when name = "$filename"
         |	with scalaMethod m begin
         |     with params prms
         |        with param p
         |          do set "name" { p.valueOf("name").toUpperCase() }
         | end
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename,
        """
          |def foo(): Unit
          |
          |def bar(a: Int, b: String): Int
        """.stripMargin))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content.contains("A: Int") should be(true)
        df.content.contains("B: String") should be(true)
        // Shouldn't have updated a's anywhere else
        df.content.contains("bAr(") should be(false)
    }
  }

  it should "insert using exposed position" in {
    val filename = "Test.scala"
    val newContent = "a: Int"
    val prog =
      s"""
         |editor Turn21
         |
         |let scalaMethod = <
         |  IDENTIFIER : [a-zA-Z][a-zA-Z0-9]+;
         |  PLACEHOLDER : [];
         |  method : 'def' name=IDENTIFIER '(' insert=PLACEHOLDER ')' ':' type=IDENTIFIER;
         |>
         |
         |with file f when name = "$filename"
         |	with scalaMethod m begin
         |     with insert p begin
         |        do setValue "$newContent"
         |     end
         | end
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val input = "def foo(): Unit"
    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename, input))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content should equal (s"def foo($newContent): Unit")
    }
  }

  it should "insert ruby class in module" in {
    val filename = "services.rb"
    val fileContent =
      """
        |module SpaceCommander
        |
        |  module Servers
        |
        |   class HensontestboxServer < StripeServer
        |      host_type 'hensontestbox'
        |      contact 'services'
        |
        |      release_code 'trusty'
        |
        |      qa_instance_type 'm3.large'
        |      prod_instance_type 'm3.large'
        |    end
        |
        |  end
        |end
      """.stripMargin
    val newContent = "class X\n\n"
    val prog =
      s"""
         |editor AddNewClass
         |
         |let firstRubyClassInModule = <
         | ID : [a-zA-Z_][a-zA-Z0-9_]*;
         | PLACEHOLDER : [];
         | first_class : 'module' name=ID ip=PLACEHOLDER 'class';
         |>
         |
         |with file f when name = "$filename"
         |	with firstRubyClassInModule c
         |     with ip p
         |        do setValue '$newContent'
         |
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(filename, fileContent))
    ed.modify(target,
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(filename).get
        df.content.contains(newContent) should be (true)
    }
  }

}
