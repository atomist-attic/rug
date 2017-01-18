package com.atomist.rug.kind.elm.ElmModel

import com.atomist.tree.content.text.{MutableTerminalTreeNode, ParsedMutableContainerTreeNode}
import com.atomist.rug.kind.elm.{ElmModuleType, ElmParserCombinator}
import com.atomist.tree.{SimpleTerminalTreeNode, TreeNode}

object ElmTypeModels {

  sealed trait ElmTypeSpecification extends ParsedMutableContainerTreeNode

  class ElmTypeParameterVariable(id: MutableTerminalTreeNode)
    extends ParsedMutableContainerTreeNode("type-variable")
    with ElmTypeSpecification {
    // I tried to do this as a trait. It didn't work. Something
    // about UpdatableScalarFieldValue is not a superclass of ParsedMutableStringUpdatingObjectValue

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

  }

  case class ElmTypeWithParameters(
                                    typeNameField: MutableTerminalTreeNode,
                                    parameters: Seq[ElmTypeSpecification] = Nil)
    extends ParsedMutableContainerTreeNode("type-with-parameters")
      with ElmTypeSpecification {

    insertFieldCheckingPosition(typeNameField)

    def typeName = typeNameField.value

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

  case class ElmTupleType(
                           elements: Seq[ElmTypeSpecification]
                         ) extends ParsedMutableContainerTreeNode("tuple-type")
    with ElmTypeSpecification {

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

  case class ElmRecordType(
                            initialFields: Seq[ElmRecordFieldType]
                          )
    extends ParsedMutableContainerTreeNode("record-type")
      with ElmTypeSpecification {

    private var _fields: Seq[ElmRecordFieldType] = initialFields

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

    appendFields(initialFields)

    addType(ElmModuleType.RecordTypeAlias)

    def fields: Seq[ElmRecordFieldType] = _fields

    def add(name: String, typ: String): Unit = {
      val newRecordAsString = s"$name : $typ"
      val newRecord =
        ElmParserCombinator.parseProduction(
          ElmParserCombinator.TypeSpecifications.recordFieldType,
          newRecordAsString)
      newRecord.pad(newRecordAsString)
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

  case class ElmRecordFieldType(
                                 recordFieldTypeNameField: MutableTerminalTreeNode,
                                 typeSpec: ElmTypeSpecification
                               )
    extends ParsedMutableContainerTreeNode("record-field-type") {

    insertFieldCheckingPosition(recordFieldTypeNameField)
    insertFieldCheckingPosition(typeSpec)

    override def childNodeNames: Set[String] = Set()

    override def childrenNamed(key: String): Seq[TreeNode] = Nil

    def recordFieldTypeName = recordFieldTypeNameField.value
  }

  case class ElmFunctionType(bits: Seq[ElmTypeSpecification])
    extends ParsedMutableContainerTreeNode("function-type")
      with ElmTypeSpecification {

    appendFields(bits)

    override def childrenNamed(key: String): Seq[TreeNode] = fieldValues.filter(n => n.nodeName.equals(key))

  }

}