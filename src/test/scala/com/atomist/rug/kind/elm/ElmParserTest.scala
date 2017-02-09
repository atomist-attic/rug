package com.atomist.rug.kind.elm

import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels.ElmTuple
import com.atomist.rug.kind.elm.ElmModel._
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.util.Utils.StringImprovements
import org.scalatest.{FlatSpec, Matchers}

class ElmParserTest extends FlatSpec with Matchers {

//  it should "not losing trailing non-empty newline when marking up source" in checkMarkingUp(
//    "module Todo exposing (..)\n ")

  it should "not lose trailing empty newline when marking up source" in checkMarkingUp(
    "module Todo exposing (..)\n")

  private  def checkMarkingUp(starterSource: String) {
    val markedUp = ElmParser.markLinesThatAreLessIndented(starterSource)
    val unmarkedUp = ElmParser.unmark(markedUp)
    withClue(s"starter=\n[$starterSource], markedUp=\n[$unmarkedUp]") {
      unmarkedUp should equal(starterSource)
    }
  }

  def parseAndVerifyCanWriteOutFromParsedStructureUnchanged(elm: String): ElmModule = {
    val em = ElmParser.parse(elm)
    // TODO shouldn't need trim
    val expected = elm.trim.toSystem
    val actual = ElmParser.unmark(em.value.trim)
    withClue(s"[$actual]\n did not equal \n[$expected]\n:${TreeNodeUtils.toShortString(em)}") {
      actual should equal(expected)
    }
    em
  }

  val restOfModule = "\n\n"

  it should "parse module with .." in {
    val input: String = "module Todo exposing (..)" + restOfModule
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    assert(em.nodeName === "Todo")
    em.exposing.isInstanceOf[AllExposing]
  }

  it should "parse module with function names and type names" in {
    val input: String = "module Todo exposing (Banana, carrot)" + restOfModule
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    assert(em.nodeName === "Todo")
    val exposedNamesAsStrings = em.exposing match {
      case mttns : FunctionNamesExposing => mttns.names.map(_.value)
      case _ => ???
    }
    exposedNamesAsStrings should equal(Seq("Banana", "carrot"))
  }

  val someValidModuleHeader = "module Carrot exposing (..)"

  it should "parse a simple import" in {
    val input = someValidModuleHeader + "\n\n" + "import Banana"
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    em.imports.map(_.moduleName) should equal(Seq("Banana"))
  }

  it should "ignore a single line comment" in {
    val input = someValidModuleHeader + "\n\n-- Comment\n" + "import Banana"
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    em.imports.map(_.moduleName) should equal(Seq("Banana"))
  }

  it should "ignore a multi-line comment" in {
    val input = someValidModuleHeader + "\n\n{- This is \nlots of text\n in a comment \n-}\n" + "import Banana"
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    em.imports.map(_.moduleName) should equal(Seq("Banana"))
  }

  it should "parse an import with all exposed" in {
    val input = someValidModuleHeader + "\n\n" + "import Banana exposing (..)"
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    //fail
    //em.imports.map(_.moduleName) should equal (Set("Banana", exposing = Some(AllExposing))))
  }

  it should "parse an import with an alias" in {
    val input = someValidModuleHeader + "\n\n" + "import Banana as Yellow"
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    //fail
    //em.imports.map(_.moduleName) should equal(Seq("Banana", alias = Some("Yellow"))))
  }

  it should "parse problematic import" in {
    val input =
      """
        |module UsesTodo exposing (..)
        |
        |import Todo as T
      """.stripMargin
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    assert(em.nodeName === "UsesTodo")
    //???
    //em.imports should equal(Seq(Import(moduleName = "Todo", alias = Some("T"))))
  }

  it should "parse a tuple" in {
    val input =
      """
        |module Deeter exposing (..)
        |
        |deet = (1, 2)
      """.stripMargin
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    assert(em.nodeName === "Deeter")
    em.functions.head.body match {
      case ElmTuple(innards) => "yay"
      case _ => ???
    }
  }

  it should "parse this thing that I want to use in my demo in an hour" in {
    val elm =
      """module Foo exposing (..)
        |
        |view : Model -> Html Msg
        |view model =
        |    let
        |        url =
        |            case model.url of
        |                Success url ->
        |                    url
        |
        |                _ ->
        |                    "waiting.gif"
        |    in
        |        Html.div []
        |            [ Html.h2 [] [ Html.text model.topic ]
        |            , Html.img [ Html.Attributes.src url ] []
        |            , Html.button [ Html.Events.onClick MorePlease ]
        |                [ Html.text "More please!" ]
        |            ]""".stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(elm)
  }

  it should "parse a recordSuchThat with curlies that move left" in {
    val elm=
      """module Whatever exposing (..)
        |
        |update : Msg -> Model -> ( Model, Cmd Msg )
        |update msg model =
        |    case msg of
        |        Click { x, y } ->
        |            { model
        |                | click =
        |                    { x = ((x * 100) // model.windowSize.width)
        |                    , y = ((y * 100) // model.windowSize.height)
        |                    }
        |            }
        |                ! []
        |
        |""".stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(elm)
  }

  it should "parse a list with returns in it" in {
    val input=
      """module Whatever exposing (..)
        |
        |view : Model -> Html Msg
        |view model =
        |    Html.div []
        |        [ Html.h2 []
        |            [ Html.text model.topic ]
        |        , Html.img [ Html.Attributes.src model.url ]
        |            []
        |        ]
        |""".stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // great

  }

  it should "parse a union deconstruction in anonymous function arguments" in {
    // This is unlikely to appear in code but it tests that anonymous function
    // arguments are patterns.
    val input =
      """module Foo exposing (..)
        |
        |type Onion
        |    = Peel String
        |
        |anonymousFunctionWithUnionDeconstruction =
        |    \(Peel bar) -> "Peeling the " ++ bar
        |
        |useIt =
        |    anonymousFunctionWithUnionDeconstruction (Peel "banana")
        |""".stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // great
  }

  it should "parse a tuple deconstruction with nested patterns. Oh and a record pattern" in {
    // This is unlikely to appear in code but it tests that anonymous function
    // arguments are patterns.
    val input =
    """module Foo exposing (..)
      |
      |
      |tryAndCallMe ( { thing, stuff }, secondBit ) =
      |    "before " ++ secondBit ++ ", " ++ thing ++ " and " ++ stuff
      |
      |
      |callingYou =
      |    tryAndCallMe ( { thing = "things", stuff = "stuffs" }, "junk" )
      |""".stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // great
  }

  it should "parse a constant declaration" in {
    val input =
      """module Foo exposing (..)
        |
        |foo = "Hello"
        |
        |( thing, stuff ) = ( "one", "two" )
        |
        |ordinaryFunction ( parameterPattern, withStuff ) = parameterPattern ++ withStuff
      """.stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // yay
  }

  it should "parse a port module" in {
    val input =
      """port module Foo exposing(..)
        |
        |port foo : String -> Cmd msg
      """.stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // yay
  }

  it should "parse this string constant" in {
    val input =
      """module Main exposing (..)
        |
        |foo = "bananas"
      """.stripMargin

    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    // yay
  }

  it should "parse an anonymous function" in {
    val input =
      """module Foo exposing (..)
        |
        |foo = \_ _ -> whatever
      """.stripMargin
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
    //yay
  }

  import ElmParserTest._

  it should "parse full project" in {
    val input = FullProgram
    val em = parseAndVerifyCanWriteOutFromParsedStructureUnchanged(input)
  }
}

object ElmParserTest {

  val FullProgram =
    """module Main exposing (main)
      |
      |import Html exposing (Html)
      |import Html.App
      |import Html.Attributes
      |import Html.Events
      |import Http
      |import Task
      |import Json.Decode
      |
      |
      |main : Program Never
      |main =
      |    Html.App.program
      |        { init = init
      |        , subscriptions = subscriptions
      |        , update = update
      |        , view = view
      |        }
      |
      |
      |
      |-- MODEL
      |
      |
      |type alias Model =
      |    { topic : String
      |    , gifUrl : String
      |    }
      |
      |
      |init : ( Model, Cmd Msg )
      |init =
      |    ( { topic = "cats"
      |      , gifUrl = "waiting.gif"
      |      }
      |    , Cmd.none
      |    )
      |
      |
      |
      |-- SUBSCRIPTIONS
      |
      |
      |subscriptions : Model -> Sub Msg
      |subscriptions model =
      |    Sub.none
      |
      |
      |
      |-- UPDATE
      |
      |
      |type Msg
      |    = Noop
      |    | MorePlease
      |    | FetchSucceed String
      |    | FetchFail Http.Error
      |
      |
      |update : Msg -> Model -> ( Model, Cmd Msg )
      |update msg model =
      |    case msg of
      |        FetchSucceed string ->
      |            ( { model | gifUrl = string }, Cmd.none )
      |
      |        FetchFail error ->
      |            ( model, Cmd.none )
      |
      |        MorePlease ->
      |            ( model, getRandomGif model.topic )
      |
      |        Noop ->
      |            ( model, Cmd.none )
      |
      |
      |getRandomGif topic =
      |    let
      |        url =
      |            "https://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=" ++ topic
      |
      |        decodeGifUrl =
      |            Json.Decode.at [ "data", "image_url" ] Json.Decode.string
      |    in
      |        fetch decodeGifUrl url
      |
      |
      |fetch decoder url =
      |    Task.perform FetchFail FetchSucceed (Http.get decoder url)
      |
      |
      |
      |-- VIEW
      |
      |
      |view : Model -> Html Msg
      |view model =
      |    Html.div []
      |        [ Html.h2 [] [ Html.text model.topic ]
      |        , Html.img [ Html.Attributes.src model.gifUrl ] []
      |        , Html.button [ Html.Events.onClick MorePlease ] [ Html.text "More Please!" ]
      |        ]
      |""".stripMargin

  val BeginnerProgram =
    """module Main exposing (main)
      |
      |import Html exposing (Html)
      |import Html.App
      |
      |
      |main : Program Never
      |main =
      |    Html.App.beginnerProgram
      |        { model = model
      |        , update = update
      |        , view = view
      |        }
      |
      |
      |
      |-- MODEL
      |
      |
      |type alias Model =
      |    {}
      |
      |
      |model : Model
      |model =
      |    {}
      |
      |
      |
      |-- UPDATE
      |
      |
      |type Msg
      |    = Noop
      |
      |
      |update : Msg -> Model -> Model
      |update msg model =
      |    case msg of
      |        Noop ->
      |            model
      |
      |
      |
      |-- VIEW
      |
      |
      |view : Model -> Html Msg
      |view model =
      |    Html.div [] []
      |""".stripMargin

  val AdvancedProgram =
    """module Main exposing (main)
      |
      |import Html exposing (Html)
      |import Html.App
      |
      |
      |main : Program Never
      |main =
      |    Html.App.program
      |        { init = init
      |        , subscriptions = subscriptions
      |        , update = update
      |        , view = view
      |        }
      |
      |
      |
      |-- MODEL
      |
      |
      |type alias Model =
      |    {}
      |
      |
      |init : ( Model, Cmd Msg )
      |init =
      |    {} ! []
      |
      |
      |
      |-- UPDATE
      |
      |
      |type Msg
      |    = Noop
      |
      |
      |update : Msg -> Model -> ( Model, Cmd Msg )
      |update msg model =
      |    case msg of
      |        Noop ->
      |            model ! []
      |
      |
      |
      |-- VIEW
      |
      |
      |view : Model -> Html Msg
      |view model =
      |    Html.div [] []
      |
      |
      |
      |-- SUBSCRIPTIONS
      |
      |
      |subscriptions model =
      |    Sub.none""".stripMargin
}
