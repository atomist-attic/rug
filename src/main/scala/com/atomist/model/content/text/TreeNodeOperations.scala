package com.atomist.model.content.text

import com.atomist.model.content.grammar.antlr.EmptyContainerTreeNode
import org.springframework.util.ReflectionUtils

import scala.collection.mutable.ListBuffer


/**
  * Defines operations on TreeNodes such as tree pruning.
  */
object TreeNodeOperations {

  def invokeMethod[T](n: TreeNode, methodName: String, args: Seq[String]): T = {
    //println(s"Testing expected [$value] against extracted [$extracted] on ${n.nodeName}:${n.nodeType}")
    val m = ReflectionUtils.getAllDeclaredMethods(n.getClass).find(m => m.getName.equals(methodName) && m.getParameterCount == args.size).getOrElse(
      throw new IllegalArgumentException(s"Cannot find method with name [$methodName] and ${args.size} parameters on ${n.getClass}")
    )
    //if (!m.getReturnType.equals(classOf[T]) ???
    m.invoke(n, args: _*).asInstanceOf[T]
  }

  def invokeMethodIfPresent[T](n: TreeNode, methodName: String, args: Seq[String] = Nil): Option[T] = {
    //println(s"Testing expected [$value] against extracted [$extracted] on ${n.nodeName}:${n.nodeType}")
    ReflectionUtils.getAllDeclaredMethods(n.getClass).find(m => m.getName.equals(methodName) && m.getParameterCount == args.size)
      .map(m => m.invoke(n, args: _*).asInstanceOf[T])
  }

  type NodeTransformer = TreeNode => Option[TreeNode]

  val Identity: NodeTransformer = f => Some(f)

  type TreeOperation = MutableContainerTreeNode => MutableContainerTreeNode

  def treeOperation(ft: NodeTransformer): TreeOperation = mtn => {
    // We put in a new temporary root to ensure that the root node itself gets
    val artificialRoot = new ParsedMutableContainerTreeNode("temporary-artificial-root")
    artificialRoot.appendField(mtn)
    ViewTree(artificialRoot, ft).childNodes.head.asInstanceOf[MutableContainerTreeNode]
  }

  /**
    * Remove collection nodes that merely contain a single collection child.
    * Preserve collection nodes that contain a single terminal child, because
    * that's typically necessary to provide a context for the name of the terminal.
    */
  val Flatten: TreeOperation = treeOperation {
    case ofv: ContainerTreeNode if ofv.childNodes.size == 1 =>
        val ret = ofv.childNodes.head match {
          case ctn: ContainerTreeNode => Some(ctn)
          case x => Some(x)
        }
      //println(s"ctn0:Flatten compressing to $ret")
      ret
    case ofv: ContainerTreeNode =>
      //println(s"ctn:Flatten keeping $ofv")
      Some(ofv)
    case x =>
      //println(s"x:Flatten keeping $x")
      Some(x)
  }

  /**
    * Get rid of this level, pulling up its children
    * @param name
    * @return
    */
  def collapse(name: String): TreeOperation = treeOperation {
    def filter: NodeTransformer = {
      case ofv: MutableContainerTreeNode =>
        //println(s"collapse looking at $ofv")
        val pulledUp = ListBuffer.empty[TreeNode]
        val kids = ofv.childNodes flatMap {
          case ctn: ContainerTreeNode if ctn.nodeName.equals(name) =>
            //println(s"Pulling up $ctn")
            pulledUp.appendAll(ctn.childNodes.flatMap(n => filter(n)))
            None
          case x => Some(x)
        }
        //println(s"ctn0:Flatten compressing to $ret")
        Some(new ViewTree(ofv, kids ++ pulledUp))
      case x =>
        //println(s"x:Flatten keeping $x")
        Some(x)
    }
    filter
  }

  /**
    * Remove empty container nodes
    */
  val Prune: TreeOperation = treeOperation {
    case ofv: ContainerTreeNode if ofv.childNodes.isEmpty =>
      //println(s"0nodes:Prune losing $ofv")
      None
    case empty if "".equals(empty.value) =>
      //println(s"mt:Prune losing $empty")
      None
    case ectn: EmptyContainerTreeNode =>
      //println(s"ectn:Prune losing $ectn")
      None
    case x =>
      //println(s"x:Prune keeping $x")
      Some(x)
  }

  val RemovePadding: TreeOperation = treeOperation {
    case pn: PaddingNode =>
      None
    case x =>
      Some(x)
  }

  def removeReservedWordTokens(reservedWords: Set[String]): TreeOperation = treeOperation {
    case tok: TerminalTreeNode if reservedWords.contains(tok.value) =>
      None
    case x =>
      Some(x)
  }

  def removeProductionsNamed(toRemove: Set[String]): TreeOperation = treeOperation {
    case tok: TerminalTreeNode if toRemove.contains(tok.nodeName) =>
      None
    case x =>
      Some(x)
  }

  val Clean: TreeOperation = RemovePadding andThen Prune andThen Flatten

}
