package com.atomist.rug.kind.java

import com.github.javaparser.ast.Node

object DocumentableNodeUtils {

  def javadoc(dn: Node): String = {
    if (dn.getComment.isPresent)
      dn.getComment.get().getContent
    else ""
  }
}
