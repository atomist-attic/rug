package com.atomist.rug.kind.elm

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.elm.ElmTypeUsageTest.TestDidNotModifyException
import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, DefaultRugPipeline, RugPipeline}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ElmTypeUsageTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler()))


  it should "rename module using native Rug predicate" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: [A-Z][\w]+
      |
      |@description "New name for the module"
      |param new_name: [A-Z][\w]+
      |
      |with elm.module when name = old_name
      | do rename new_name
      |
      |with elm.module e when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  def elmRenamer(param: Boolean) = {
    val imp1 = if (param)
      """
        |@parameter({description: "Name of module we're renaming",
        |    pattern: "[A-Z][\w]+", maxLength: 100, required: true
        |  })
      """.stripMargin
    else ""

    val imp2 = if (param)
      """
        |@parameter({description: "New name for the module",
        |    pattern: "[A-Z][\w]+", maxLength: 100
        |  })
      """.stripMargin
    else ""

    // TODO printing barfs due to primitive issue

    s"""import {ProjectEditor} from 'user-model/operations/ProjectEditor'
            |import {Parameters, ParametersSupport} from 'user-model/operations/Parameters'
            |import {Project,ElmModule} from 'user-model/model/Core'
            |import {PathExpressionEngine} from 'user-model/tree/PathExpression'
            |
            |import {editor, parameter, parameters,inject} from 'user-model/support/Metadata'
            |import {Result,Status} from 'user-model/operations/Result'
            |
            |abstract class ElmRenamerParameters extends ParametersSupport {
            |
            |   $imp1
            |   old_name: string = null
            |
            |   $imp2
            |   new_name: string = null
            |}
            |
            |declare var print
            |
            |@editor("Renames an Elm module")
            |class Renamer implements ProjectEditor<ElmRenamerParameters> {
            |
            |     private eng: PathExpressionEngine;
            |
            |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
            |      this.eng = _eng;
            |    }
            |
            |    edit(project: Project,
            |        @parameters("ElmRenamerParameters") p: ElmRenamerParameters): Result {
            |        let allModules: Array<ElmModule> =
            |             this.eng.children<ElmModule>(project, "elm.module")
            |
            |         for (let em of allModules) if (em.name() == p.old_name) {
            |            //print(`Modifying $${em} to have name $${p.new_name}`)
            |            em.rename(p.new_name)
            |         }
            |
            |         print(`found $${allModules.length} elm modules in $${project}`)
            |         for (let em of allModules) {
            |           //print(`Module $${em}`)
            |           if (em.imports(p.old_name)) {
            |             em.updateImport(p.old_name, p.new_name)
            |           }
            |        }
            |        return new Result(Status.Success, "OK")
            |    }
            |}
          """.stripMargin
    }

  it should "rename module using TypeScript without path expression" in doRename(
    elmRenamer(param = false),
    runtime = typeScriptPipeline
  )

  it should "rename module using TypeScript without path expression with @parameter decorator" in pendingUntilFixed(doRename(
    elmRenamer(param = true),
    runtime = typeScriptPipeline
  ))

  it should "rename module using path expression" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: [A-Z][\w]+
      |
      |@description "New name for the module"
      |param new_name: [A-Z][\w]+
      |
      | # TODO note hard coding here
      |let em = $(/->elm.module[name='Todo'])
      |
      |with em
      | do rename new_name
      |
      |with elm.module when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  it should "rename module using native Rug predicate under file" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: [A-Z][\w]+
      |
      |@description "New name for the module"
      |param new_name: [A-Z][\w]+
      |
      |with file
      | with elm.module when name = old_name
      |   do rename new_name
      |
      |with elm.module e when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  it should "rename module using JavaScript predicate" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: [A-Z][\w]+
      |
      |@description "New name for the module"
      |param new_name: [A-Z][\w]+
      |
      |with elm.module e
      |when { e.name() == old_name}
      |do
      |  rename new_name
      |
      |with elm.module e
      |when { e.imports(old_name) }
      |do
      |updateImport old_name new_name
    """.stripMargin
  )

  private def doRename(program: String,
                       runtime: RugPipeline = new DefaultRugPipeline()) {
    val oldModuleName = "Todo"
    val newModuleName = "Foobar"
    val todoSource = StringFileArtifact(s"$oldModuleName.elm", s"module $oldModuleName exposing (..)")
    def makeUsesTodoSource(moduleName: String) = StringFileArtifact("UsesTodo.elm",
      s"""module UsesTodo exposing (..)
          |
          |import $moduleName""".stripMargin)

    val usesTodoSource = makeUsesTodoSource(oldModuleName)

    val elmProject = new SimpleFileBasedArtifactSource("", Seq(
      todoSource, usesTodoSource
    )
    )

    val result = elmExecute(elmProject, program, Map(
      "old_name" -> oldModuleName,
      "new_name" -> newModuleName
    ),
      runtime = runtime)

    val newTodoContent = result.findFile(s"$newModuleName.elm").get.content
    newTodoContent.trim should equal("module Foobar exposing (..)")
    val newUsesTodoContent = result.findFile(usesTodoSource.path).get.content
    newUsesTodoContent.trim should equal(makeUsesTodoSource(newModuleName).content.trim)
  }

  it should "rename module directly under project using native Rug predicate" in {
    val prog = """
      |editor Organize
      |
      |RenameModuleToMain
      |
      |editor RenameModuleToMain
      |with project
      |   with elm.module m
      |     do rename newName="Foobar"
    """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Foo exposing (..)
          |"""
        .stripMargin)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    // We've seen failures with Elm module directly under project in the wild,
    // so if we get here we're probably OK
  }


  it should "add import when no existing imports" in {
    val newImport = "Foo"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with elm.module
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Foo exposing (..)"""
        .stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val cont = r.findFile("Todo.elm").get.content
    cont.contains(newImportStatement) should be(true)
    // TODO had to add 4 returns here. Maybe 3 is correct?
    cont should be(todoSource.content + "\n\n\n\n" + newImportStatement)
  }

  it should "add import when one existing import" in {
    val newImport = "Foo"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with elm.module
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Foo exposing (..)
          |
         |import Thing
          |"""
        .stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Todo.elm").get.content
    content.contains(newImportStatement) should be(true)
    content should be(todoSource.content + newImportStatement + "\n")
  }

  it should "not add import when it is already there" in {
    val newImport = "Thing"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with elm.module
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Todo exposing (..)
          |
          |import $newImport
          |"""
        .stripMargin)

    an[TestDidNotModifyException] shouldBe thrownBy (
      elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog))
  }

  it should "not add import when it is already there checking trailing newline" in {
    val newImport = "Thing"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with elm.module
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Todo exposing (..)
          |
          |import $newImport
          |"""
        .stripMargin)
    an[TestDidNotModifyException] shouldBe thrownBy (
      elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog))
  }

  it should "add import in complete source" in {
    val newImport = "Absquatulator"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with elm.module
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(newImportStatement) should be(true)
  }

  it should "add a whole declaration" in {
    val pattern = "SeeThis string"
    val expression = "model"
    val prog =
      s"""
         |editor AddFunction
         |
         |with file
         |  with elm.module when name = module
         |    do addFunction code
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val addThisCode =
      """foo bar =
    bar [ and
        , more
        , bar
        ]"""

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog, Map("module" -> "Main", "code" ->
      """foo bar =
        |    bar [ and
        |        , more
        |        , bar
        |        ]""".stripMargin))
    val content = r.findFile("Main.elm").get.content
    content.contains(addThisCode) should be(true)
  }

  it should "rename a function" in {
    val input =
      """module Main exposing (..)
        |
        |foo p1 p2 = p1 ++ p2
      """.stripMargin

    val output =
      """module Main exposing (..)
        |
        |bar p1 p2 = p1 ++ p2
      """.stripMargin

    val prog =
      """editor RenameAFunction
        |
        |with elm.module
        |  with function when name = 'foo'
        |    do rename 'bar'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", input)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.trim should equal(output.trim)
  }

  it should "rename a simple constant" in {
    val input =
    """module Main exposing (..)
      |
      |foo = "bananas"
    """.stripMargin

    val output =
      """module Main exposing (..)
        |
        |bar = "bananas"
      """.stripMargin

    val prog =
      """editor RenameAFunction
        |
        |with elm.module
        |  with function when name = 'foo'
        |    do rename 'bar'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", input)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.trim should equal(output.trim)
  }

  it should "add a value to the model in an advanced program with quotes around any string" in {
    val identifier = "init"
    val newField = "hits"
    val initialValue = 0
    val fieldType = "String"
    val prog =
      s"""editor AddToRecordValue
          |param initial_value:.*
          |param field_type:.*
          |
         |let initial_value_careful = {
          |  var quoted = /^".*"$$/
          |  if (field_type == "String") {
          |    if (quoted.test(initial_value)) {
          |      print("String value already quoted");
          |      return initial_value;
          |    } else {
          |      //print("adding quotes");
          |      return '"' + initial_value + '"';
          |    }
          |  } else {
          |      print("not a string");
          |     return initial_value;
          |  }
          |}
          |with elm.module
          |  with function when name = '$identifier'
          |    with recordValue
          |      do add '$newField' initial_value_careful
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", ElmParserTest.AdvancedProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
      Map("field_type" -> fieldType, "initial_value" -> initialValue.toString))
    val content = r.findFile("Main.elm").get.content
    content.contains(s"""$newField = \"$initialValue\"""") should be(true)
  }

  /*
   Add to this structure:

      case msg of
        Increment ->
            { model | counter = model.counter }

        Noop ->
            model
   */
  it should "add case clause in complete source" in {
    val pattern = "SeeThis string"
    val expression = "model"
    val prog =
      s"""
         |editor AddCaseClause
         |
         |let pattern='$pattern'
         |
         |with elm.module
         |  with function f
         |    with case c
         |      do addClause pattern '$expression'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
  }

  it should "rename function" in {
    val oldFunctionName = "update"
    val newFunctionName = "absquatulate"
    val prog =
      s"""
         |editor AddCaseClause
         |with elm.module
         |  with function f when name = '$oldFunctionName'
         |    do rename '$newFunctionName'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(newFunctionName) should be(true)
    //content should be(todoSource.content + newImportStatement)
  }

  it should "rename type alias" in {
    val oldTypeAliasName = "Model"
    val newTypeAliasName = "Absquatulate"
    val prog =
      s"""
         |editor RenameTypeAlias
         |with elm.module
         |  with type.alias when name = '$oldTypeAliasName'
         |    do rename '$newTypeAliasName'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(newTypeAliasName) should be(true)
    //content should be(todoSource.content + newImportStatement)
  }

  it should "add to record type" in {
    val oldTypeAliasName = "Model"
    val newIdentifier = "hits"
    val newType = "Int"
    val prog =
      s"""
         |editor AddToRecord
         |with file when name = 'Main.elm'
         | with elm.module when name = 'Main'
         |  with type.alias when name = '$oldTypeAliasName'
         |    with recordType
         |      do add '$newIdentifier' '$newType'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(s"$newIdentifier : $newType") should be(true)
  }

  it should "add to empty record type" in {
    val oldTypeAliasName = "Model"
    val newIdentifier = "hits"
    val newType = "Int"
    val prog =
      s"""
         |editor AddToRecord
         |with file
         | with elm.module when name = 'Main'
         |  with type.alias when name = '$oldTypeAliasName'
         |    with recordType
         |      do add '$newIdentifier' '$newType'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      """module Main exposing (..)
        |
        |type alias Model =
        |    {}
        |""".stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(s"{ $newIdentifier : $newType }") should be(true)
  }

  it should "add to record value at top of function" in {
    val identifier = "model"
    val newField = "hits"
    val initialValue = "0"
    val prog =
      s"""
         |editor AddToRecordValue
         |with elm.module
         |  with function when name = '$identifier'
         |    with recordValue
         |      do add '$newField' '$initialValue'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      """module Main exposing (..)
        |
        |model =
        |    { recordField = "foo" }
        |""".stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(s"$newField = $initialValue") should be(true)
  }

  it should "add to record value deep in function" in {
    val identifier = "init"
    val newField = "hits"
    val initialValue = "0"
    val prog =
      s"""
         |editor AddToRecordValue
         |with elm.module
         |  with function when name = '$identifier'
         |    with recordValue
         |      do add '$newField' '$initialValue'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      """module Main exposing (..)
        |
        |init =
        |    ( { recordField = "foo" }, Cmd.none )
        |""".stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(s"$newField = $initialValue") should be(true)
  }

  it should "add a constructor to a union type" in {
    val newConstructor = "SeeThis String" // is an ElmTypeWithParameters
    val unionTypeName = "Msg"
    val prog =
      s"""
         |editor AddToUnionType
         |with elm.module
         |  with type when name = '$unionTypeName'
         |      do addConstructor '$newConstructor'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.contains(s"| $newConstructor") should be(true) // assumes it's at the end or in the middle
  }

  it should "add function" in {
    val prog =
      s"""
         |editor AddFunction
         |with elm.module
         |  do addFunction "thisIsAFunction = foo"
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
  }

  it should "replace the body of this function" in {
    val prog =
      """editor UpgradeMainFunction
        |  param module: .*
        |
        |  with file f begin
        |    with elm.module when name = module
        |      begin
        |        with function f when name = 'main'
        |          do replaceBody{ return (
        |             "(Html.App.program" + "\n" +
        |             "        { init = init" + "\n" +
        |             "        , subscriptions = subscriptions2" + "\n" +
        |             "        , update = update" + "\n" +
        |             "        , view = view" + "\n" +
        |             "        })\n") }
        |      end
        |   end
      """.stripMargin

    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog, Map("module" -> "Main"))
    val content = r.findFile("Main.elm").get.content
    content.contains(", subscriptions = subscriptions2") should be(true)
  }

  it should "replace the body of case clauses" in {
    val toAppend = "-- And this is the end of the line"
    val prog =
      s"""editor Foo
          |
        |with elm.module
          |  # use alias f to avoid issues with JavaScript reserved word
          |  with function f when name = 'update'
          |    with case c when matchAsString = "msg"
          |      with caseClause cc
          |        do replaceBody { cc.body() + "$toAppend" }
      """.stripMargin

    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").get.content
    content.split(toAppend).length should equal(5)
  }

  it should "let me switch on the type of a function" in {
    val prog =
      """editor AddCase
        |param new_pattern: .*
        |param body: .*
        |
        |with file
        |  with elm.module when name = "Main"
        |    with function upd when name = "update"
        |      with case oops when matchAsString = "msg"
        |        do addClause new_pattern {
        |           if (upd.typeSpecification() == "Msg -> Model -> ( Model, Cmd Msg )" &&
        |					     body == "model") {
        |							  print("recognized advanced application");
        |								return "model ! []"
        |						 } else return body;
        |				}""".stripMargin

    {
      val todoSource = StringFileArtifact("Main.elm",
        ElmParserTest.FullProgram)

      val advancedProgramShouldAlterThis = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
        Map("new_pattern" -> "Hello", "body" -> "model"))
      val content = advancedProgramShouldAlterThis.findFile("Main.elm").get.content
      content.contains("Hello ->") should be(true)
      content.contains("model ! []") should be(true)
    }

    {
      val basicProgramShouldNotAlterThis = elmExecute(new SimpleFileBasedArtifactSource("", StringFileArtifact("Main.elm", ElmParserTest.BeginnerProgram)), prog,
        Map("new_pattern" -> "Hello", "body" -> "model"))
      val content = basicProgramShouldNotAlterThis.findFile("Main.elm").get.content
      content.contains("Hello ->") should be(true)
      content.contains("model ! []") should be(false)
    }

    {
      val todoSource = StringFileArtifact("Main.elm",
        ElmParserTest.FullProgram)

      val advancedProgramShouldPassASpecialBody = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
        Map("new_pattern" -> "Hello", "body" -> "(model, Cmd.none)"))
      val content = advancedProgramShouldPassASpecialBody.findFile("Main.elm").get.content
      content.contains("Hello ->") should be(true)
      content.contains("model ! []") should be(false)
    }
  }

  it should "add stuff to a record inside a let and an infix fn" in {
    val elm =
      """module Foo exposing(..)
        |init : ( Model, Cmd Msg )
        |init =
        |    let
        |        ( randomGifModel, randomGifCommands ) =
        |            RandomGif.init
        |    in
        |        { randomGif = randomGifModel }
        |            ! [ Cmd.map RandomGifMsg randomGifCommands ]
        |""".stripMargin

    val prog =
      """editor Bar
        |
        |let field_value_careful="10000000"
        |let field_name="chuck"
        |
        |with file
        |    with elm.module when name = "Foo"
        |      with function f when { f.name() == "init" || f.name() == "model"}
        |        with recordValue begin
        |          #do fail { print(recordValue.toString()) }
        |          do add field_name field_value_careful
        |        end
        |
      """.stripMargin

    val todoSource = StringFileArtifact("Foo.elm",
      elm)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Foo.elm").get.content
    content.contains("chuck = 10000000") should be(true)
  }
}

object ElmTypeUsageTest extends LazyLogging {

  class TestDidNotModifyException extends RuntimeException

  def elmExecute(elmProject: ArtifactSource, program: String,
                 params: Map[String, String] = Map(),
                 runtime: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)
                ): ArtifactSource = {

    val eds = runtime.createFromString(program)
    val pe = eds.head.asInstanceOf[ProjectEditor]

    val r = pe.modify(elmProject, SimpleProjectOperationArguments("", params))
    r match {
      case sm: SuccessfulModification =>
        for {
          f <- sm.result.allFiles
          if f.name.endsWith(ElmModuleType.ElmExtension)
        } {
          // Parse it to see its content is OK
          try {
            ElmParser.parse(f.content)
          }
          catch {
            case e: Exception =>
              logger.debug(s"Failed to pass [${f.path}] after edit(s). Source is[\n${f.content}]")
              throw e
          }
        }
        sm.result
      case nmn : NoModificationNeeded =>
        throw new TestDidNotModifyException

    }
  }
}
