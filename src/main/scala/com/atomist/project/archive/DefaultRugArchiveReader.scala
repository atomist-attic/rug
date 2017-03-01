package com.atomist.project.archive

import com.atomist.rug.EmptyRugDslFunctionRegistry
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.AddressableRug
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource

/**
  * Use to read _all_ rugs from an archive
  */
class DefaultRugArchiveReader(atomistConfig: AtomistConfig = DefaultAtomistConfig,
                              evaluator: Evaluator = new DefaultEvaluator(new EmptyRugDslFunctionRegistry),
                              typeRegistry: TypeRegistry = DefaultTypeRegistry)

  extends RugArchiveReader{

  private val readers: Seq[RugArchiveReader] = Seq(
    new JavaScriptRugArchiveReader())

  /**
    *
    * @param as
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  override def find(as: ArtifactSource, otherRugs: Seq[AddressableRug]): Rugs = {
    readers.foldLeft[Rugs](new Rugs(Nil,Nil,Nil,Nil,Nil,Nil)){ (acc, reader) =>
      val rugs = reader.find(as,otherRugs)
      //TODO - is there a fancy way to do this?
     val result =  new Rugs(
        rugs.editors ++ acc.editors,
        rugs.generators ++ acc.generators,
        rugs.reviewers ++ acc.reviewers,
        rugs.commandHandlers ++ acc.commandHandlers,
        rugs.eventHandlers ++ acc.eventHandlers,
        rugs.responseHandlers ++ acc.responseHandlers)
      //tell the rugs about one another
      result.allRugs.foreach(r => r.addToArchiveContext(result.allRugs))
      result
    }

  }
}
