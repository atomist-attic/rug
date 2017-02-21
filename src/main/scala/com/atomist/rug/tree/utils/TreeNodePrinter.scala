package com.atomist.rug.tree.utils

object TreeNodePrinter {

  case class BabyTree(print: String, children: Seq[BabyTree])

  def drawTree(baby: BabyTree) = draw[BabyTree](_.children, _.print)(baby)

  /**
   * TreeNodePrinter utility.
   * It draws a cute tree like
   *

Grandma
├─┬ Dad
| ├── Sister
| └─┬ Me
|   ├── Daughter
|   └── Son
└── Aunt

   *
   * To use it, pass two functions:
   * @param children how to access child nodes
   * @param info how to print a node
   *
   * @param topOfTree and then the top-level node!
   *
   * @return String. Print it yourself
   */
  def draw[T](children: T => Seq[T], info: T => String)(topOfTree: T): String = {

    def drawInternal(tn: T): Seq[String] = {
      tn match {
        case leaf if children(leaf).isEmpty => Seq(info(leaf))
        case parent => info(parent) +: printChildren(children(parent))
      }
    }

    def printChildren(ch: Seq[T]): Seq[String] = {
      val last = ch.last
      val notLast = ch.slice(0, ch.length - 1)

      val lastLine = prefixChildLines(children(last).nonEmpty, last = true, drawInternal(last))
      val earlierLines = notLast.flatMap(tn => prefixChildLines(children(tn).nonEmpty, last = false, drawInternal(tn)))

      earlierLines ++ lastLine
    }

    drawInternal(topOfTree).mkString("\n")
  }

  private def prefixChildLines(hasChildren: Boolean, last: Boolean, childLines: Seq[String]): Seq[String] =
    childLines.toList match {
      case head :: rest =>
        val connector = if (last) FILLER else TREE_CONNECTOR
        val firstLine = firstLineOfChildPrefix(hasChildren, last) + head
        val restLines = rest.map(connector + _)
        firstLine +: restLines
      case _ => throw new RuntimeException("empty list")
    }

  private def firstLineOfChildPrefix(hasChildren: Boolean, last: Boolean): String =
    if (hasChildren && last) LAST_TREE_NODE_WITH_CHILDREN
    else if (hasChildren && !last) TREE_NODE_WITH_CHILDREN
    else if (!hasChildren && last) LAST_TREE_NODE
    else if (!hasChildren && !last) TREE_NODE
    else "the impossible has happened"

  // this won't be pretty on Windows
  val LAST_TREE_NODE: String = "└── "
  val LAST_TREE_NODE_WITH_CHILDREN = "└─┬ "
  val TREE_NODE: String = "├── "
  val TREE_NODE_WITH_CHILDREN: String = "├─┬ "
  val TREE_CONNECTOR: String = "| "
  val FILLER: String = "  "
}
