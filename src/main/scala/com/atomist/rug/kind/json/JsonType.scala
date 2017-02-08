package com.atomist.rug.kind.json

import com.atomist.rug.kind.core._
import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.rug.spi.{ExportFunction, _}
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.pathexpression.PathExpressionEngine
import com.atomist.tree.utils.TreeNodeFinders._
import com.atomist.tree.{MutableTreeNode, TreeNode}

/**
  * Type for JSON files
  */
class JsonType
  extends TypeUnderFile {

  import JsonType._

  private def jsonParser = new JsonParser

  override def description = "JSON file"

  override def viewManifest: Manifest[JsonMutableView] = manifest[JsonMutableView]

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(Extension)

  override def fileToRawNode(f: FileArtifact, ml: Option[MatchListener]): Option[MutableContainerTreeNode] = {
    jsonParser.parse(f.content, ml)
  }

  override protected def createView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView): MutableView[_] = {
    new JsonMutableView(f.currentBackingObject, f.parent, n)
  }
}

object JsonType {

  val Extension = ".json"

  val pe = new PathExpressionEngine

  /*
  value
   : STRING
   | NUMBER
   | object
   | array
   | 'true'
   | 'false'
   | 'null'
   ;
   */
  private[json] def findPairsInValueNode(parent: MutableView[_], n: TreeNode): Seq[PairMutableView] = n match {
    case mtn: MutableContainerTreeNode /*if "value".equals(mtn.nodeName)*/ =>
      mtn.childNodes flatMap {
        case p: MutableContainerTreeNode if "pair".equals(p.nodeName) =>
          Seq(new PairMutableView(p, parent))
        case o: MutableContainerTreeNode if "object".equals(o.nodeName) =>
          val nodes: Seq[PairMutableView] = o.childrenNamed("pair").map(p => new PairMutableView(p.asInstanceOf[MutableContainerTreeNode], parent))
          nodes
      }
    case x => Nil
  }

  // Get rid of ""
  private[json] def deJsonize(s: String) = s.substring(1).dropRight(1)

  private[json] def jsonize(s: String) = "\"" + s + "\""
}

import com.atomist.rug.kind.json.JsonType._

class JsonMutableView(
                       originalBackingObject: FileArtifact,
                       parent: ProjectMutableView,
                       originalParsed: MutableContainerTreeNode
                     )
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  private val currentParsed: MutableContainerTreeNode = originalParsed

  override def dirty = true

  override def value: String = currentParsed.value

  //println(TreeNodeUtils.toShorterString(currentParsed))

  // There's just one value
  /*
 json
  : value
  ;
  */
  private val soleKid: MutableContainerTreeNode = currentParsed.childrenNamed("value").head match {
    case mv: MutableContainerTreeNode => mv
  }

  override protected def currentContent: String = currentParsed.value

  override def childNodeNames: Set[String] =
    findPairsInValueNode(this, soleKid).map(_.nodeName).toSet

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = {
    findPairsInValueNode(this, soleKid)
      .filter(n => fieldName.equals(n.nodeName))
  }
}

class PairTypeProvider extends TypeProvider(classOf[PairMutableView]) {

  override def description: String = "JSON pair"
}

/*
  pair
   : STRING ':' value
   ;
   */
private class PairMutableView(
                               originalBackingObject: MutableContainerTreeNode,
                               parent: MutableView[_])
  extends ViewSupport[MutableContainerTreeNode](originalBackingObject, parent) {

  // The backing node has a different name, as the Antlr
  // grammar doesn't adhere to our capitalization rules
  require(currentBackingObject.nodeName.equals("pair"))

  private val kids: Seq[MutableView[_]] =
    singleChild(currentBackingObject, "value") match {
      case Some(value) =>
        value.childNodes match {
          case Seq(s: MutableTerminalTreeNode) if "STRING".equals(s.nodeName) =>
            Seq(new JsonStringView(s, this))
          case Seq(mv: MutableContainerTreeNode) if "object" equals mv.nodeName =>
            findPairsInValueNode(this, mv)
          case Seq(arr: MutableContainerTreeNode) if "array".equals(arr.nodeName) =>
            Nil
          case Nil => Nil
        }
      case None => requiredSingleChild(currentBackingObject, "STRING") match {
        case s: MutableTerminalTreeNode if "STRING".equals(s.nodeName) =>
          Seq(new JsonStringView(s, this))
      }
    }

  override def nodeName: String = nodeNameFromValue(currentBackingObject)

  private def nodeNameFromValue(tn: TreeNode): String = deJsonize(requiredSingleFieldValue(tn, "STRING"))

  override val childNodeTypes: Set[String] = Set("value")

  override def value: String = currentBackingObject.value

  override def childNodeNames: Set[String] = kids.map(k => k.nodeName).toSet

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] =
    kids.filter(k => k.nodeName.equals(fieldName))

  @ExportFunction(readOnly = false, description = "setValue")
  def setValue(newValue: String): Unit = {
    requiredSingleChild(currentBackingObject, "value") match {
      case mtn: MutableTreeNode => mtn.update(jsonize(newValue))
    }
  }

  @ExportFunction(readOnly = false, description = "Add a key value")
  def addKeyValue(k: String, v: String): Unit = {
    requiredSingleChild(currentBackingObject, "value") match {
      case vm: MutableContainerTreeNode =>
        val index = vm.value.lastIndexOf("\"")
        val split = vm.value.splitAt(index)
        val newContent = s"${jsonize(k)}: ${jsonize(v)}"
        // TODO how many tabs do we have? could get from previous line
        vm.update(split._1 + "\",\n\t" + newContent + split._2.substring(1))
    }
  }

  override def toString = s"${currentBackingObject.nodeName}:${currentBackingObject.nodeTags}:[${currentBackingObject.value}]"
}

/**
  * Handle " surrounding JSON strings.
  */
private class JsonStringView(
                              originalBackingObject: MutableTerminalTreeNode,
                              parent: MutableView[_])
  extends ViewSupport[MutableTerminalTreeNode](originalBackingObject, parent)
    with MutableTreeNode {

  override def dirty: Boolean = originalBackingObject.dirty

  addTypes(currentBackingObject.nodeTags)

  override def nodeName: String = originalBackingObject.nodeName

  @ExportFunction(readOnly = true, description = "Value of the content")
  override def value: String = deJsonize(originalBackingObject.value)

  override def update(to: String): Unit = setValue(to)

  @ExportFunction(readOnly = true, description = "Return the value of the sole key")
  def valueOf: String = originalBackingObject.value

  @ExportFunction(readOnly = false, description = "Update the value of the sole key")
  def setValue(@ExportFunctionParameterDescription(name = "name",
    description = "The new value")
               newValue: String): Unit =
    originalBackingObject.update("\"" + newValue + "\"")

  override val childNodeNames: Set[String] = Set(originalBackingObject.nodeName)

  override def childNodeTypes: Set[String] = Set()

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = Nil

  override def toString = s"${getClass.getSimpleName} wrapping $currentBackingObject"
}
