package com.atomist.rug.kind.java

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
  * Type definition enabling us to select types on act on
  * from a codebase parsed with JavaParser.
  */
object TypeSelection {

  type TypeSelector = ClassOrInterfaceDeclaration => Boolean

  def bySimpleNameSelector(name: String): TypeSelector =
    coit => coit.getName.equals(name)
}
