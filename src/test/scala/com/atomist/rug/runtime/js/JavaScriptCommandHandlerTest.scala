package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.common.MissingParametersException
import com.atomist.rug.RugArchiveReader
import com.atomist.rug.runtime.ResponseHandler
import com.atomist.rug.runtime.plans._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.Secret
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.util.JsonUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class JavaScriptCommandHandlerTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val kitties = "ShowMeTheKitties"
  val kittyDesc = "Search Youtube for kitty videos and post results to slack"
  val simpleCommandHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "KittieFetcher.ts"))

  val simpleCommandHandlerWithMappedParams = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "KittieFetcherWithMappedParams.ts"))

  val simpleCommandHandlerHandlerWithSecrets = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "KittieFetcherWithSecrets.ts"))

  val simpleCommandHandlerWithPresentable = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandWithPresentable.ts"))

  val simpleCommandHandlerExecuteInstructionCallingRespondable =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      contentOf(this, "SimpleCommandHandlerExecuteInstructionCallingRespondable.ts"))

  val simpleCommandHandlerReturningMessage = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningMessage.ts"))

  val simpleCommandHandlerReturningEmptyMessage = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningEmptyMessage.ts"))

  val simpleCommandHandlerWithNullDefault = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerWithNullDefault.ts"))

  val simpleCommandHandlerWithBadStubUse = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerWithBadStubUse.ts"))

  "JavaScriptCommandHandler" should "allow us to return an empty message" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(simpleCommandHandlerReturningEmptyMessage))
    val rugs = RugArchiveReader(rugArchive)
    val com = rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
    assert(plan.messages.size === 1)
    assert(plan.messages.head.body.value == null)
    assert(JsonUtils.toJson(plan.messages.head) == """{"body":{},"instructions":[]}""")
  }

  it should "#488: get good error message from generated model stubs" in {
    val rugArchive = TypeScriptBuilder.compileWithExtendedModel(
      SimpleFileBasedArtifactSource(simpleCommandHandlerWithBadStubUse))
    val rugs = RugArchiveReader(rugArchive)
    val commandHandler = rugs.commandHandlers.head
    try {
      commandHandler.handle(null, SimpleParameterValues.Empty)
      fail("Should have failed with exception")
    }
     catch {
       case ex: Throwable =>
         assert(ex.getMessage.contains("stub"))
     }
  }

  it should "be able to schedule an Execution and handle its response" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerExecuteInstructionCallingRespondable))
    val rugs = RugArchiveReader(rugArchive)
    val com = rugs.commandHandlers.head
    val responseHandler = rugs.responseHandlers.head

    val plan = com.handle(null,SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(responseHandler, null, null, new TestSecretResolver(null) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = Nil}))

    val results = Await.result(runner.run(plan, None), 10.seconds)
    PlanUtils.drawEventLogs("testplan",results.log)
    results.log.foreach {
      case i: InstructionResult =>
      case e :InstructionError => e.error.printStackTrace()
      case i => println("Ga:" + i)
    }
  }

  it should "extract and run a command handler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))

    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be(kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name", "el duderino")))
  }

  it should "parse messages containing Presentables" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWithPresentable))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    handlers.size should be(1)
    val handler = handlers.head
    handler.name should be(kitties)
    handler.description should be(kittyDesc)
    handler.tags.size should be(3)
    handler.intent.size should be(2)
    val plan = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("owner", "his dudeness"), SimpleParameterValue("name", "el duderino")))
  }

  it should "throw exceptions if required parameters are not set" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandler))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val handler = handlers.head
    assertThrows[MissingParametersException] {
      handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues.Empty)
    }
  }

  it should "set mapped properties correctly if present" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerWithMappedParams))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val handler = handlers.head
    handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name", "el duderino")))
  }
  it should "expose the required secrets on a CommandHandler" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerHandlerWithSecrets))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    assert(handlers.head.secrets.size === 2)
    assert(handlers.head.secrets.head === Secret("atomist/user_token", "atomist/user_token"))
    assert(handlers.head.secrets(1) === Secret("atomist/showmethemoney", "atomist/showmethemoney"))
  }

  it should "resolve secrets from the secret resolver" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(simpleCommandHandlerHandlerWithSecrets))
    val finder = new JavaScriptCommandHandlerFinder()
    val handlers = finder.find(new JavaScriptContext(rugArchive))
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(fn, null, null, new TestSecretResolver(handlers.head) {
      /**
        * Resolve a bunch of secrets at once
        *
        * @param secrets set of Secrets
        * @return a mapping from secret name (could be same as path) to actual secret value
        */
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }))
    Await.result(runner.run(handlers.head.handle(null, SimpleParameterValues.Empty).get, None), 10.seconds).log.foreach {
      case i:
        InstructionResult =>
        assert(i.response.status === Status.Success)
      case i => println(i.getClass)
    }
  }

  it should "Not fail if parameter has default value 'null' https://github.com/atomist/rug/issues/458" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(simpleCommandHandlerWithNullDefault))
    val rugs = RugArchiveReader(rugArchive)
    val com = rugs.commandHandlers.head
    val plan = com.handle(null,SimpleParameterValues.Empty).get
  }
}

class TestResponseHandler(r: ResponseHandler) extends ResponseHandler {

  override def handle(response: Response, params: ParameterValues): Option[Plan] = {
    None
  }

  override def name: String = r.name

  override def description: String =r.description

  override def tags: Seq[Tag] = r.tags

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    *
    * @return a list of parameters
    */
  override def parameters: Seq[Parameter] = ???
}
