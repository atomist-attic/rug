package com.atomist.param

/**
  * Maps CommandHandler injected field names to predefined/bot defined parameters
  *
  * See MappedParameters in Handlers.ts for examples.
  *
  * @param handlerFieldName - name on the CommandHandler instance
  * @param mappedName -
  */
case class MappedParameter (handlerFieldName: String, mappedName: String)
