package com.atomist.rug.kind.elm.ElmModel

import com.atomist.tree.content.text._
import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels.{ElmExpression, ElmPattern}
import com.atomist.rug.kind.elm.ElmModel.ElmTypeModels.{ElmTypeSpecification, ElmTypeWithParameters}
import com.atomist.rug.kind.elm.{ElmModuleType, ElmParser, ElmParserCombinator}
import com.atomist.tree.{SimpleTerminalTreeNode, TreeNode}

/**
  * Elm top-level declarations include:
  * - port
  * - union type
  * - type alias
  * - function
  * - constant
  *   - simple constant (acts like a no-arg function)
  *   - patterned constant
  *
  * (this is all that's modeled so far)
  */
object ElmDeclarationModels {

  sealed trait ElmDeclaration extends TreeNode {
    def declaredIdentifier: String
  }

  class ElmPortDeclaration(name: MutableTerminalTreeNode,
                           typeSpec: ElmTypeSpecification)
    extends ParsedMutableContainerTreeNode("port")
      with ElmDeclaration {
    def declaredIdentifier: String = name.value

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil
  }

  class ElmTypeAlias(
                      nameField: MutableTerminalTreeNode,
                      val initialAlias: ElmTypeSpecification)
    extends ParsedMutableContainerTreeNode("type-alias") with ElmDeclaration {

    override def declaredIdentifier: String = nameField.value

    override def childNodeNames: Set[String] = Set()

    addType(ElmModuleType.TypeAliasAlias)

    override def childrenNamed(key: String): Seq[TreeNode] = Seq(alias)

    private var _alias = initialAlias

    insertFieldCheckingPosition(nameField)
    insertFieldCheckingPosition(initialAlias)

    def alias: ElmTypeSpecification = _alias

    def typeAliasName: String = nameField.value

    def setTypeAliasName(newName: String): Unit = nameField.update(newName)

    def replaceBody(newBody: String): Unit = {
      val newNode = ElmParserCombinator.parseProduction(ElmParserCombinator.TypeSpecifications.recordType, newBody)
      replaceField(_alias, newNode)
      _alias = newNode
    }
  }

  class ElmUnionType(val typeName: String, initialValues: Seq[ElmTypeWithParameters])
    extends ParsedMutableContainerTreeNode("union-type") with ElmDeclaration {

    override def declaredIdentifier: String = typeName

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    private var _values = initialValues

    appendFields(initialValues)

    def values: Seq[ElmTypeWithParameters] = _values

    def replace(contents: String): Unit = {
      val wholeUnionType =
        s"type $typeName\n" +
          s"    = $contents"
      val newNode = ElmParserCombinator.parseProduction(ElmParserCombinator.elmUnionType, wholeUnionType)

      replaceFields(newNode.childNodes)
    }

    def add(elmTypeWithParametersAsString: String): Unit = {
      val parsed = ElmParserCombinator.parseProduction(ElmParserCombinator.TypeSpecifications.typeWithParameters, elmTypeWithParametersAsString)
      _values = values :+ parsed
      val separator = SimpleTerminalTreeNode("ctor-sep", "\n    | ")
      appendField(separator)
      appendField(parsed)
    }
  }

  import ElmFunctionHelpers._

  class ElmFunction(
                     nameField: MutableTerminalTreeNode,
                     initialBody: ElmExpression,
                     val parameterNames: Seq[ElmPattern] = Seq(),
                     initialElmType: Option[(MutableTerminalTreeNode, ElmTypeSpecification)] = None)
    extends ParsedMutableContainerTreeNode("function")
      with ElmDeclaration
      with CanBeThoughtOfAsAFunction
      with IHaveAMutableBody {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    override def declaredIdentifier: String = functionName

    private var _elmType: Option[ElmTypeSpecification] = initialElmType.map(_._2)

    def elmType = _elmType

    initialElmType foreach { case (name, typeSpec) =>
      insertFieldCheckingPosition(name)
      insertFieldCheckingPosition(typeSpec)
    }
    insertFieldCheckingPosition(nameField)
    initBody(initialBody)

    addType(ElmModuleType.FunctionAlias)

    def functionName: String = nameField.value

    def setFunctionName(name: String): Unit = {
      nameField.update(name)
      initialElmType.foreach { case (nameFieldFromTypeDeclaration, _) => nameFieldFromTypeDeclaration.update(name) }
    }

    def changeType(newType: String): Unit = {
      val newTypeStructure = ElmParserCombinator.parseProduction(ElmParserCombinator.TypeSpecifications.typeSpecification, newType)
      elmType match {
        case Some(et) =>
          replaceField(et, newTypeStructure)
          _elmType = Some(newTypeStructure)
        case None =>
          // TODO should allow this
          throw new IllegalArgumentException(s"No type specified in $this: Cannot change type")
      }
    }
  }

  class ElmPatternedConstantDeclaration(pattern: ElmPattern, initialBody: ElmExpression)
    extends ParsedMutableContainerTreeNode("patterned constant")
      with ElmDeclaration {
    override def declaredIdentifier: String = ???

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  class ElmSimpleConstantDeclaration(nameField: MutableTerminalTreeNode, initialBody: ElmExpression, initialElmType: Option[(MutableTerminalTreeNode, ElmTypeSpecification)] = None)
    extends ParsedMutableContainerTreeNode("simple constant")
      with ElmDeclaration
      with CanBeThoughtOfAsAFunction
      with IHaveAMutableBody {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    override def declaredIdentifier: String = nameField.value

    private var _elmType: Option[ElmTypeSpecification] = initialElmType.map(_._2)

    def elmType = _elmType

    initialElmType foreach { case (name, typeSpec) =>
      insertFieldCheckingPosition(name)
      insertFieldCheckingPosition(typeSpec)
    }
    insertFieldCheckingPosition(nameField)
    initBody(initialBody)

    def functionName: String = nameField.value

    def setFunctionName(name: String): Unit = {
      nameField.update(name)
      initialElmType.foreach { case (nameFieldFromTypeDeclaration, _) => nameFieldFromTypeDeclaration.update(name) }
    }

    def changeType(newType: String): Unit = {
      val newTypeStructure = ElmParserCombinator.parseProduction(ElmParserCombinator.TypeSpecifications.typeSpecification, newType)
      elmType match {
        case Some(et) =>
          replaceField(et, newTypeStructure)
          _elmType = Some(newTypeStructure)
        case None =>
          // TODO should allow this
          throw new IllegalArgumentException(s"No type specified in $this: Cannot change type")
      }
    }
  }

}

object ElmFunctionHelpers {

  trait CanBeThoughtOfAsAFunction {
    def functionName: String

    def setFunctionName(name: String): Unit

    def body: ElmExpression

    def replaceBody(newBody: String): Unit

    def elmType: Option[ElmTypeSpecification]

    def changeType(newType: String): Unit
  }

  trait IHaveAMutableBody {
    self: AbstractMutableContainerTreeNode =>

    private var _body: ElmExpression = _

    protected def initBody(initialBody: ElmExpression) = {
      _body = initialBody
      insertFieldCheckingPosition(initialBody)
    }

    def body = _body

    def replaceBody(newBody: String): Unit = {
      val newBodyExpression = {
        // TODO move this into parser if it's needed elsewhere
        val markedUp = ElmParser.markLinesThatAreLessIndented(" " + newBody)
        ElmParserCombinator.parseProduction(ElmParserCombinator.ElmExpressions.expression, markedUp)
      }
      replaceField(body, newBodyExpression)
      _body = newBodyExpression
    }
  }

}
