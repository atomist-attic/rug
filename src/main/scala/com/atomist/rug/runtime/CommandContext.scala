package com.atomist.rug.runtime

import com.atomist.rug.runtime.js.interop.{TeamContext, UserServices}

/**
  * Sent to CommandHandlers
  */
trait CommandContext extends UserServices with TeamContext{

}
