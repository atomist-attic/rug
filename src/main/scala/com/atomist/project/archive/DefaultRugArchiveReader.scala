package com.atomist.project.archive

import com.atomist.rug.EmptyRugDslFunctionRegistry
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource

/**
  * Use to read _all_ rugs from an archive
  */
class DefaultRugArchiveReader(atomistConfig: AtomistConfig = DefaultAtomistConfig,
                              evaluator: Evaluator = new DefaultEvaluator(new EmptyRugDslFunctionRegistry),
                              typeRegistry: TypeRegistry = DefaultTypeRegistry)

  extends RugArchiveReader[Rug]{

  private val readers: Seq[RugArchiveReader[_ <: Rug]] = Seq(
    new JavaScriptRugArchiveReader(),
    new RugDslArchiveReader(atomistConfig,evaluator,typeRegistry))

  /**
    *
    * @param as
    * @param namespace
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  override def find(as: ArtifactSource, namespace: Option[String], otherRugs: Seq[Rug]): Rugs = {
    readers.foldLeft[Rugs](Rugs(Nil,Nil,Nil,Nil,Nil,Nil)){ (acc, reader) =>
      val rugs = reader.find(as,namespace,otherRugs)
      //TODO - is there a fancy way to do this?
      Rugs(
        rugs.editors ++ acc.editors,
        rugs.generators ++ acc.generators,
        rugs.reviewers ++ acc.reviewers,
        rugs.commandHandlers ++ acc.commandHandlers,
        rugs.eventHandlers ++ acc.eventHandlers,
        rugs.responseHandlers ++ acc.responseHandlers)
    }
  }
}
