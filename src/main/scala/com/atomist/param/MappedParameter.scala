package com.atomist.param

/**
  * Maps CommandHandler injected field names to predefined/bot defined parameters
  *
  * See MappedParameters in Handlers.ts for examples.
  *
  * @param localKey - name on the CommandHandler instance
  * @param foreignKey - name known by some external source - say bot
  */
case class MappedParameter (localKey: String, foreignKey: String)
