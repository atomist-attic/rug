package com.atomist.rug.kind.json

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core._
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.kind.json.JsonType._
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ExportFunction, _}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.tree.content.text._
import com.atomist.tree.pathexpression.PathExpressionEngine
import com.atomist.tree.utils.TreeNodeFinders._
import com.atomist.tree.{MutableTreeNode, TreeNode}

class JsonType(
                evaluator: Evaluator
              )
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "package.json configuration file"

  override val resolvesFromNodeTypes: Set[String] =
    Typed.typeClassesToTypeNames(classOf[ProjectType], classOf[FileType])

  override def viewManifest: Manifest[JsonMutableView] = manifest[JsonMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .allFiles
          .filter(f => f.name.endsWith(Extension))
          .map(f => new JsonMutableView(f, pmv))
        )
      case f: FileArtifactBackedMutableView =>
        Some(Seq(f)
          .filter(f => f.filename.endsWith(Extension))
          .map(j => new JsonMutableView(j.currentBackingObject, j.parent))
        )
      case dmv: DirectoryMutableView =>
        Some(dmv.currentBackingObject
          .allFiles
          .filter(f => f.name.endsWith(Extension))
          .map(f => new JsonMutableView(f, dmv.parent))
        )
      case _ => None
    }
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
                       parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  val originalParsed: MutableContainerTreeNode = new JsonParser().parse(originalBackingObject.content)

  private var currentParsed = originalParsed

  override def dirty = true

  override def value: String = currentParsed.value

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
        requiredSingleChild(value) match {
          case s: MutableTerminalTreeNode if "STRING".equals(s.nodeName) =>
            Seq(new JsonStringView(s, this))
          case mv: MutableContainerTreeNode if "object" equals mv.nodeName =>
            findPairsInValueNode(this, mv)
          case arr: MutableContainerTreeNode if "array".equals(arr.nodeName) =>
            Nil
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

  override def toString = s"${currentBackingObject.nodeName}:${currentBackingObject.nodeType}:[${currentBackingObject.value}]"
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

  addTypes(currentBackingObject.nodeType)

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
