package com.atomist.rug.kind.elm.ElmModel

import com.atomist.tree.content.text._
import com.atomist.rug.kind.elm.ElmModel.ElmDeclarationModels.ElmDeclaration
import com.atomist.rug.kind.elm.{ElmModuleType, ElmParser, ElmParserCombinator}
import com.atomist.tree.{PaddingTreeNode, SimpleTerminalTreeNode, TreeNode}

object ElmExpressionModels {

  sealed trait ElmExpression extends TreeNode

  class ElmAnonymousFunction(params: Seq[ElmPattern], body: ElmExpression)
    extends ParsedMutableContainerTreeNode("anonymous-function") with ElmExpression {

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  class ElmRecordField(name: MutableTerminalTreeNode, expr: ElmExpression)
    extends ParsedMutableContainerTreeNode("record-field-value-assignment") {

    def elmRecordFieldName: String = name.value

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil


    appendField(name)
    appendField(expr)
  }

  class ElmRecord(initialFields: Seq[ElmRecordField])
    extends ParsedMutableContainerTreeNode("elm-record") with ElmExpression {

    private var _fields: Seq[ElmRecordField] = initialFields

    appendFields(initialFields)

    def fields: Seq[ElmRecordField] = _fields

    override def childrenNamed(key: String): Seq[TreeNode] = fields.filter(n => n.nodeName.equals(key))

    def add(name: String, typ: String): Unit = {
      val oldValue = value
      val newRecordFieldAsString = s"$name = $typ"
      val newRecord =
        ElmParserCombinator.parseProduction(
          ElmParserCombinator.ElmExpressions.RecordExpressions.field,
          newRecordFieldAsString)
      newRecord.pad(newRecordFieldAsString)
      if (fields.nonEmpty) {
        val commaField = SimpleTerminalTreeNode("comma", " , ")
        addFieldAfter(fields.last, commaField)
        addFieldAfter(commaField, newRecord)
      }
      else {
        // Add it before the }
        val leftCurly = SimpleTerminalTreeNode("lc", "{ ")
        val rightCurly = SimpleTerminalTreeNode("rc", " }")
        replaceFields(Seq(leftCurly, newRecord, rightCurly))
      }
      _fields = _fields :+ newRecord
    }
  }

  /* represents { model | field = value, otherField = otherValue }, a copy-on-mod update of a record. */
  case class ElmRecordUpdate(startingRecordName: String, updates: Seq[ElmRecordField])
    extends ParsedMutableContainerTreeNode(startingRecordName) with ElmExpression {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  trait ElmIdentifierRef extends ElmExpression {
    def id: String
  }

  class ElmVariableReference(qualifiers: Seq[String], name: MutableTerminalTreeNode) extends
    ParsedMutableContainerTreeNode("variable-reference")
    with ElmExpression {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  case class ElmLetExpression(definitions: Seq[ElmDeclaration], body: ElmExpression)
    extends ParsedMutableContainerTreeNode("let-expression")
      with ElmExpression {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    // TODO will eventually need to have this in the AST but it seems to corrupt files right now
    // appendFields(definitions)
    insertFieldCheckingPosition(body)
  }

  class ElmIf(initialCondition: ElmExpression, action: ElmExpression)
    extends ParsedMutableContainerTreeNode(s"if-$initialCondition")
      with ElmExpression {

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  class ElmCondition(left: ElmExpression, op: String, right: ElmExpression)
    extends ParsedMutableContainerTreeNode(s"elm-condition")
      with ElmExpression {

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  class ElmCase(initialMatchOn: ElmExpression, initialClauses: Seq[ElmCaseClause])
    extends ParsedMutableContainerTreeNode(s"case-$initialMatchOn")
      with ElmExpression {

    private var _matchOn: ElmExpression = initialMatchOn
    private var _clauses: Seq[ElmCaseClause] = initialClauses

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    insertFieldCheckingPosition(_matchOn)
    for (c <- initialClauses) insertFieldCheckingPosition(c)

    addType(ElmModuleType.CaseAlias)

    def matchOn: ElmExpression = _matchOn

    def clauses: Seq[ElmCaseClause] = _clauses

    def patterns = clauses.map(_.left)

    // TODO this is ugly
    private val spaces = "        "

    def addClause(pattern: String, expression: String): Unit = {
      val clauseAsString = ElmParser.markLinesThatAreLessIndented(s" $pattern ->\n$spaces    $expression")
      val newClause = ElmParserCombinator.parseProduction(
        ElmParserCombinator.ElmExpressions.caseClause,
        clauseAsString)
      _clauses = _clauses :+ newClause
      // TODO could we copy another clause's padding
      val padding = SimpleTerminalTreeNode("clause_padding", s"\n\n$spaces")
      appendField(padding)
      // appendField(newClause)
      // appendField(SimpleScalarFieldValue("new-clause", clauseAsString))
      appendField(newClause)
    }

    def replaceBody(newBody: String): Unit = {
      // This blows up without position
      val newCase = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.caseExpression, newBody)
      this._matchOn = newCase.matchOn
      this._clauses = newCase.clauses
      replaceFields(Seq(SimpleTerminalTreeNode("replaced-case-body", newBody)))
    }
  }

  class ElmCaseClause(initialLeft: ElmPattern, initialRight: ElmExpression)
    extends ParsedMutableContainerTreeNode("") {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    addType(ElmModuleType.CaseClauseAlias)

    private var _left = initialLeft

    private var _right = initialRight

    def left = _left

    def right = _right

    def replaceBody(newBody: String): Unit = {
      val ecc = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.caseClause, newBody)
      _left = ecc.left
      _right = ecc.right
      replaceFields(Seq(PaddingTreeNode("updated-body", newBody)))
    }
  }

  trait ElmPattern

  case object MatchAnything extends ElmPattern

  case class MatchAnythingAndNameIt(id: String) extends ElmPattern

  case class UnionDeconstruction(constructor: String, parameters: Seq[ElmPattern]) extends ElmPattern

  class RecordPattern(fields: Seq[MutableTerminalTreeNode]) extends ElmPattern

  class TuplePattern(elements: Seq[ElmPattern]) extends ElmPattern

  class ElmFunctionApplication(val functionName: ElmExpression, val parameters: Seq[ElmExpression])
    extends ParsedMutableContainerTreeNode("elm-function-application")
      with ElmExpression {

    insertFieldCheckingPosition(functionName)
    parameters.foreach(insertFieldCheckingPosition(_))

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

  trait ElmInfixOperator extends TreeNode {
    def id: String
  }

  case class ElmInfixFunctionApplication(left: ElmExpression, op: ElmInfixOperator, right: ElmExpression)
    extends ParsedMutableContainerTreeNode("elm-function-application")
      with ElmExpression {

    insertFieldCheckingPosition(left)
    insertFieldCheckingPosition(op)
    insertFieldCheckingPosition(right)
    //def functionName: String

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

  class ElmRecordFieldAccess(record: ElmExpression, fields: Seq[MutableTerminalTreeNode])
    extends ParsedMutableContainerTreeNode("elm-record-field-access")
      with ElmExpression {
    insertFieldCheckingPosition(record)
    fields.foreach( field => insertFieldCheckingPosition(field))

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

  trait StringConstant
    extends ElmExpression {

    def s: String
  }

  trait IntConstant extends ElmExpression {

    def i: Int
  }

  case class ListLiteral(elements: Seq[ElmExpression])
    extends ParsedMutableContainerTreeNode("list-literal")
      with ElmExpression {
    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  case class ElmTuple(elements: Seq[ElmExpression])
    extends ParsedMutableContainerTreeNode("tuple")
      with ElmExpression {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    appendFields(elements)
  }
}