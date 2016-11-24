package com.atomist.rug.kind.http

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import org.springframework.web.client.{RestOperations, RestTemplate}

class HttpType(evaluator: Evaluator, httpClient: RestOperations)
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator, new RestTemplate())

  override def name = "http"

  override def description = "Execute http calls"

  override def viewManifest: Manifest[HttpMutableView] = manifest[HttpMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    Some(Seq(new HttpMutableView(httpClient)))
  }
}