package com.atomist.rug.runtime.js.nashorn

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.BaseRugContext
import com.atomist.rug.runtime.js.interop.{ExposeAsFunction, jsPathExpressionEngine}
import com.atomist.tree.SimpleTerminalTreeNode
import jdk.nashorn.api.scripting.JSObject
import org.scalatest.{FlatSpec, Matchers}

class jsScalaHidingProxyTest extends FlatSpec with Matchers {

  import com.atomist.rug.runtime.js.JavaScriptEngineTestUtils._

  case class Animal(name: String, age: Int, friends: Seq[Animal] = Nil, mate: Option[Animal] = None) {
    def friendsLike(arg: String) = friends.size

    // Forces use of a method to invoke
    @ExposeAsFunction
    def evilSideEffect() = "evilSideEffect"
  }

  "ScalaHidingProxy" should "expose simple case class" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    assert(proxy.getMember("name") === fido.name)
    assert(proxy.getMember("toString").toString.contains(fido.name))
  }

  it should "allow string property access via property" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    val r = engine.eval("proxy.name")
    assert(r === fido.name)
    assert(engine.eval("proxy.age") === 6)
  }

  it should "enforce method access access via function for @ExposeAsFunction method" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    val r = engine.eval("proxy.name")
    assert(engine.eval("proxy.evilSideEffect()") === "evilSideEffect")
  }

  it should "allow method access access via function" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    val r = engine.eval("proxy.name")
    assert(engine.eval("proxy.friendsLike('tom')") === 0)
  }

  it should "return undefined for unknown property" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    val r = engine.eval("proxy.absquatulate == undefined")
    assert(r == true)
  }

  it should "allow toString" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    engine.eval("var s = '' + proxy")
  }

  it should "convert seq property access via property" in {
    val fido = Animal("Fido", 6, Seq(Animal("Rover", 2)))
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    assert(engine.eval("proxy.mate") === null)
    engine.eval("proxy.friends") match {
      case friends: NashornJavaScriptArray[_]@unchecked =>
        assert(friends.size() === 1)
        assert(friends.get(0).asInstanceOf[jsScalaHidingProxy].getMember("name") === "Rover")
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "convert option property to nullable" in {
    val fido = Animal("Fido", 6, mate = Some(Animal("Rover", 2)))
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.setMember("proxy", proxy)
    val mate = engine.eval("proxy.mate")
    mate match {
      case p: jsScalaHidingProxy =>
        assert(p.getMember("name") === "Rover")
      case _ => fail
    }
  }

  it should "work with real world target" in {
    val node = SimpleTerminalTreeNode("foo", "bar")
    val rc = new TestContext(node)
    val proxy = jsScalaHidingProxy(rc)
    val engine = createEngine
    engine.setMember("context", proxy)
    val pex = engine.eval("context.pathExpressionEngine")
    pex should not be null
    pex match {
      case js: jsScalaHidingProxy =>
        assert(js.target.isInstanceOf[jsPathExpressionEngine])
      case x => fail(s"Unexpected: $x")
    }
    val withFunction = engine.eval("context.pathExpressionEngine.with")
    engine.eval("context.pathExpressionEngine.with")
    withFunction match {
      case js: JSObject if js.isFunction =>
      case x =>
        fail(s"Unexpected: $x")
    }
    engine.eval("context.contextRoot") match {
      case sp: jsScalaHidingProxy => assert(sp.target === node)
      case x => fail(s"Unexpected $x")
    }
  }
}

class TestContext(node: GraphNode) extends BaseRugContext {
  override def contextRoot(): GraphNode = node
}
