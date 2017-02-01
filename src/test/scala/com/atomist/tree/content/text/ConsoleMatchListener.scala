package com.atomist.tree.content.text

import com.atomist.tree.content.text.grammar.AbstractMatchListener

object ConsoleMatchListener extends AbstractMatchListener("console") {

  override protected def onMatchInternal(m: PositionedTreeNode): Unit = {
    //println(s"Matched [${m.nodeTags}(${m.startPosition}-${m.endPosition})]")
  }
}
