package com.atomist.rug.kind.elm

import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class ElmTextInputTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

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
        |""".stripMargin

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
        |    with case cc when matchAsString = 'msg'
        |      begin
        |         do replaceBody { cc.body() + " ! []" }
        |      end
        |
      """.stripMargin

    val r = elmExecute(r3, prog4)
    val content = r.findFile("Main.elm").value.content

    // TODO should really bring this back, but there appears to be an ordering thing and
    // I'm not sure you've implemented all necessary edits
    content.trim should equal(ElmParserTest.AdvancedProgram)
  }

  it should "add to model" in {
    val source =
      StringFileArtifact("Main.elm", ElmParserTest.BeginnerProgram)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", source),
      ElmTextInputTest.AddToModel,
      Map("module" -> "Main",
        "type_name" -> "model",
        "field_name" -> "topic",
        "field_type" -> "String",
        "initial_value" -> "cats"))

    val content = r.findFile("Main.elm").value.content

    pending
  }
}

object ElmTextInputTest {
  val TextInputEditor =
    """@tag "elm"
      |@description "Adds a text input to an Elm beginner program "
      |editor AddTextInput
      |
      |@displayName "Name"
      |@description "Name of the text input (the field to store in the model)"
      |@validInput "An elm identifier"
      |param input_name: ^[a-z][\w]*$
      |
      |# capitalize
      |let type_name={ input_name.charAt(0).toUpperCase() + input_name.slice(1) }
      |let function_body={ input_name + "Input model = \n" +
      |   "    Html.input [ Html.Events.onInput " + type_name + " ][]\n" }
      |
      |AddToModel initial_value='""', field_type="String", field_name=input_name
      |
      |AddMessage constructor={ type_name + " String" },
      |           update_model={ "{model | model." + input_name + " = string }" },
      |           deconstructor={ type_name + " string" }
      |
      |AddImport module="Main", fqn="Html.Events"
      |
      |AddFunction module="Main", code=function_body
      |
      |@description "add an import statement, if it does not exist yet"
      |editor AddImport
      |  param module: [A-Z][\w]*
      |
      |  @description "The import to add, fully qualified"
      |  param fqn: ^.*$
      |
      |  with ElmModule when name = module
      |    do addImportStatement {"import " + fqn}
      |
      |@description "add a whole declaration"
      |editor AddFunction
      |
      |@description "Where does this belong"
      |@displayName "Module Name"
      |param module: [A-Z][\w]*
      |
      |@displayName "Code"
      |@description "All the code to add"
      |@validInput "An Elm declaration"
      |param code: ^.*$
      |
      |with File
      |  with ElmModule when name = module
      |    do addFunction code
      |""".stripMargin

  val AddToModel =
    """editor AddToModel
      |
      |param field_name: ^[a-z][\w]*$
      |param field_type: ^.*$
      |param initial_value: ^.*$
      |
      |
      |AddToRecordTypeAlias type_name="Model", module="Main"
      |AddToModelInitialization function_name="model", module="Main", field_value=initial_value
      |
      |
      |editor AddToRecordTypeAlias
      |
      |  param module: ^.*$
      |  param type_name: ^[a-z][\w]*$
      |  param field_name: ^[a-z][\w]*$
      |  param field_type: ^.*$
      |
      |with File
      |  with ElmModule when name = module
      |    with type.alias when name = type_name
      |      with recordType
      |        do add field_name field_type
      |
      |
      |editor AddToModelInitialization
      |
      |	 param module: ^.*$
      |  param function_name: ^[a-z][\w]*$
      |  param field_name: ^[a-z][\w]*$
      |  param field_value: ^.*$
      |
      |with File
      |  with ElmModule when name = module
      |    with function when name = function_name
      |      with recordValue
      |        do add field_name field_value
      |""".stripMargin

  val AddMessage =
    """@tag "elm"
      |@description "Adds a Msg to an Elm beginner program"
      |editor AddMessage
      |
      |@displayName "Message Constructor"
      |@description "Message constructor, its name and parameter types"
      |param constructor: ^.*$
      |
      |@displayName "Deconstructor"
      |@description "What this looks like in a pattern match; leave blank if there are no type parameters"
      |@optional
      |@default ""
      |param deconstructor: ^.*$
      |
      |@displayName "Case Clause Body"
      |@description "Value for model when the message is received"
      |@optional
      |@default "model"
      |param update_model: ^.*$
      |
      |AddToUnionType module="Main", type_name="Msg", constructor=constructor
      |AddCaseClause module="Main", function_name="update", match="msg", new_pattern=deconstructor, body=update_model
      |
      |@description "Add a constructor to a union type"
      |editor AddToUnionType
      |
      |	@description "Where is this type"
      |	@displayName "Module Name"
      |	param module: [A-Z][\w]*
      |
      |  @description "The union type"
      |  @displayName "Type"
      |  param type_name: ^[a-z][\w]*$
      |
      |  @description "The constructor to add, with its type parameters"
      |  @displayName "Constructor"
      |  param constructor: ^[a-z][\w]*$
      |
      |with File
      |  with ElmModule when name = module
      |    with type when name = type_name
      |        do addConstructor constructor
      |
      |
      |@description "Add a new pattern to a case"
      |editor AddCaseClause
      |
      |	@description "Elm module to modify"
      |	@displayName "Module Name"
      |	param module: ^[A-Z][\w]*$
      |
      |  @description "Name of the function that has a case in it"
      |  @displayName "Function name"
      |  param function_name: ^[a-z][\w]*$
      |
      |  @displayName "Match Expression"
      |  @description "What the case statement is matching"
      |  param match: ^.*$
      |
      |  @displayName "New Pattern"
      |  @description "new pattern the case can match"
      |  @validInput "A union type deconstructor"
      |  param new_pattern: ^.*$
      |
      |  @displayName "Result"
      |  @description "body of the new case clause"
      |  @validInput "An Elm expression"
      |  param body: ^.*$
      |
      |# TODO implement the selection by match expression
      |with File
      |  with ElmModule when name = module
      |    with function when name = function_name
      |      with case when matchAsString = match
      |        do addClause new_pattern body
      |""".stripMargin
}
