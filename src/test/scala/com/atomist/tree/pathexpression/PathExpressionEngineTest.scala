package com.atomist.tree.pathexpression

import com.atomist.tree.content.text.OffsetInputPosition._
import com.atomist.tree.content.text.{LineHoldingOffsetInputPosition, MutableTerminalTreeNode, ParsedMutableContainerTreeNode, SimpleMutableContainerTreeNode}
import com.atomist.tree.{ContainerTreeNodeImpl, SimpleTerminalTreeNode, TreeNode}
import org.scalatest.{FlatSpec, Matchers}

class PathExpressionEngineTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  "PathExpressionEngine" should "return root node with / expression" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    tn.appendField(fooNode)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/"
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(tn))
  }

  it should "find property in container tree node" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    tn.appendField(fooNode)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))

    val expr = "/foo"
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode))
  }

  it should "find property in container tree node 2 levels deep" in {
    val tn = new ParsedMutableContainerTreeNode("name")
    val prop1 = new ParsedMutableContainerTreeNode("nested")
    val fooNode = SimpleTerminalTreeNode("foo", "foo")
    prop1.appendField(fooNode)
    tn.appendField(prop1)
    tn.appendField(SimpleTerminalTreeNode("bar", "bar"))
    val expr = "/nested/foo"
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode))
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
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode1, fooNode2))
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
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode1, fooNode2))
  }

  it should "match on node name with /@name" in
    matchOnNodeName("/nested/level2/*[@name='foo']")

  it should "match on node name with //@name" in
    matchOnNodeName("/nested/level2//*[@name='foo']")

  it should "match on node name with //foo" in
    matchOnNodeName("/nested/level2//foo")

  it should "match on node name with /foo and or predicate" in
    matchOnNodeName("/nested/level2/*[@value='foo1' or @value='foo2']")

  it should "match on node name with //foo and or predicate" in
    matchOnNodeName("/nested/level2//*[@value='foo1' or @value='foo2']")

  private def matchOnNodeName(expr: String) {
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
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode1, fooNode2))
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
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(fooNode1))

    val expr2 = "/nested/level2/*[2]"
    val rtn2 = ee.evaluate(tn, expr2)
    assert(rtn2.right.get === Seq(fooNode2))
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
    val rtn = ee.evaluate(rn, expr, nodePreparer = Some {
      case ttn: TouchableTreeNode =>
        ttn.touched = true
        ttn
      case x => x
    })
    val s = rtn.right.get
    s should equal(Seq(tn))
    assert(tn.touched === true)
  }

  it should "compare value to null" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def foo: String = null
    }
    rn.appendField(tn)

    val expr = "/*[.foo()=null]"
    val rtn = ee.evaluate(rn, expr)
    assert(rtn.right.get === Seq(tn))

    val expr2 = "/*[not(.foo()=null)]"
    val rtn2 = ee.evaluate(rn, expr2)
    assert(rtn2.right.get === Nil)
  }

  it should "compare method value to int with method" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def age = 25
    }
    rn.appendField(tn)

    val expr = "/*[.age()=25]"
    val rtn = ee.evaluate(rn, expr)
    assert(rtn.right.get === Seq(tn))

    val expr2 = "/*[.age()=26]"
    val rtn2 = ee.evaluate(rn, expr2)
    assert(rtn2.right.get === Nil)
  }

  it should "compare method value to int with property" in {
    val rn = new ParsedMutableContainerTreeNode("root")
    val tn = new ParsedMutableContainerTreeNode("name")
    tn.appendField(SimpleTerminalTreeNode("age", "25"))
    rn.appendField(tn)

    val expr = "/*[@age='25']"
    val rtn = ee.evaluate(rn, expr)
    assert(rtn.right.get === Seq(tn))

    val expr2 = "/*[@age='26']"
    val rtn2 = ee.evaluate(rn, expr2)
    assert(rtn2.right.get === Nil)
  }

  it should "descend to scalar property" in {
    val kid = SimpleTerminalTreeNode("name", "thing")
    val tn = new ParsedMutableContainerTreeNode("name") {
      def thing = kid
    }

    val expr = "/thing"
    val rtn = ee.evaluate(tn, expr)
    assert(rtn.right.get === Seq(kid))
  }

  it should "reject unknown path expression function" in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedContent = "this is incorrect"
    val line = inputA + unmatchedContent + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)

    val expr = "/*[utter-balderdash(., 'fo')]"
    an[IllegalArgumentException] should be thrownBy
      ee.evaluate(soo, expr)
  }

  it should "use XPath style contains function against ." in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedContent = "this is incorrect"
    val line = inputA + unmatchedContent + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)

    val expr = "/*[contains(.,'foo')]"
    val rtn = ee.evaluate(soo, expr)
    assert(rtn.right.get === Seq(f1))

    val expr2 = "/*[contains(.,'fxxxxxoo')]"
    val rtn2 = ee.evaluate(soo, expr2)
    assert(rtn2.right.get === Nil)
  }

  it should "use XPath style starts-with function against ." in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedContent = "this is incorrect"
    val line = inputA + unmatchedContent + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)

    val expr = "/*[starts-with(., 'fo')]"
    val rtn = ee.evaluate(soo, expr)
    assert(rtn.right.get === Seq(f1))

    val expr2 = "/*[starts-with(.,'fxxxxxoo')]"
    val rtn2 = ee.evaluate(soo, expr2)
    assert(rtn2.right.get === Nil)
  }

  it should "use XPath style contains function against child" in {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedContent = "this is incorrect"
    val bollocks2 = "(more bollocks)"
    val line = inputA + unmatchedContent + inputB + inputC + bollocks2 + inputD
    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))
    val ff1 = new MutableTerminalTreeNode("c1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("c2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + bollocks2.length))
    val f3 = new SimpleMutableContainerTreeNode("c", Seq(ff1, ff2), ff1.startPosition, endOf(line), TreeNode.Signal)
    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2, f3), line)

    val expr = "/*[contains(c1,'Lisbo')]"
    val rtn = ee.evaluate(soo, expr)
    assert(rtn.right.get === Seq(f3))
  }

  it should "handle a property name axis specifier and Object type" in {
    val tn = new ContainerTreeNodeImpl("Issue", "Issue")
    tn.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    tn.addField(repo)
    val expr = """/Issue()[@state='open']/belongsTo::Repo()[@name2='rug-cli']"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(tn)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(repo))
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
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(repo))
  }

  it should "handle a property name axis specifier nested predicate with Object type" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][belongsTo::Repo()[@name2='rug-cli']]"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "not match due to unsatisfied nested predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr: PathExpression = """/Issue()[@state='open'][nonsense::Repo()[@name2='rug-cli']]"""
    //println(expr)
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Nil)
  }

  it should "match with an optional predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val expr = """/Issue()[@state='open']?"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "match with a non-matching optional predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val expr = """/Issue()[@state='closed']?"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "match with a required and an optional predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][belongsTo::Repo()[@name2='rug-cli']]?"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "match with a required and a non-matching optional predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][nonsense::Repo()[@name2='rug-cli']]?"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "match with an optional nested predicate" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][belongsTo::Repo()[@name2='rug-cli']?]"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }

  it should "match with a non-matching optional nested predicate that does not" in {
    val issue = new ContainerTreeNodeImpl("Issue", "Issue")
    issue.addField(SimpleTerminalTreeNode("state", "open"))
    val repo = new ContainerTreeNodeImpl("belongsTo", "Repo")
    repo.addField(SimpleTerminalTreeNode("name2", "rug-cli"))
    issue.addField(repo)
    val expr = """/Issue()[@state='open'][belongsTo::Repo()[@name2='rig-cli']?]"""
    val parent = new ContainerTreeNodeImpl("root", "root")
    parent.addField(issue)
    val rtn = ee.evaluate(parent, expr)
    assert(rtn.right.get === Seq(issue))
  }
}
