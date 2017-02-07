package com.atomist.rug.kind.elm

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.elm.ElmTypeUsageTest.TestDidNotModifyException
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.rug.{CompilerChainPipeline, DefaultRugPipeline, RugPipeline}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

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
      |param old_name: ^[A-Z][\w]+$
      |
      |@description "New name for the module"
      |param new_name: ^[A-Z][\w]+$
      |
      |with ElmModule when name = old_name
      | do rename new_name
      |
      |with ElmModule e when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  private def createElmRenamerClass(param: Boolean, prints: Boolean = false) = {
    val p1 = """{name: "old_name", description: "Name of module we're renaming", required: true, maxLength: 100, pattern: "^[A-Z][\w]+$"}"""
    val p2 = """{name: "new_name", description: "New name for the module", required: true, maxLength: 100, pattern: "^[A-Z][\w]+$"}"""

    var params = "[]"
    if(param){
       params = s"""[$p1, $p2]"""
    }
    s"""
        |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
        |import {Project,ElmModule} from '@atomist/rug/model/Core'
        |import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
        |
        |import {Parameter} from '@atomist/rug/operations/RugOperation'
        |
        |let params: Parameter[] = $params
        |
        |declare var print
        |
        |class Renamer implements ProjectEditor {
        |    name: string = "ModuleRenamer"
        |    description: string = "Renames an Elm module"
        |    parameters: Parameter[] = params
        |
        |    edit(project: Project, {old_name, new_name }: {old_name: string, new_name: string}) {
        |        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        |        let allModules: ElmModule[] =
        |             eng.children<ElmModule>(project, "ElmModule")
        |
        |         for (let em of allModules) if (em.name() == old_name) {
        |            ${if (prints) "print(`Modifying $${em} to have name $${new_name}`)" else ""}
        |            em.rename(new_name)
        |         }
        |
        |         //print(`found $${allModules.length} ElmModules in $${project}`)
        |         for (let em of allModules) {
        |            ${if (prints) "print(`Module $${em}`)" else ""}
        |           if (em.imports(old_name)) {
        |             em.updateImport(old_name, new_name)
        |           }
        |        }
        |    }
        |}
        |
        |export let editor = new Renamer()
          """.stripMargin
  }

  it should "rename module using TypeScript without path expression" in doRename(
    createElmRenamerClass(param = false),
    runtime = typeScriptPipeline
  )

  it should "rename module using TypeScript without path expression with @parameter decorator" in pendingUntilFixed(doRename(
    createElmRenamerClass(param = true),
    runtime = typeScriptPipeline
  ))

  it should "print SafeCommittingProxy without failing" in pendingUntilFixed(doRename(
    createElmRenamerClass(param = true, prints = true),
    runtime = typeScriptPipeline
  ))

  it should "rename module using path expression" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: ^[A-Z][\w]+$
      |
      |@description "New name for the module"
      |param new_name: ^[A-Z][\w]+$
      |
      | # TODO note hard coding here
      |let em = $(/ElmModule()[@name='Todo'])
      |
      |with em
      | do rename new_name
      |
      |with ElmModule when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  it should "rename module using native Rug predicate under file" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: ^[A-Z][\w]+$
      |
      |@description "New name for the module"
      |param new_name: ^[A-Z][\w]+$
      |
      |with File
      | with ElmModule when name = old_name
      |   do rename new_name
      |
      |with ElmModule e when imports old_name
      |do updateImport from old_name to new_name
    """.stripMargin
  )

  it should "rename module using JavaScript predicate" in doRename(
    """
      |@description "Renames an Elm module"
      |editor Renamer
      |
      |@description "Name of module we're renaming"
      |param old_name: ^[A-Z][\w]+$
      |
      |@description "New name for the module"
      |param new_name: ^[A-Z][\w]+$
      |
      |with ElmModule e
      |when { e.name() == old_name}
      |do
      |  rename new_name
      |
      |with ElmModule e
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

    val newTodoContent = result.findFile(s"$newModuleName.elm").value.content
    newTodoContent.trim should equal("module Foobar exposing (..)")
    val newUsesTodoContent = result.findFile(usesTodoSource.path).value.content
    newUsesTodoContent.trim should equal(makeUsesTodoSource(newModuleName).content.trim)
  }

  it should "rename module directly under project using native Rug predicate" in {
    val prog =
      """
        |editor Organize
        |
        |RenameModuleToMain
        |
        |editor RenameModuleToMain
        |with Project
        |   with ElmModule m
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
         |with ElmModule
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Foo exposing (..)"""
        .stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val cont = r.findFile("Todo.elm").value.content
    cont should include(newImportStatement)
    // TODO had to add 4 returns here. Maybe 3 is correct?
    cont should be(todoSource.content + s"${System.lineSeparator()}${System.lineSeparator()}${System.lineSeparator()}${System.lineSeparator()}" + newImportStatement)
  }

  it should "add import when one existing import" in {
    val newImport = "Foo"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with ElmModule
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Foo exposing (..)
          |
         |import Thing
          |"""
        .stripMargin)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Todo.elm").value.content
    content should include(newImportStatement)
    content should be(todoSource.content + newImportStatement + System.lineSeparator())
  }

  it should "not add import when it is already there" in {
    val newImport = "Thing"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with ElmModule
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Todo exposing (..)
          |
          |import $newImport
          |"""
        .stripMargin)

    an[TestDidNotModifyException] shouldBe thrownBy(
      elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog))
  }

  it should "not add import when it is already there checking trailing newline" in {
    val newImport = "Thing"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with ElmModule
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Todo.elm",
      s"""module Todo exposing (..)
          |
          |import $newImport
          |"""
        .stripMargin)
    an[TestDidNotModifyException] shouldBe thrownBy(
      elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog))
  }

  it should "add import in complete source" in {
    val newImport = "Absquatulator"
    val newImportStatement = "import " + newImport
    val prog =
      s"""
         |editor AddImport
         |with ElmModule
         |do addImportStatement "$newImportStatement"
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content should include(newImportStatement)
  }

  it should "add a whole declaration" in {
//    val pattern = "SeeThis string"
//    val expression = "model"
    val prog =
      s"""
         |editor AddFunction
         |
         |with File
         |  with ElmModule when name = module
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
    val content = r.findFile("Main.elm").value.content
    content should include(addThisCode)
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
        |with ElmModule
        |  with function when name = 'foo'
        |    do rename 'bar'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", input)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
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
        |with ElmModule
        |  with function when name = 'foo'
        |    do rename 'bar'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", input)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content.trim should equal(output.trim)
  }

  it should "add a value to the model in an advanced program with quotes around any string" in {
    val identifier = "init"
    val newField = "hits"
    val initialValue = 0
    val fieldType = "String"
    val prog =
      s"""editor AddToRecordValue
          |param initial_value:^.*$$
          |param field_type:^.*$$
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
          |with ElmModule
          |  with function when name = '$identifier'
          |    with recordValue
          |      do add '$newField' initial_value_careful
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm", ElmParserTest.AdvancedProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
      Map("field_type" -> fieldType, "initial_value" -> initialValue.toString))
    val content = r.findFile("Main.elm").value.content
    content should include(s"""$newField = \"$initialValue\"""")
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
         |with ElmModule
         |  with function f
         |    with case c
         |      do addClause pattern '$expression'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content

    pending
  }

  it should "rename function" in {
    val oldFunctionName = "update"
    val newFunctionName = "absquatulate"
    val prog =
      s"""
         |editor AddCaseClause
         |with ElmModule
         |  with function f when name = '$oldFunctionName'
         |    do rename '$newFunctionName'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content should include(newFunctionName)
    //content should be(todoSource.content + newImportStatement)
  }

  it should "rename type alias" in {
    val oldTypeAliasName = "Model"
    val newTypeAliasName = "Absquatulate"
    val prog =
      s"""
         |editor RenameTypeAlias
         |with ElmModule
         |  with type.alias when name = '$oldTypeAliasName'
         |    do rename '$newTypeAliasName'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content should include(newTypeAliasName)
    //content should be(todoSource.content + newImportStatement)
  }

  it should "add to record type" in {
    val oldTypeAliasName = "Model"
    val newIdentifier = "hits"
    val newType = "Int"
    val prog =
      s"""
         |editor AddToRecord
         |with File when name = 'Main.elm'
         | with ElmModule when name = 'Main'
         |  with type.alias when name = '$oldTypeAliasName'
         |    with recordType
         |      do add '$newIdentifier' '$newType'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content should include(s"$newIdentifier : $newType")
  }

  it should "add to empty record type" in {
    val oldTypeAliasName = "Model"
    val newIdentifier = "hits"
    val newType = "Int"
    val prog =
      s"""
         |editor AddToRecord
         |with File
         | with ElmModule when name = 'Main'
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
    val content = r.findFile("Main.elm").value.content
    content should include(s"{ $newIdentifier : $newType }")
  }

  it should "add to record value at top of function" in {
    val identifier = "model"
    val newField = "hits"
    val initialValue = "0"
    val prog =
      s"""
         |editor AddToRecordValue
         |with ElmModule
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
    val content = r.findFile("Main.elm").value.content
    content should include(s"$newField = $initialValue")
  }

  it should "add to record value deep in function" in {
    val identifier = "init"
    val newField = "hits"
    val initialValue = "0"
    val prog =
      s"""
         |editor AddToRecordValue
         |with ElmModule
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
    val content = r.findFile("Main.elm").value.content
    content should include(s"$newField = $initialValue")
  }

  it should "add a constructor to a union type" in {
    val newConstructor = "SeeThis String" // is an ElmTypeWithParameters
    val unionTypeName = "Msg"
    val prog =
      s"""
         |editor AddToUnionType
         |with ElmModule
         |  with type when name = '$unionTypeName'
         |      do addConstructor '$newConstructor'
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content should include(s"| $newConstructor") // assumes it's at the end or in the middle
  }

  it should "add function" in {
    val prog =
      s"""
         |editor AddFunction
         |with ElmModule
         |  do addFunction "thisIsAFunction = foo"
      """.stripMargin
    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content

    pending
  }

  it should "replace the body of this function" in {
    val prog =
      """editor UpgradeMainFunction
        |  param module: ^.*$
        |
        |  with File f begin
        |    with ElmModule when name = module
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
    val content = r.findFile("Main.elm").value.content
    content should include(", subscriptions = subscriptions2")
  }

  it should "replace the body of case clauses" in {
    val toAppend = "-- And this is the end of the line"
    val prog =
      s"""editor Foo
          |
        |with ElmModule
          |  # use alias f to avoid issues with JavaScript reserved word
          |  with function f when name = 'update'
          |    with case c when matchAsString = "msg"
          |      with caseClause cc
          |        do replaceBody { cc.body() + "$toAppend" }
      """.stripMargin

    val todoSource = StringFileArtifact("Main.elm",
      ElmParserTest.FullProgram)

    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog)
    val content = r.findFile("Main.elm").value.content
    content.split(toAppend) should have length(5)
  }

  it should "let me switch on the type of a function" in {
    val prog =
      """editor AddCase
        |param new_pattern: ^.*$
        |param body: ^.*$
        |
        |with File
        |  with ElmModule when name = "Main"
        |    with function upd when name = "update"
        |      with case oops when matchAsString = "msg"
        |        do addClause new_pattern {
        |           if (upd.typeSpecification() == "Msg -> Model -> ( Model, Cmd Msg )" &&
        |					     body == "model") {
        |							  //print("recognized advanced application");
        |								return "model ! []"
        |						 } else return body;
        |				}""".stripMargin

    {
      val todoSource = StringFileArtifact("Main.elm",
        ElmParserTest.FullProgram)

      val advancedProgramShouldAlterThis = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
        Map("new_pattern" -> "Hello", "body" -> "model"))
      val content = advancedProgramShouldAlterThis.findFile("Main.elm").value.content
      content should(
        include("Hello ->")
          and
          include("model ! []")
        )
    }

    {
      val basicProgramShouldNotAlterThis = elmExecute(new SimpleFileBasedArtifactSource("", StringFileArtifact("Main.elm", ElmParserTest.BeginnerProgram)), prog,
        Map("new_pattern" -> "Hello", "body" -> "model"))
      val content = basicProgramShouldNotAlterThis.findFile("Main.elm").value.content
      content should(
        include("Hello ->")
          and
          not include("model ! []")
        )
    }

    {
      val todoSource = StringFileArtifact("Main.elm",
        ElmParserTest.FullProgram)

      val advancedProgramShouldPassASpecialBody = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), prog,
        Map("new_pattern" -> "Hello", "body" -> "(model, Cmd.none)"))
      val content = advancedProgramShouldPassASpecialBody.findFile("Main.elm").value.content
      content should(
        include("Hello ->")
        and
          not include ("model ! []")
      )
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
        |with File
        |    with ElmModule when name = "Foo"
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
    val content = r.findFile("Foo.elm").value.content
    content should include("chuck = 10000000")
  }
}

object ElmTypeUsageTest extends FlatSpec {

  class TestDidNotModifyException extends RuntimeException

  def elmExecute(elmProject: ArtifactSource, program: String,
                 params: Map[String, String] = Map(),
                 runtime: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)
                ): ArtifactSource = {

    val as = TypeScriptBuilder.compileWithModel(new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(runtime.defaultFilenameFor(program), program)))
    val eds = runtime.create(as,  None)
    if (eds.isEmpty) {
      print(program); throw new Exception("No editor was parsed")
    }
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
            withClue(s"Content was: ${f.content}") {
              ElmParser.parse(f.content)
            }
          }
          catch {
            case e: Exception =>
              println(s"Failed to pass [${f.path}] after edit(s). Source is[\n${f.content}]")
              throw e
          }
        }
        sm.result
      case nmn: NoModificationNeeded =>
        throw new TestDidNotModifyException
      case _ => ???

    }
  }
}
