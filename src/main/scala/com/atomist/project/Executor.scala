package com.atomist.project

import com.atomist.rug.kind.service.ServiceSource

trait Executor extends ProjectOperation {
  /**
    * Execute operation against the services provided by the given ServiceSource
    *
    * @param serviceSource service source returning services we are interested in
    * @param poa parameters to the operation
    */
  def execute(serviceSource: ServiceSource, poa: ProjectOperationArguments)
}
