package com.atomist.rug.runtime.js.interop

import com.atomist.util.lang.JavaScriptArray
import org.scalatest.{FlatSpec, Matchers}

class jsScalaHidingProxyTest extends FlatSpec with Matchers {

  import NashornUtilsTest._

  case class Animal(name: String, age: Int, friends: Seq[Animal] = Nil, mate: Option[Animal] = None)

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
    engine.put("proxy", proxy)
    val r = engine.eval("proxy.name")
    assert(r === fido.name)
    assert(engine.eval("proxy.age") === 6)
  }

  it should "allow toString" in {
    val fido = Animal("Fido", 6)
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.put("proxy", proxy)
    engine.eval("var s = '' + proxy")
  }

  it should "convert seq property access via property" in {
    val fido = Animal("Fido", 6, Seq(Animal("Rover", 2)))
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.put("proxy", proxy)
    assert(engine.eval("proxy.mate") === null)
    engine.eval("proxy.friends") match {
      case friends: JavaScriptArray[Animal]@unchecked =>
        assert(friends.size() === 1)
        assert(friends.get(0).name === "Rover")
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "convert option property to nullable" in {
    val fido = Animal("Fido", 6, mate = Some(Animal("Rover", 2)))
    val proxy = jsScalaHidingProxy(fido)
    val engine = createEngine
    engine.put("proxy", proxy)
    val mate = engine.eval("proxy.mate")
    mate match {
      case p: jsScalaHidingProxy =>
        assert(p.getMember("name") === "Rover")
      case _ => fail
    }
  }
}
