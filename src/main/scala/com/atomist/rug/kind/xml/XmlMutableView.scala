package com.atomist.rug.kind.xml

import java.io.{StringReader, StringWriter}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.xpath.{XPathConstants, XPathFactory}

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, TerminalView}
import com.atomist.source.FileArtifact
import org.w3c.dom.{Document, Node, NodeList}
import org.xml.sax.InputSource

object XmlMutableView {

  def stringToDocument(xmlStr: String): Document = {
    val factory = DocumentBuilderFactory.newInstance()

    val builder = factory.newDocumentBuilder()
    builder.parse(new InputSource(new StringReader(xmlStr)))
  }

  def documentToString(doc: Document): String = {
    val tf = TransformerFactory.newInstance()

    tf.setAttribute("indent-number", 4)

    val transformer = tf.newTransformer()

    transformer.setOutputProperty(OutputKeys.INDENT, "yes")

    val writer = new StringWriter()
    transformer.transform(new DOMSource(doc), new StreamResult(writer))
    writer.getBuffer.toString
  }

  def addContentAsChild(xmlDocument: Document, nodesList: NodeList, nodeToAdd: Node): Node = {
    val lineBreak = xmlDocument.createTextNode("\t")

    nodesList.item(0).appendChild(lineBreak)
    nodesList.item(0).appendChild(nodeToAdd)
  }

  def prepareContentForInsertion(nodeContent: String, xmlDocument: Document): Node = {
    val factory = DocumentBuilderFactory.newInstance()

    val builder = factory.newDocumentBuilder()
    val subDocument = builder.parse(new InputSource(new StringReader(nodeContent)))

    val actualNode = subDocument.getDocumentElement

    val nodeToAdd = xmlDocument.importNode(actualNode, true)
    nodeToAdd
  }

  def executeXPathExpression(xpath: String, xmlDocument: Document): NodeList = {
    val xpathExpression = XPathFactory.newInstance().newXPath()

    val nodesList = xpathExpression.compile(xpath)
      .evaluate(xmlDocument, XPathConstants.NODESET).asInstanceOf[NodeList]
    nodesList
  }

  def parentNode(xpath: String, xmlDocument: Document): Option[Node] = {
    val xpathExpression = XPathFactory.newInstance().newXPath()

    val nodesList = xpathExpression.compile(xpath)
      .evaluate(xmlDocument, XPathConstants.NODESET).asInstanceOf[NodeList]

    if (nodesList.getLength > 0) {
      Some(nodesList.item(0).getParentNode)
    } else {
      None
    }
  }
}

class XmlMutableView(
                      originalBackingObject: FileArtifact,
                      parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent)
    with TerminalView[FileArtifact] {

  private var xml = originalBackingObject.content

  override protected def currentContent: String = xml

  import XmlMutableView._

  @ExportFunction(readOnly = false, description = "Add the specified content under the indicated xpath-selected node")
  private def addChildNode(@ExportFunctionParameterDescription(name = "xpath",
    description = "The XPath selector for the node to add the content under")
                           xpath: String,
                           @ExportFunctionParameterDescription(name = "newNode",
                             description = "The new node name to be added as a child")
                           newNode: String,
                           @ExportFunctionParameterDescription(name = "nodeContent",
                             description = "XML document to be added under the indicated node")
                           nodeContent: String): Unit = {
    val xmlDocument = stringToDocument(currentContent)

    val nodesList: NodeList = executeXPathExpression(xpath, xmlDocument)

    val nodeToAdd: Node = prepareContentForInsertion(nodeContent, xmlDocument)

    addContentAsChild(xmlDocument, nodesList, nodeToAdd)

    this.xml = documentToString(xmlDocument)
  }

  @ExportFunction(readOnly = false, description = "Adds or replaces a node")
  def addOrReplaceNode(@ExportFunctionParameterDescription(name = "parentNodeXPath",
    description = "The XPath selector for the parent node")
                       xpathOfParent: String,
                       @ExportFunctionParameterDescription(name = "xPathOfNodeToReplace",
                         description = "The XPath selector for the node to replace")
                       xPathOfNodeToReplace: String,
                       @ExportFunctionParameterDescription(name = "newNode",
                         description = "The name of the node being placed")
                       newNode: String,
                       @ExportFunctionParameterDescription(name = "nodeContent",
                         description = "The content of the node being placed")
                       nodeContent: String): Unit = {

    val xmlDocument = stringToDocument(currentContent)

    val nodeToAdd: Node = prepareContentForInsertion(nodeContent, xmlDocument)

    this.contains(xPathOfNodeToReplace) match {
      case true =>
        val xpathExpression = XPathFactory.newInstance().newXPath()
        val nodeToReplace: Node = xpathExpression.compile(xPathOfNodeToReplace)
          .evaluate(xmlDocument, XPathConstants.NODE).asInstanceOf[Node]
        nodeToReplace.getParentNode.replaceChild(nodeToAdd, nodeToReplace)
        this.xml = documentToString(xmlDocument)
      case false => addChildNode(xpathOfParent, newNode, nodeContent)
    }
  }

  @ExportFunction(readOnly = true, description = "Tests whether a node matching the given xpath expression is present")
  def contains(@ExportFunctionParameterDescription(name = "xpath",
    description = "The XPath to test against for the presence of a node")
               xpath: String): Boolean = {
    val xmlDocument = stringToDocument(currentContent)

    executeXPathExpression(xpath, xmlDocument).getLength match {
      case 0 => false
      case _ => true
    }
  }

  @ExportFunction(readOnly = true, description = "Get the text content for a specific xpath expression")
  def getTextContentFor(@ExportFunctionParameterDescription(name = "xpath",
    description = "The XPath to use to retrieve the test content")
                        xpath: String): String = {
    val xmlDocument = stringToDocument(currentContent)
    val nodesList: NodeList = executeXPathExpression(xpath, xmlDocument)

    val numberOfFoundNodes = nodesList.getLength
    if (numberOfFoundNodes > 0) {
      nodesList.item(0).getTextContent
    } else {
      ""
    }
  }

  @ExportFunction(readOnly = false, description = "Set the text content for a specific xpath expression")
  def setTextContentFor(@ExportFunctionParameterDescription(name = "xpath",
    description = "The XPath to use to set the test content")
                        xpath: String,
                        @ExportFunctionParameterDescription(name = "newContent",
                          description = "New text content for the XPath")
                        newContent: String): Unit = {
    val xmlDocument = stringToDocument(currentContent)
    val nodesList: NodeList = executeXPathExpression(xpath, xmlDocument)

    val numberOfFoundNodes = nodesList.getLength
    if (numberOfFoundNodes > 0) {
      nodesList.item(0).setTextContent(newContent)
      this.xml = documentToString(xmlDocument)
    }
  }

  @ExportFunction(readOnly = false, description = "Deletes the specified node")
  def deleteNode(@ExportFunctionParameterDescription(name = "xpath",
    description = "The XPath to the node to delete")
                 xpath: String): Unit = {
    val xmlDocument = stringToDocument(currentContent)
    val nodesList: NodeList = executeXPathExpression(xpath, xmlDocument)

    val numberOfFoundNodes = nodesList.getLength

    if (numberOfFoundNodes > 0) {
      val nodeToRemove: Node = nodesList.item(0)
      nodeToRemove.getParentNode.removeChild(nodeToRemove)
      this.xml = documentToString(xmlDocument)
    }
  }
}
