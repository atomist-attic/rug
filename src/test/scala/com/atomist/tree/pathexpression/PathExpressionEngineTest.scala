package com.atomist.tree.pathexpression

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.tree.{ContainerTreeNodeImpl, SimpleTerminalTreeNode}
import com.atomist.tree.content.text.ParsedMutableContainerTreeNode
import org.scalatest.{FlatSpec, Matchers}

class PathExpressionEngineTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  it should "return root node with / expression" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    tn.appendField(fooNode)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(tn))
  }

  it should "find property in container tree node" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    tn.appendField(fooNode)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))

    val expr = "/foo"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode))
  }

  it should "find property in container tree node 2 levels deep" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    prop1.appendField(fooNode)
    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/nested/foo"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode))
  }

  it should "follow //" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val prop11 = new ParsedMutableContainerTreeNode("level2")
    val fooNode1 = SimpleTerminalTreeNode("foo", "foo1")
    val fooNode2 = SimpleTerminalTreeNode("foo", "foo2")

    prop1.appendField(prop11)
    prop11.appendField(fooNode1)
    prop11.appendField(fooNode2)

    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))

    val expr = "/nested//*[@name='foo']"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode1, fooNode2))
  }

  it should "match on node name" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val prop11 = new ParsedMutableContainerTreeNode("level2")
    val fooNode1 = SimpleTerminalTreeNode("foo", "foo1")
    val fooNode2 = SimpleTerminalTreeNode("foo", "foo2")
    prop1.appendField(prop11)
    prop11.appendField(fooNode1)
    prop11.appendField(fooNode2)
    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/nested/level2/*[@name='foo']"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode1, fooNode2))
  }

  it should "match on node type" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val prop11 = new ParsedMutableContainerTreeNode("level2")
    val fooNode1 = SimpleTerminalTreeNode("foo", "foo1")
    val fooNode2 = SimpleTerminalTreeNode("foo", "foo2")
    prop1.appendField(prop11)
    prop11.appendField(fooNode1)
    prop11.appendField(fooNode2)
    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/nested/level2/*[@name='foo']"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode1, fooNode2))
  }

  it should "match on node index" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val prop11 = new ParsedMutableContainerTreeNode("level2")
    val fooNode1 = SimpleTerminalTreeNode("foo", "foo1")
    val fooNode2 = SimpleTerminalTreeNode("foo", "foo2")

    prop1.appendField(prop11)
    prop11.appendField(fooNode1)
    prop11.appendField(fooNode2)

    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))

    val expr = "/nested/level2/*[1]"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(fooNode1))

    val expr2 = "/nested/level2/*[2]"
    val rtn2 = ee.evaluate(tn, expr2, DefaultTypeRegistry)
    rtn2.right.get should equal (Seq(fooNode2))
  }

  it should "preparing nodes in path" in {
    class TouchableTreeNode extends ParsedMutableContainerTreeNode("name") {
      def foo: String = null
      var touched: Boolean = false
    }

    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new TouchableTreeNode
    rn.appendField(tn)

    val expr = "/*[.foo()=null]"
    val rtn = ee.evaluate(rn, expr, DefaultTypeRegistry, Some{
      case ttn: TouchableTreeNode =>
        ttn.touched = true
        ttn
      case x => x
    })
    val s = rtn.right.get
    s should equal (Seq(tn))
    tn.touched should be (true)
  }

  it should "compare value to null" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def foo: String = null
    }
    rn.appendField(tn)

    val expr = "/*[.foo()=null]"
    val rtn = ee.evaluate(rn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(tn))

    val expr2 = "/*[not(.foo()=null)]"
    val rtn2 = ee.evaluate(rn, expr2, DefaultTypeRegistry)
    rtn2.right.get should equal (Nil)
  }

  it should "compare method value to int with method" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def age = 25
    }
    rn.appendField(tn)

    val expr = "/*[.age()=25]"
    val rtn = ee.evaluate(rn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(tn))

    val expr2 = "/*[.age()=26]"
    val rtn2 = ee.evaluate(rn, expr2, DefaultTypeRegistry)
    rtn2.right.get should equal (Nil)
  }

  it should "compare method value to int with property" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name")
    tn.appendField(SimpleTerminalTreeNode("age", "25"))
    rn.appendField(tn)

    val expr = "/*[@age='25']"
    val rtn = ee.evaluate(rn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(tn))

    val expr2 = "/*[@age='26']"
    val rtn2 = ee.evaluate(rn, expr2, DefaultTypeRegistry)
    rtn2.right.get should equal (Nil)
  }

  it should "descend to scalar property" in {
    val kid = SimpleTerminalTreeNode("name", "thing")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def thing = kid
    }

    val expr = "/thing"
    val rtn = ee.evaluate(tn, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(kid))
  }

  it should "handle a property name axis specifier and Object type" in {
    val tn = new ContainerTreeNodeImpl("Issue", "Issue")
    tn.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    tn.addField(repo)
    // TODO should we be able to handle a property called "name"?
    val expr = """/Issue()[@state='open']/belongsTo::Repo()[@name2='rug-cli']"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(tn)
    val rtn = ee.evaluate(parent, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(repo))
  }

  it should "handle a property name axis specifier" in {
    val tn = new ContainerTreeNodeImpl("Issue", "Issue")
    tn.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    tn.addField(repo)
    val expr = """/Issue()[@state='open']/belongsTo::*[@name2='rug-cli']"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(tn)
    val rtn = ee.evaluate(parent, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(repo))
  }

  it should "handle a property name axis specifier nested predicate with Object type" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][/belongsTo::Repo()[@name2='rug-cli']]"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Seq(issue))
  }

  it should "not match due to unsatisfied nested predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][/nonsense::Repo()[@name2='rug-cli']]"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr, DefaultTypeRegistry)
    rtn.right.get should equal (Nil)
  }

}
