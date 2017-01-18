package com.atomist.rug.kind.elm

import com.atomist.tree.content.text._
import com.atomist.rug.kind.elm.ElmModel.ElmDeclarationModels._
import com.atomist.rug.spi.Typed
import com.atomist.tree.{SimpleTerminalTreeNode, TerminalTreeNode, TreeNode}

package object ElmModel {

  sealed trait Exposing extends TreeNode {
    final override val nodeName: String = "exposing"
  }

  class FunctionNamesExposing(initialNames: Seq[TerminalTreeNode])
    extends ParsedMutableContainerTreeNode("exposing")
      with Exposing {

    var _names = initialNames
    appendFields(initialNames)

    def names = _names

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    def contains(name: String) = names.exists(_.value == name)

    def addExposure(localVariableReference: TerminalTreeNode): Unit = {
      _names = _names :+ localVariableReference
      appendField(SimpleTerminalTreeNode("comma", ","))
      appendField(localVariableReference)
    }

  }

  trait AllExposing extends Exposing

  /**
    * Model for an Elm module. Mutable. Capable of preserving positions
    * when updating. This is an exemplar of what will probably become a common pattern:
    * A structure consisting of updateable field types and structures in the FieldValue hierarchy (used for in-place string updates),
    * also exposing an AST for use by callers.
    * Simple updates can be performed by modifying UpdateableScalarFieldValue instanes.
    * More complex, structural updates, such as adding a new structure,
    * typically mean updating the AST model, but adding a String field for the
    * FieldValue hierarchy to ensure correct output. Padding can also be created.
    *
    * @param markedUpSource initial content, marked up
    * @param nameField
    * @param initialImports initial imports
    * @param declarations
    */
  class ElmModule(
                   markedUpSource: String,
                   val nameField: MutableTerminalTreeNode,
                   initialExposing: Exposing,
                   initialImports: Seq[Import] = Nil,
                   val declarations: Seq[ElmDeclaration] = Nil)
    extends ParsedMutableContainerTreeNode(nameField.value) {

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))


    private var _exposing = initialExposing

    addType(Typed.typeClassToTypeName(classOf[ElmModuleType]))

    def exposing: Exposing = _exposing

    // Populate initial fields
    // The AST isn't flat, but the fields are from the point of building our result string
    insertFieldCheckingPosition(nameField)
    insertFieldCheckingPosition(initialExposing)
    initialImports.foreach(insertFieldCheckingPosition(_))
    declarations.foreach(insertFieldCheckingPosition(_))

    def moduleName = nameField.value

    /**
      * Current source, after modifications
      *
      * @return
      */
    def currentSource: String = {
      val asString = ElmParser.unmark(value)
      asString
    }

    def functions = declarations.collect {
      case a: ElmFunction => a
      case b: ElmSimpleConstantDeclaration => b
    }

    def typeAliases = declarations.collect {
      case ta: ElmTypeAlias => ta
    }

    def types = declarations collect {
      case eut: ElmUnionType => eut
    }

    private var currentImports: Seq[Import] = initialImports

    def imports: Seq[Import] = currentImports

    /**
      *
      * @param newExposing new content of exposing. Does not include exposing keyword.
      *                    Will be either a CSV list or .. for all exposing.
      */
    def replaceExposing(newExposing: String): Unit = {
      val newExposingNode = ElmParserCombinator.parseProduction(ElmParserCombinator.exposing, newExposing)
      replaceField(_exposing, newExposingNode)
      _exposing = newExposingNode
    }

    // TODO parse the thing as a declaration
    def addFunctionBody(code: String): Unit = {
      // TODO what if no declarations
      val lastDeclaration = declarations.last
      val f = SimpleTerminalTreeNode("new-function", "\n\n\n" + code)
      addFieldAfter(lastDeclaration, f)
    }

    def removeFunction(name: String): Unit = {
      functions.find(_.functionName == name) match {
        case None => // Bonus
        case Some(function) => removeField(function)
      }
    }

    def addImportStatement(importText: String) = {
      val modifiedImportText = ElmParser.markLinesThatAreLessIndented(importText)

      // Position cannot be correct in this. It's not dirty, however, so it won't get written out
      val newImport: Import = ElmParserCombinator.parseProduction(ElmParserCombinator.elmImport, modifiedImportText)
      // Be sure to do this or updates won't be written up
      // newImport.moduleNameField.markDirty()

      if (currentImports.exists(_.moduleName == newImport.moduleName)) {
        currentImports
      } else {
        if (currentImports.isEmpty) {
          // We're the first import
          // If declarations is empty, add at the empty
          if (declarations.isEmpty) {
            val padding = SimpleTerminalTreeNode("newlines-before-import", "\n\n\n")
            appendField(padding)
            appendField(newImport)
          }
          else {
            // There are declarations
            ???
          }
        }
        else {
          val padding = SimpleTerminalTreeNode("newline-before-import", "\n")
          addFieldAfter(currentImports.last, padding)
          addFieldAfter(padding, newImport)
        }
        // Allow queries of object model to work
        currentImports = currentImports :+ newImport
      }
    }

    def addedImports: Seq[Import] = currentImports.filter(imp => !initialImports.contains(imp))

  }

  case class Import(
                     moduleNameField: MutableTerminalTreeNode,
                     exposing: Option[Exposing] = None,
                     alias: Option[String] = None
                   ) extends ParsedMutableContainerTreeNode("import") {

    var _exposing = exposing

    insertFieldCheckingPosition(moduleNameField)
    exposing.foreach(insertFieldCheckingPosition(_))


    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

    def moduleName = moduleNameField.value

    def exposes(identifier: String): Boolean =
      exposing match {
        case None => false
        case Some(_: AllExposing) => true
        case Some(names: FunctionNamesExposing) => names.contains(identifier)
      }

    def addExposure(identifier: String): Unit = {
      if (exposes(identifier)) { return }
      val parsedName = ElmParserCombinator.parseProduction(ElmParserCombinator.ElmIdentifiers.localVariableReference, identifier)
      exposing match {
        case None =>
          val newExposing = ElmParserCombinator.parseProduction(ElmParserCombinator.exposings, s"exposing ($identifier)")
          _exposing = Some(newExposing)
          val p1 = SimpleTerminalTreeNode("x", " exposing (")
          addFieldAfter(moduleNameField, p1)
          addFieldAfter(p1, newExposing)
          addFieldAfter(newExposing, SimpleTerminalTreeNode("x2", ")"))
        case Some(names: FunctionNamesExposing) =>
          names.addExposure(parsedName)
        case Some(_: AllExposing) => // fine
      }
    }

  }

}
