package com.atomist.rug.kind.elm

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels.{ElmCase, ElmCaseClause}
import com.atomist.rug.kind.elm.ElmModuleType._
import com.atomist.rug.spi._

class ElmCaseMutableView(
                          ec: ElmCase,
                          parent: ElmFunctionMutableView)
  extends TreeViewSupport[ElmCase](ec, parent) {

  override def nodeType: String = ElmModuleType.CaseAlias

  override def childNodeNames: Set[String] = Set(CaseClauseAlias)

  override def childNodeTypes: Set[String] = childNodeNames

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case CaseClauseAlias => ec.clauses.map(cc => new ElmCaseClauseMutableView(cc, this))
    case x => throw new RugRuntimeException(null, s"Script error: No child with name [$x] of Elm case")
  }

  @ExportFunction(readOnly = true, description = "Return the match expression as a string")
  def matchAsString = ec.matchOn.value

  @ExportFunction(readOnly = false, description = "Add a case clause")
  def addClause(@ExportFunctionParameterDescription(name = "pattern", description = "Case clause pattern")
                pattern: String,
                @ExportFunctionParameterDescription(name = "expression", description = "Case clause expression")
                expression: String
               ): Unit = {
    // logger.debug(s"Renamed function ${ta.typeAliasName} to $newName")
    ec.addClause(pattern, expression)
  }

  @ExportFunction(readOnly = true, description = "The body of the case expression")
  def body = ec.value

  @ExportFunction(readOnly = false, description = "Replace body of the case expression")
  def replaceBody(@ExportFunctionParameterDescription(name = "newBody", description = "New case body")
                  newBody: String
                 ): Unit = {
    // logger.debug(s"Renamed function ${ta.typeAliasName} to $newName")
    ec.replaceBody(newBody)
  }

}

class ElmCaseClauseMutableView(
                                val ecc: ElmCaseClause,
                                parent: ElmCaseMutableView)
  extends TreeViewSupport[ElmCaseClause](ecc, parent) {

  override def childNodeTypes: Set[String] = childNodeNames

  @ExportFunction(readOnly = true, description = "String content of case clause")
  def body = ecc.value

  @ExportFunction(readOnly = false, description = "Replace body of the case clause")
  def replaceBody(@ExportFunctionParameterDescription(name = "newBody", description = "New case clause body")
                  newBody: String
                 ): Unit = {
    ecc.replaceBody(newBody)
  }

  override def childNodeNames: Set[String] = Set()

  override def children(fieldName: String): Seq[MutableView[_]] = Nil

}