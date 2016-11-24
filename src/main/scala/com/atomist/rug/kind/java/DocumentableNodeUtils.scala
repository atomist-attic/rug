package com.atomist.rug.kind.java

import com.github.javaparser.ast.DocumentableNode

object DocumentableNodeUtils {

  def javadoc(dn: DocumentableNode): String = {
    if (dn.getJavaDoc != null)
      dn.getJavaDoc.asLineComment().getContent
    else ""
  }
}
