package com.atomist.rug.ts

import com.atomist.rug.TestUtils
import org.scalatest.{FlatSpec, Matchers}

class TypeGeneratorTest extends FlatSpec with Matchers {

  private val typeGen = new TypeGenerator

  lazy val theJson: String =
    TestUtils.resourcesInPackage(this).allFiles
      .filter(_.name == "extra_types.json")
        .head.content

  "Type generation" should "find some types" in {
    val types = typeGen.extract(theJson)
    assert(types.nonEmpty)
  }

  it should "return types with operations" in {
    val types = typeGen.extract(theJson)
    println(types)
    types.foreach(t => {
      println(t)
      assert(t.operations.nonEmpty, s"Type ${t.name} should have operations")
    })
  }

  it should "generate interfaces" in {
    val as = typeGen.toTypeScript(theJson)
    as.allFiles.foreach(f =>
      println(s"${f.path}\n${f.content}\n\n"))
  }

}
