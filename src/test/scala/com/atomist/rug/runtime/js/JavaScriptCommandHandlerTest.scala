package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.common.MissingParametersException
import com.atomist.rug.runtime.ResponseHandler
import com.atomist.rug.runtime.RugScopes.Scope
import com.atomist.rug.runtime.plans._
import com.atomist.rug.spi.Handlers._
import com.atomist.rug.spi.Secret
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{RugArchiveReader, RugRuntimeException, SimpleRugResolver}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.helpers.NOPLogger

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

  val simpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction.ts"))

  val simpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction2 = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction2.ts"))

  val simpleCommandHandlerReturningDirectedMessageWithInstructions = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningDirectedMessageWithInstructions.ts"))

  val simpleCommandHandlerReturningUpdatableMessageWithMessageIdAndTimestamp = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningUpdatableMessageWithMessageIdAndTimestamp.ts"))

  val simpleCommandHandlerReturningEmptyMessage = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerReturningEmptyMessage.ts"))

  val simpleCommandHandlerWithNullDefault = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerWithNullDefault.ts"))

  val simpleCommandHandlerWithBadStubUse = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerWithBadStubUse.ts"))

  val simpleCommandHandlerWithSameParametersConfig = StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
    contentOf(this, "SimpleCommandHandlerWithSameParametersConfig.ts"))

  "command handler" should "not overwrite parameters when we reuse the same parameters object" in {
    val rugArchive = TypeScriptBuilder.compileWithExtendedModel(
      SimpleFileBasedArtifactSource(simpleCommandHandlerWithSameParametersConfig))
    val rugs = RugArchiveReader(rugArchive)
    val handler = rugs.commandHandlers.head
    val params = handler.parameters
    assert(params.head.name == "foo")
    assert(params(1).name == "bar")
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
    val resolver = SimpleRugResolver(rugArchive)
    val rugs = resolver.resolvedDependencies.rugs
    val com = rugs.commandHandlers.head
    val responseHandler = rugs.responseHandlers.head

    val plan = com.handle(null, SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(responseHandler, null, null, new TestSecretResolver(null) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }, rugResolver = Some(resolver)), loggerOption = Some(NOPLogger.NOP_LOGGER))
    val results = runner.run(plan, None)
    val single = PlanResultInterpreter.interpret(results)
    assert(single.status == Status.Success)
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
    val plano = handler.handle(LocalRugContext(TestTreeMaterializer), SimpleParameterValues(SimpleParameterValue("name", "el duderino")))
    assert(plano.nonEmpty)
    val plan = plano.get
    assert(plan.lifecycle.isEmpty)
    assert(plan.local.size == 2)
    assert(plan.instructions.size == 2)
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
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }))
    runner.run(handlers.head.handle(null, SimpleParameterValues.Empty).get, None).log.foreach {
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
    val plan = com.handle(null, SimpleParameterValues.Empty).get
  }

  val instructionWithNonExistentRugFunction =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      contentOf(this, "CommandHandlerWithNonExistentRugFunction.ts"))

  it should "return error responses if rug function is not found" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(instructionWithNonExistentRugFunction))
    val resolver = SimpleRugResolver(rugArchive)
    val rugs = resolver.resolvedDependencies.rugs
    val com = rugs.commandHandlers.head
    val responseHandler = rugs.responseHandlers.head

    val plan = com.handle(null, SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(responseHandler, null, null, new TestSecretResolver(null) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = Nil
    }, rugResolver = Some(resolver)), loggerOption = Some(NOPLogger.NOP_LOGGER))
    val results = runner.run(plan, None)
    val msg = results.log(1).asInstanceOf[InstructionResult].response.body.get.asInstanceOf[Plan].local.head.body
    assert(msg.contains("Cannot find Rug Function NonExistent"))
  }

  val handlerWithFailingRugFunction =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      contentOf(this, "HandlerWithFailingRugFunction.ts"))

  it should "return error responses if rug function throws exceptions" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(handlerWithFailingRugFunction))
    val resolver = SimpleRugResolver(rugArchive)
    val rugs = resolver.resolvedDependencies.rugs
    val com = rugs.commandHandlers.head

    val plan = com.handle(null, SimpleParameterValues.Empty).get
    val runner = new LocalPlanRunner(null, new LocalInstructionRunner(com, null, null, new TestSecretResolver(null) {
      override def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
        assert(secrets.size === 1)
        assert(secrets.head.name === "very")
        assert(secrets.head.path === "/secret/thingy")
        Seq(SimpleParameterValue("very", "cool"))
      }
    }, rugResolver = Some(resolver)), loggerOption = Some(NOPLogger.NOP_LOGGER))
    val results = runner.run(plan, None)
    val msg = results.log.head.asInstanceOf[InstructionResult].response.body.get.asInstanceOf[RugRuntimeException].getMessage
    assert(msg.contains("uh oh"))
  }

  it should "return plan with DirectedMessage containing instructions" in
    directed(simpleCommandHandlerReturningDirectedMessageWithInstructions)

  it should "return plan with UpdatableMessage containing id and timestamp" in {
    val plan = directed(simpleCommandHandlerReturningUpdatableMessageWithMessageIdAndTimestamp)
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].messageId.get === "some-message")
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].timestamp.isEmpty)
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].ttl.isDefined)
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].post.get === "update_only")
  }


  private def directed(f: FileArtifact): Plan = {
    val rugArchive = TypeScriptBuilder.compileWithModel(
      SimpleFileBasedArtifactSource(f))
    val rugs = RugArchiveReader(rugArchive)
    val com = rugs.commandHandlers.head
    assert(com.name === "ShowMeTheKitties")
    val plan = com.handle(null, SimpleParameterValues.Empty).get
    assert(plan.messages.length === 1)
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].instructions.length === 1)
    assert(plan.messages.head.asInstanceOf[LocallyRenderedMessage].instructions.head.id.get === "123")
    plan
  }

  it should "return plan with DirectedMessage containing instructions created by function" in
    directed(simpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction)

  it should "return plan with DirectedMessage containing instructions created by function without annotations" in
    directed(simpleCommandHandlerReturningDirectedMessageWithInstructionsCreatedByFunction2)

  val handlerWithoutExplicitName =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      contentOf(this, "HandlerWithoutExplicitName.ts"))

  it should "should be able to extract name from constructor" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(handlerWithoutExplicitName))
    val resolver = SimpleRugResolver(rugArchive)
    val rugs = resolver.resolvedDependencies.rugs
    val com = rugs.commandHandlers.head
    assert(com.name == "FunctionKiller")
  }

  val handlerWithIntegrationTestDecorator =
    StringFileArtifact(atomistConfig.handlersRoot + "/Handler.ts",
      contentOf(this, "HandlerIntegrationTest.ts"))

  it should "add test metadata if the @IntegrationTest decorator is used" in {
    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(handlerWithIntegrationTestDecorator))
    val resolver = SimpleRugResolver(rugArchive)
    val rugs = resolver.resolvedDependencies.rugs
    assert(rugs.commandHandlers.nonEmpty)
    val test = rugs.commandHandlers.head.asInstanceOf[JavaScriptCommandHandler]
    assert(test.testDescriptor.nonEmpty)
    val td = test.testDescriptor.get
    assert(td.description == "Test some flow")
    assert(td.kind == "integration")
  }
}

class TestResponseHandler(r: ResponseHandler) extends ResponseHandler {

  override def handle(ctx: RugContext, response: Response, params: ParameterValues): Option[Plan] = {
    None
  }

  override def scope: Scope = r.scope
  override def name: String = r.name

  override def description: String = r.description

  override def tags: Seq[Tag] = r.tags

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    *
    * @return a list of parameters
    */
  override def parameters: Seq[Parameter] = ???
}
