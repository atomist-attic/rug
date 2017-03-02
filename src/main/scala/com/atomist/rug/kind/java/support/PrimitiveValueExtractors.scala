package com.atomist.rug.kind.java.support

case class SourcePaths(
                        baseSourcePath: String,
                        baseTestPath: String
                      )

import com.atomist.util.lang.JavaConstants._

object DefaultSourcePaths extends SourcePaths(
  DefaultBaseSourcePath,
  DefaultBaseTestPath
)