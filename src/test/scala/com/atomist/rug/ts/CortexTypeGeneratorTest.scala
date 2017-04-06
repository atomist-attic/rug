package com.atomist.rug.ts

import com.atomist.rug.spi.EnumParameterOrReturnType
import com.atomist.source.ArtifactSource
import com.atomist.util.Utils
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

class CortexTypeGeneratorTest extends FlatSpec with Matchers {

  import CortexTypeGenerator._
  import DefaultTypeGeneratorConfig.CortexJson

  private val typeGen = new CortexTypeGenerator(DefaultCortexDir, DefaultCortexStubDir)

  "Type generation" should "find some types" in {
    val types = typeGen.extract(CortexJson)
    assert(types.nonEmpty)
  }

  it should "handle enums" in pendingUntilFixed {
    val enumJson =
      Utils.withCloseable(getClass.getResourceAsStream("/com/atomist/rug/ts/enum_test.json"))(IOUtils.toString(_, "UTF-8"))
    val types = typeGen.extract(enumJson)
    assert(types.size === 1)
    val t = types.head
    t.allOperations.last.returnType shouldBe a[EnumParameterOrReturnType]
  }

  it should "return types with operations" in {
    val types = typeGen.extract(CortexJson)
    types.foreach(t => {
      assert(t.operations.nonEmpty, s"Type ${t.name} should have operations")
    })
  }

  it should "generate compiling node module" in {
    val extendedModel = typeGen.toNodeModule(CortexJson)
      .withPathAbove(".atomist/rug")
    //println(ArtifactSourceUtils.prettyListFiles(as))
    //        extendedModel.allFiles.filter(_.name.endsWith(".ts")).foreach(f =>
    //          println(s"${f.path}\n${f.content}\n\n"))
    val cas = TypeScriptBuilder.compiler.compile(extendedModel + TypeScriptBuilder.compileUserModel(Seq(
      TypeScriptBuilder.coreSource,
      extendedModel
    )))
    //println(ArtifactSourceUtils.prettyListFiles(cas))

    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.ts")))
    assert(cas.allFiles.exists(_.name.endsWith("ChatChannel.js")))
    assert(cas.allFiles.exists(_.content.contains("Repo[]")))
    assert(cas.allFiles.exists(_.content.contains("Issue[]")), "Must have back relationship from Repo to Issue")

    val buildFile = cas.allFiles.find(f => f.content.contains("class Build ")).get
    assert(buildFile.content.contains("""[ "Build", "-dynamic""""), "We should have correct node tags")
  }
}

object CortexTypeGeneratorTest {

  private val typeGen = new CortexTypeGenerator(CortexTypeGenerator.DefaultCortexDir, CortexTypeGenerator.DefaultCortexStubDir)

  val fullModel: ArtifactSource = {
    val as = typeGen.toNodeModule(DefaultTypeGeneratorConfig.CortexJson)
      .withPathAbove(".atomist/rug")
    TypeScriptBuilder.compiler.compile(as + TypeScriptBuilder.compileUserModel(Seq(
      TypeScriptBuilder.coreSource,
      as
    )))
  }
}
