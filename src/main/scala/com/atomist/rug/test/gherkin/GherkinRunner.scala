package com.atomist.rug.test.gherkin

import com.atomist.project.archive.{RugArchiveReader, Rugs}
import com.atomist.rug.runtime.js.JavaScriptContext
import com.typesafe.scalalogging.LazyLogging

/**
  * Combine Gherkin DSL BDD definitions with JavaScript backing code
  * and provide the ability to execute the tests
  * @param jsc JavaScript backed by a Rug archive
  */
class GherkinRunner(jsc: JavaScriptContext, rugs: Option[Rugs] = None) extends LazyLogging {

  import GherkinRunner._

  private val definitions = new Definitions(jsc)

  jsc.engine.put(DefinitionsObjectName, definitions)
  jsc.atomistContent
    .filter(_ => true, jsc.atomistConfig.isJsTest)
    .allFiles
    .foreach(f => {
      jsc.evaluate(f)
    })

  /**
    * Features found in this archive
    */
  val features: Seq[FeatureDefinition] = GherkinReader.findFeatures(jsc.rugAs)

  private val executableFeatures = features.map(f => new ProjectManipulationFeature(f, definitions, jsc.rugAs, rugs))

  /**
    * Execute all the tests in this archive
    */
  def execute(): ArchiveTestResult = {
    logger.info(s"Execute on $this")
    ArchiveTestResult(executableFeatures.map(ef => jsc.withEnhancedExceptions {
      println(s"Executing feature ${ef.definition.feature.getName}")
      val result = ef.execute()
      println(s"Completed feature ${ef.definition.feature.getName}")
      result
    }))
  }

  override def toString: String =
    s"${getClass.getSimpleName}: Features [${features}] found in $jsc"

}

object GherkinRunner {

  val DefinitionsObjectName: String = (getClass.getName + "_definitions").replace(".", "_")

}



