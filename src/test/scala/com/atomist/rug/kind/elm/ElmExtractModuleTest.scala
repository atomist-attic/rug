package com.atomist.rug.kind.elm

import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._
import com.atomist.util.Utils.StringImprovements

class ElmExtractModuleTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

  it should "move stuff from main to another file" in {
    val prog =
      """editor ExtractAllIntoModule
        |
        |@description "Name of the module to extract into"
        |param new_module_name: ^[A-Z][\w]*$
        |
        |let new_file_name={ new_module_name + ".elm" }
        |
        |# copy the Main file and make into the new module
        |with Project p
        |  do copyFile "Main.elm" new_file_name
        |
        |with File when name = new_file_name
        |  with ElmModule begin
        |    do rename new_module_name
        |    do replaceExposing "Model, init, Msg, update, view, subscriptions"
        |  end
        |
        |RemoveFunction module=new_module_name, function="main"
        |
        |editor RemoveFunction
        |  param module: ^.*$
        |  param function: ^.*$
        |
        |  with ElmModule when name = module
        |    do removeFunction function
        |""".stripMargin

    val module = "RandomGif"
    val source =
      StringFileArtifact("Main.elm", ElmExtractModuleTest.OriginalMain)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), prog, Map("new_module_name" -> module))

    val content = r.findFile("RandomGif.elm").value.content
    val expectedHeader = "module RandomGif exposing (Model, init, Msg, update, view, subscriptions)"
    content should (
      include(expectedHeader)
        and
        not include("main")
      )
  }
  it should "replace the init function" in {
    val prog =
      """editor ReplaceInit
        |param new_module_name: ^[\s\S]*$
        |
        |let lower_new_module={ new_module_name.charAt(0).toLowerCase() + new_module_name.slice(1) }
        |let new_init={
        |return ("    let\n" +
        |        "        ( _LOWER_Model, _LOWER_Commands ) =" + "\n" +
        |        "            _UPPER_.init" + "\n" +
        |        "    in" + "\n" +
        |        "        { arr = _LOWER_Model }" + "\n" +
        |        "            ! [ _LOWER_Commands ]" + "\n").replace(
        |          /_LOWER_/g, lower_new_module).replace(
        |          /_UPPER_/g, new_module_name);
        |}
        |
        |ReplaceFunctionBody function="init", new_body=new_init
        |
        |editor ReplaceFunctionBody
        |
        |param function: ^.*$
        |param new_body: ^[\s\S]*$
        |# gut all the functions in Main and make them call the new module
        |  with ElmModule when name = "Main"
        |    with function when name = function
        |      do replaceBody new_body
        |""".stripMargin

    val module = "RandomGif"
    val source =
      StringFileArtifact("Main.elm", ElmExtractModuleTest.OriginalMain)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), prog, Map("new_module_name" -> module))

    val content = r.findFile("Main.elm").value.content
    content should include("( randomGifModel, randomGifCommands )")
  }

  it should "replace a type alias Model" in {
    val elm =
      """{ randomGif : RandomGif.Model }"""

    val prog =
      """editor ReplaceModel
        |param new_module_name: ^.*$
        |
        |let lower_new_module={ new_module_name.charAt(0).toLowerCase() + new_module_name.slice(1) }
        |let new_body={
        |return ("       { _LOWER_ : _UPPER_.Model }" + "\n").replace(
        |          /_LOWER_/g, lower_new_module).replace(
        |          /_UPPER_/g, new_module_name);
        |}
        |
        |  with ElmModule when name = "Main"
        |    with type.alias when name = "Model"
        |      do replaceBody new_body
        |""".stripMargin

    val module = "RandomGif"
    val source =
      StringFileArtifact("Main.elm", ElmExtractModuleTest.OriginalMain)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), prog, Map("new_module_name" -> module))

    val content = r.findFile("Main.elm").value.content
    content should include(elm)
  }

  it should "replace the subscriptions body" in {
    val elm =
      """Sub.batch [ RandomGif.subscriptions model.randomGif ]"""

    val prog =
      """editor ReplaceSubscriptions
        |param new_module_name: ^.*$
        |
        |let lower_new_module={ new_module_name.charAt(0).toLowerCase() + new_module_name.slice(1) }
        |let new_body={
        |return ("   Sub.batch [ _UPPER_.subscriptions model._LOWER_ ]" + "\n").replace(
        |          /_LOWER_/g, lower_new_module).replace(
        |          /_UPPER_/g, new_module_name);
        |}
        |
        |  with ElmModule when name = "Main"
        |    with type.alias when name = "Model"
        |      do replaceBody new_body
        |""".stripMargin

    pending
  }

  it should "replace the Msg" in {
    val elm = " Noop\n    | RandomGifMsg RandomGif.Msg"

    val prog =
      """editor ReplaceMsg
        |param new_module_name: ^.*$
        |
        |let new_body={
        |return ("  Noop" + "\n" +
        |        "    | _UPPER_Msg _UPPER_.Msg" + "\n").replace(
        |          /_UPPER_/g, new_module_name);
        |}
        |
        |  with ElmModule when name = "Main"
        |    with type when name = "Msg"
        |      do replaceBody new_body
        |""".stripMargin

    val module = "RandomGif"
    val source =
      StringFileArtifact("Main.elm", ElmExtractModuleTest.OriginalMain)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source), prog, Map("new_module_name" -> module))

    val content = r.findFile("Main.elm").value.content
    content should include(elm.toSystem)
  }
}

object ElmExtractModuleTest {
  val OriginalMain =
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
      |init =
      |    { topic = "cats"
      |    , gifUrl = "waiting.gif"
      |    }
      |        ! []
      |
      |
      |
      |-- SUBSCRIPTIONS
      |
      |
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
      |            { model | gifUrl = string } ! []
      |
      |        FetchFail error ->
      |            model ! []
      |
      |        MorePlease ->
      |            model ! [ getRandomGif model.topic ]
      |
      |        Noop ->
      |            model ! []
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
}
