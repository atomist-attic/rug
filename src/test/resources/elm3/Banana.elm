module Banana exposing (..)

import Html exposing (Html)
import Html.App


main : Program Never
main =
    { model = {}
    , update = update
    , view = view
    }
        |> Html.App.beginnerProgram


type alias Model =
    {}


type Msg
    = Noop


update : Msg -> Model -> Model
update msg model =
    case msg of
        Noop ->
            model


view model =
    Html.text "Foo Banana!"
