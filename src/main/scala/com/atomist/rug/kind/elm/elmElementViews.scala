package com.atomist.rug.kind.elm

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.elm.ElmModel.ElmDeclarationModels.{ElmTypeAlias, ElmUnionType}
import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels._
import com.atomist.rug.kind.elm.ElmModel.ElmFunctionHelpers.CanBeThoughtOfAsAFunction
import com.atomist.rug.kind.elm.ElmModel.ElmTypeModels.ElmRecordType
import com.atomist.rug.kind.elm.ElmModuleType._
import com.atomist.rug.spi._

class ElmFunctionMutableView(
                              val ef: CanBeThoughtOfAsAFunction,
                              parent: ElmModuleMutableView)
  extends ViewSupport[CanBeThoughtOfAsAFunction](ef, parent) {

  override def nodeType: String = "CanBeThoughtOfAsAFunction"

  override def childNodeTypes: Set[String] = childNodeNames

  override def nodeName: String = ef.functionName

  override val childrenNames: Seq[String] = Seq(CaseAlias, RecordValueAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case CaseAlias =>
      ef.body match {
        case c: ElmCase => Seq(new ElmCaseMutableView(c, this))
        case _ => Nil
      }
    // TODO these should be recursive
    case RecordValueAlias =>
      ef.body match {
        case le: ElmLetExpression =>
          le.body match {
            case rv: ElmRecord =>
              Seq(new ElmRecordValueMutableView(rv, this))
            case fa: ElmInfixFunctionApplication => Seq(fa.left, fa.right) collect {
              case rv: ElmRecord => new ElmRecordValueMutableView(rv, this)
            }
            case _ =>
              Nil
          }
        case rv: ElmRecord => Seq(new ElmRecordValueMutableView(rv, this))
        case t: ElmTuple => t.elements.collect {
          case rv: ElmRecord => new ElmRecordValueMutableView(rv, this)
        }
        case fa: ElmInfixFunctionApplication => Seq(fa.left, fa.right) collect {
          case rv: ElmRecord => new ElmRecordValueMutableView(rv, this)
        }
        case _ => Nil
      }
    case x => throw new RugRuntimeException(null, s"Script error: No child with name [$x] of Elm function type")
  }

  @ExportFunction(readOnly = true, description = "Name of the function")
  def name = ef.functionName

  @ExportFunction(readOnly = true, description = "The body of the function")
  def body = ef.body.value

  @ExportFunction(readOnly = true, description = "The body of the type specification")
  def typeSpecification = ef.elmType match {
    case Some(et) => et.value
    case None => ""
  }

  @ExportFunction(readOnly = false, description = "Change the name of the function")
  def rename(@ExportFunctionParameterDescription(name = "newName",
    description = "The function name to change to")
             newName: String): Unit = {
    logger.debug(s"Renamed function ${ef.functionName} to $newName")
    ef.setFunctionName(newName)
  }

  @ExportFunction(readOnly = false, description = "Change the type of this function")
  def changeType(@ExportFunctionParameterDescription(name = "newType",
    description = "The new type as a string")
                 newType: String): Unit = {
    ef.changeType(newType)
  }

  @ExportFunction(readOnly = false, description = "Replace the body of this function")
  def replaceBody(@ExportFunctionParameterDescription(name = "newBody",
    description = "The new function body")
                  newBody: String): Unit = {
    ef.replaceBody(newBody)
  }
}

class ElmTypeMutableView(
                          val eut: ElmUnionType,
                          parent: ElmModuleMutableView)
  extends TreeViewSupport[ElmUnionType](eut, parent) {

  override def childNodeTypes: Set[String] = childNodeNames

  override val childrenNames: Seq[String] = Seq(CaseAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = ???

  @ExportFunction(readOnly = true, description = "Name of the function")
  def name = eut.typeName

  @ExportFunction(readOnly = false, description = "Add")
  def addConstructor(@ExportFunctionParameterDescription(name = "constructor",
    description = "Constructor to add")
                     constructor: String): Unit = {
    eut.add(constructor)
  }

  @ExportFunction(readOnly = false, description = "Replace")
  def replaceBody(@ExportFunctionParameterDescription(name = "?",
    description = "")
                  contents: String): Unit = {
    eut.replace(contents)
  }
}

class ElmImportMutableView(
                          val imp: ElmModel.Import,
                          parent: ElmModuleMutableView)
  extends TreeViewSupport[ElmModel.Import](imp, parent) {

  override def nodeType: String = ElmModuleType.ImportAlias

  override def childNodeTypes: Set[String] = childNodeNames

  override val childrenNames: Seq[String] = Seq()

  override def children(fieldName: String): Seq[MutableView[_]] = ???

  @ExportFunction(readOnly = true, description = "Name of the imported module")
  def module = imp.moduleName

  @ExportFunction(readOnly = false, description = "Add")
  def addExposure(@ExportFunctionParameterDescription(name = "name",
    description = "variable to expose")
                     identifier: String): Unit = {
    imp.addExposure(identifier)
  }
}

class ElmTypeAliasMutableView(
                               val ta: ElmTypeAlias,
                               parent: ElmModuleMutableView)
  extends TreeViewSupport[ElmTypeAlias](ta, parent) {

  override def childNodeTypes: Set[String] = childNodeNames

  override val childrenNames: Seq[String] = Seq(RecordTypeAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case RecordTypeAlias => ta.alias match {
      case rt: ElmRecordType => Seq(new ElmRecordTypeMutableView(rt, this))
      case _ => Nil
    }
    case x => throw new RugRuntimeException(null, s"Script error: No child with name [$x] of Elm type alias")
  }

  @ExportFunction(readOnly = true, description = "Name of the function")
  def name: String = ta.typeAliasName

  @ExportFunction(readOnly = false, description = "Change the name of the type alias")
  def rename(@ExportFunctionParameterDescription(name = "newName",
    description = "The alias name to change to")
             newName: String): Unit = {
    logger.debug(s"Renamed function ${ta.typeAliasName} to $newName")
    ta.setTypeAliasName(newName)
  }

  @ExportFunction(readOnly = false, description = "Replace body of the type alias")
  def replaceBody(@ExportFunctionParameterDescription(name = "newBody", description = "New type alias body")
                  newBody: String
                 ): Unit = {
    ta.replaceBody(newBody)
  }

}

class ElmRecordTypeMutableView(
                                val rec: ElmRecordType,
                                parent: ElmTypeAliasMutableView)
  extends TreeViewSupport[ElmRecordType](rec, parent) {

  override def childNodeTypes: Set[String] = childNodeNames

  override def childrenNames: Seq[String] = rec.fields.map { _.recordFieldTypeName }

  override def children(fieldName: String): Seq[MutableView[_]] = ???

  @ExportFunction(readOnly = false, description = "Add a field to the record type")
  def add(@ExportFunctionParameterDescription(name = "name", description = "Record identifier")
          name: String,
          @ExportFunctionParameterDescription(name = "type", description = "Record type")
          typ: String
         ): Unit = {
    rec.add(name, typ)
  }

}

class ElmRecordValueMutableView(
                                 val rec: ElmRecord,
                                 parent: ElmFunctionMutableView)
  extends TreeViewSupport[ElmRecord](rec, parent) {

  override def childNodeTypes: Set[String] = childNodeNames

  override def childrenNames: Seq[String] = rec.fields.map { e => e.elmRecordFieldName }

  override def children(fieldName: String): Seq[MutableView[_]] = ???

  @ExportFunction(readOnly = false, description = "Add a field to the record")
  def add(@ExportFunctionParameterDescription(name = "name", description = "Name of the field")
          name: String,
          @ExportFunctionParameterDescription(name = "value", description = "Expression that populates the field")
          value: String
         ): Unit = {
    rec.add(name, value)
  }
}
