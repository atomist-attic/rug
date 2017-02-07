package com.atomist.rug.kind.elm

import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class ElmUpgradeProgramTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

  it should "add an exposing to an existing import" in {
    val elm =
      """module Main exposing (..)
        |
        |import Foo
        |""".stripMargin
    val editor =
      """editor Whoever
        |
        |with Project
        |  with ElmModule m
        |    with import i when module="Foo"
        |      do addExposure "fooFunction"
      """.stripMargin

    val source =
      StringFileArtifact("Main.elm", elm)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), editor)

    r.findFile("Main.elm").value.content.lines.find(_.contains("import Foo")).headOption.value should equal("import Foo exposing (fooFunction)")

  }

  it should "find a module that exposes a thing explicitly" in {

    val elm =
      """module Main exposing (main)
        |
        |main = "yay"
      """.stripMargin

    val editor=
      """editor Whatever
        |
        |with Project
        |  with ElmModule m when m.exposes("main")
        |    do rename "Carrot"
      """.stripMargin

    val source =
      StringFileArtifact("Main.elm", elm)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), editor)

    r.findFile("Carrot.elm").value.content should include("module Carrot")
  }

  it should "find a module that exposes all things and defines this one" in {
    val elm=
      """module Main exposing (..)
        |
        |main = "Foo"
      """.stripMargin

    val editor=
      """editor Whatever
        |
        |with Project
        |  with ElmModule m when m.exposes("main")
        |    do rename "Carrot"
      """.stripMargin

    val source =
      StringFileArtifact("Main.elm", elm)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), editor)

    r.findFile("Carrot.elm").value.content should include("module Carrot")
  }

  it should "find not a module that does not expose the thing" in {

    val elm =
      """module Main exposing (main)
        |
        |main = "yay"
      """.stripMargin

    val editor=
      """editor Whatever
        |
        |with Project
        |  with ElmModule m when m.exposes("armadillo")
        |    do renameModule "Carrot"
      """.stripMargin

    val source =
      StringFileArtifact("Main.elm", elm)
    an[ElmTypeUsageTest.TestDidNotModifyException] should be thrownBy {
      elmExecute(new SimpleFileBasedArtifactSource("", source), editor)
    }
  }

  it should "not find a module that exposes everything but does define the thing" in {

    val elm =
      """module Main exposing (..)
        |
        |main = "yay"
      """.stripMargin

    val editor=
      """editor Whatever
        |
        |with Project
        |  with ElmModule m when m.exposes("armadillo")
        |    do renameModule "Carrot"
      """.stripMargin

    val source =
      StringFileArtifact("Main.elm", elm)

    an[ElmTypeUsageTest.TestDidNotModifyException] should be thrownBy {
      elmExecute(new SimpleFileBasedArtifactSource("", source), editor)
    }
  }

  it should "upgrade a beginner program" in {
    // Can we bring all these things into one operation?
    // or we can break them out into separate editors if that is necessary.
    val prog =
    """editor UpgradeMainFunction
      |
      |# main
      |with ElmModule when name = 'Main'
      |  begin
      |    with function f when name = 'main'
      |      do replaceBody
      |       { return "Html.App.program\n        { init = init\n        , subscriptions = subscriptions\n        , update = update\n        , view = view\n        }\n" }
      |   end
      |
      |""".stripMargin

    val source =
      StringFileArtifact("Main.elm", ElmParserTest.BeginnerProgram)
    val r1 = elmExecute(new SimpleFileBasedArtifactSource("", source), prog)

    val prog2 =
      """
        |editor UpgradeInit
        |with ElmModule when name = 'Main'
        |  with function f when name = 'model'
        |  begin
        |    do changeType '( Model, Cmd Msg )'
        |    do rename 'init'
        |    do replaceBody { f.body() + " ! []" }
        |  end
        | """.stripMargin

    val r2 = elmExecute(r1, prog2)

    // What I really want this to do is add this before `-- UPDATE`
    val prog3 =
    """editor AddSubscriptionsFunction
      |with ElmModule when name = 'Main' begin
      |  do addFunction { '\n-- SUBSCRIPTIONS\n\n\nsubscriptions model =\n    Sub.none\n\n' }
      |end
      | """.stripMargin

    val r3 = elmExecute(r2, prog3)

    val prog4 =
      """editor UpgradeUpdate
        |with ElmModule when name = 'Main'
        |  # use alias f to avoid issues with JavaScript reserved word
        |  with function f when name = 'update'
        |    do changeType 'Msg -> Model -> ( Model, Cmd Msg )'
        |
        |with ElmModule when name = 'Main'
        |  with function f when name = 'update'
        |    with case cc # when match = 'msg'
        |      begin
        |         do replaceBody { cc.body() + " ! []" }
        |      end
        |
      """.stripMargin

    val r = elmExecute(r3, prog4)
    val content = r.findFile("Main.elm").value.content

    // TODO should really bring this back, but there appears to be an ordering thing and
    // I'm not sure you've implemented all necessary edits
    //content should equal(ElmParserTest.AdvancedProgram)
    pending
  }

}
