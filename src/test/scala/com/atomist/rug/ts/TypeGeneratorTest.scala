package com.atomist.rug.ts

import com.atomist.source.{ArtifactSource, ArtifactSourceUtils}
import org.scalatest.{FlatSpec, Matchers}

class TypeGeneratorTest extends FlatSpec with Matchers {

  import TypeGenerator.CortexJson

  private val typeGen = new TypeGenerator

  "Type generation" should "find some types" in {
    val types = typeGen.extract(CortexJson)
    assert(types.nonEmpty)
  }

  it should "return types with operations" in {
    val types = typeGen.extract(CortexJson)
    types.foreach(t => {
      assert(t.operations.nonEmpty, s"Type ${t.name} should have operations")
    })
  }

  it should "generate compiling node module" in {
    val as = typeGen.toNodeModule(CortexJson)
      .withPathAbove(".atomist/rug")
     //println(ArtifactSourceUtils.prettyListFiles(as))
    as.allFiles.filter(_.name.endsWith(".ts")).foreach(f =>
      println(s"${f.path}\n${f.content}\n\n"))
    val cas = TypeScriptBuilder.compiler.compile(as + TypeScriptBuilder.compileUserModel(Seq(
      TypeScriptBuilder.coreSource,
      as
    )))
    //println(ArtifactSourceUtils.prettyListFiles(cas))

    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.ts")))
    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.js")))
    assert(!cas.allFiles.exists(_.content.contains("started_at")))
    assert(cas.allFiles.exists(_.content.contains("startedAt")))
    assert(cas.allFiles.exists(_.content.contains("Repo[]")))
    assert(cas.allFiles.exists(_.content.contains("Issue[]")), "Must have back relationship from Repo to Issue")
  }

}

object TypeGeneratorTest {

  private val typeGen = new TypeGenerator

  val fullModel: ArtifactSource = {
    val as = typeGen.toNodeModule(TypeGenerator.CortexJson)
      .withPathAbove(".atomist/rug")
    TypeScriptBuilder.compiler.compile(as + TypeScriptBuilder.compileUserModel(Seq(
      TypeScriptBuilder.coreSource,
      as
    )))
  }

}
