package com.atomist.rug.spi

import java.lang.reflect.InvocationTargetException

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.OutOfDateNodeException

/**
  * Parameter to a Rug type.
  */
case class TypeParameter(
                          name: String,
                          parameterType: String,
                          description: Option[String]
                        ) {

  def getDescription: String = description.getOrElse("")

  override def toString: String =
    s"$name : $parameterType : ${description.getOrElse("No Description")}"
}

/**
  * Operation on an exported Rug type. Typically annotated with an
  * [[ExportFunction]] annotation.
  *
  * @param name        name of the type
  * @param description description of the type. May be used in generated code
  * @param example     optional example of usage of the operation
  * @param definedOn  type we are defined on. May be an abstract superclass.
  * @see ExportFunction
  */
case class TypeOperation(
                          name: String,
                          description: String,
                          readOnly: Boolean,
                          parameters: Seq[TypeParameter],
                          returnType: String,
                          definedOn: Class[_],
                          example: Option[String]) {

  import TypeOperation._

  def hasExample: Boolean = example.isDefined

  /**
    * Does this type support reflective invocation on a node?
    */
  def invocable: Boolean = definedOn != null

  /**
    * Convenient way of invoking the method using arguments
    * passed directly from Javascript.
    */
  def invoke(target: Object, rawArgs: Seq[AnyRef]): Object = {
    val args = rawArgs.map(a => NashornUtils.toJavaType(a))
    // Include TreeNode methods, although the annotations won't be inherited
    val methods = target.getClass.getMethods.toSeq.filter(m =>
      this.name.equals(m.getName) &&
        (m.getDeclaredAnnotations.exists(_.isInstanceOf[ExportFunction]) || TreeNodeOperations.contains(m.getName)) &&
        this.parameters.size == m.getParameterCount
    )
    if (methods.size != 1)
      throw new IllegalArgumentException(
        s"Operation [$name] cannot be invoked on [${target.getClass.getName}]: Found ${methods.size} definitions with ${parameters.size}, required exactly 1: " +
          s"Known methods=[${methods.mkString(",")}]"
      )

    try {
      methods.head.invoke(target, args: _*)
    } catch {
      case e: InvocationTargetException if e.getCause.isInstanceOf[InstantEditorFailureException] => throw e.getCause // we meant to do this
      case e: InvocationTargetException if e.getCause.isInstanceOf[OutOfDateNodeException] => throw e.getCause // we meant to do this
      case t: Throwable =>
        val argDiagnostics = args map {
          case null => "null"
          case o => s"$o: ${o.getClass}"
        }
        throw new RugRuntimeException(null, s"Exception invoking ${methods.head} with args=${argDiagnostics.mkString(",")}: ${t.getMessage}", t)
    }
  }
}

object TypeOperation {

  val TreeNodeAllTypeOperations: Seq[TypeOperation] =
    new ReflectiveTypeOperationFinder(classOf[TreeNode]).allOperations

  val TreeNodeTypeOperations: Seq[TypeOperation] =
    new ReflectiveTypeOperationFinder(classOf[TreeNode]).operations

  val TreeNodeType = new Typed {
    override val name = "TreeNode"
    override def description: String = "TreeNode operations"
    override def allOperations = TreeNodeAllTypeOperations
    override def operations = TreeNodeTypeOperations
  }

  val TreeNodeOperations: Set[String] =
    TreeNodeAllTypeOperations.map(_.name).toSet
}