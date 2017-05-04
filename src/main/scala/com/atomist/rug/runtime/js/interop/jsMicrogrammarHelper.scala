package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.runtime.js.interop.NashornUtils._
import com.atomist.tree.content.text.microgrammar._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._


/**
  * JavaScript-friendly facade to a MicrogrammarHelper.
  * Paralleled by a user model TypeScript interface.
  */
class jsMicrogrammarHelper(rugContext: RugContext) {

  import jsMicrogrammarHelper._

  def describe(microgrammar: Object): String =
    parseOrThrow(microgrammar).shortDescription


  /**
    * Evaluate the microgrammar; it must match starting at the first character.
    * @return a GraphNode that is a match, or a DismatchReport
    */
  def strictMatch(microgrammar: Object, string: String): AnyRef = {
    val mg = parseOrThrow(microgrammar)
    println(s"I am looking for ${mg.shortDescription}")
    val result = mg.strictMatch(string)
    result match {
      case Left(r) => DismatchReport.detailedReport(r, string)
      case Right(n) => new jsSafeCommittingProxy(n, DefaultTypeRegistry)
    }
  }

  private def parseOrThrow(microgrammar: Object): MatcherMicrogrammar =
    microgrammar match {
    case som: ScriptObjectMirror => parseMicrogrammar(som)
    case x =>
      throw new RugRuntimeException(null, s"Not a microgrammar $x")
  }
}

object jsMicrogrammarHelper {
  def looksLikeAMicrogrammar(som: ScriptObjectMirror): Boolean = {
    hasDefinedProperties(som, "name", "grammar", "submatchers")
  }

  def parseMicrogrammar(som: ScriptObjectMirror): MatcherMicrogrammar = {
    val name = stringProperty(som, "name")
    val grammar = stringProperty(som, "grammar")
    val submatchers = toJavaMap(som.getMember("submatchers"))
    MatcherMicrogrammarConstruction.matcherMicrogrammar(name, grammar, submatchers)
  }
}