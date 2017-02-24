package com.atomist.rug.runtime.plans

import com.atomist.param.{MappedParameter, ParameterValues, SimpleParameterValue, Tag}
import com.atomist.rug.{InvalidSecretException, MissingSecretException}
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.Secret
import org.scalatest.{FlatSpec, Matchers}

class SecretResolverTest extends FlatSpec with Matchers {


  it should "replace secret tokens found in parameters" in {
    val resolver = new TestSecretResolver(handler)
    val replaced = resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string #{secret/path}")))
    assert(replaced.head.getValue === "complicated string super-secret-value-1")
  }

  it should "should be happy with fancy paths" in {
    val resolver = new TestSecretResolver(handler)
    val replaced = resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string #{secret/path/blah?key=value&other=blah}")))
    assert(replaced.head.getValue === "complicated string super-secret-value-4")
  }

  it should "not mess up parameters without tokens" in {
    val resolver = new TestSecretResolver(handler)
    val replaced = resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string ${secret/path}")))
    assert(replaced.head.getValue === "complicated string ${secret/path}")
  }

  it should "not replace secrets for paths that aren't declared on the rug" in {
    val resolver = new TestSecretResolver(handler)
    assertThrows[MissingSecretException]{
      resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string #{secret/otherpath}")))
    }
  }

  it should "should not allow invalid secrets in  the handlers secrets in the first place" in {
    val resolver = new TestSecretResolver(invalidSecretHandler)
    assertThrows[InvalidSecretException]{
      resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string #{secret/otherpath}")))
    }
  }

  it should "not worry about #{} patterns that span multiple lines or are clearly not tokens" in {
    val resolver = new TestSecretResolver(handler)
    resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "complicated string #{secret\n/otherpath}")))
    resolver.replaceSecretTokens(Seq(SimpleParameterValue("paramName", "#{secret{/otherpath}")))
  }
}

object handler extends CommandHandler {

  override def intent: Seq[String] = ???

  override def handle(ctx: RugContext, params: ParameterValues): Option[Plan] = ???

  override def mappedParameters: Seq[MappedParameter] = ???

  override def secrets: Seq[Secret] = Seq(Secret("some-secret", "secret/path"), Secret("blah","secret/path/blah?key=value&other=blah"))

  override def name: String = "somehandler"

  override def description: String = ???

  override def tags: Seq[Tag] = ???
}

object invalidSecretHandler extends CommandHandler {

  override def intent: Seq[String] = ???

  override def handle(ctx: RugContext, params: ParameterValues): Option[Plan] = ???

  override def mappedParameters: Seq[MappedParameter] = ???

  override def secrets: Seq[Secret] = Seq(Secret("some-secret", "secret/path"), Secret("blah","secret/path?&key="))

  override def name: String = "somehandler"

  override def description: String = ???

  override def tags: Seq[Tag] = ???
}
