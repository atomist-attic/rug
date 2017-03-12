package com.atomist.rug.ts

import com.atomist.rug.TestUtils
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils}
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
      //println(t)
      assert(t.operations.nonEmpty, s"Type ${t.name} should have operations")
    })
  }

  it should "generate compiling node module" in {
    val as = typeGen.toNodeModule(theJson)
      .withPathAbove(".atomist/rug")
    //println(ArtifactSourceUtils.prettyListFiles(as))
    val cas = TypeScriptBuilder.compiler.compile(as + TypeScriptBuilder.compileUserModel(Seq(
      TypeScriptBuilder.coreSource,
      as
    )))
    println(ArtifactSourceUtils.prettyListFiles(cas))
//    cas.allFiles.foreach(f =>
//      println(s"${f.path}\n${f.content}\n\n"))
    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.ts")))
    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.js")))
  }

}


object TypeGeneratorTest {

  private val typeGen = new TypeGenerator

  lazy val theJson: String =
    TestUtils.resourcesInPackage(this).allFiles
      .filter(_.name == "extra_types.json")
      .head.content

  val as = typeGen.toNodeModule(theJson)
    .withPathAbove(".atomist/rug")

  val fullModel: ArtifactSource = TypeScriptBuilder.compiler.compile(as + TypeScriptBuilder.compileUserModel(Seq(
    TypeScriptBuilder.coreSource,
    as
  )))
}