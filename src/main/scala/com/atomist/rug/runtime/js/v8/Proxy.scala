package com.atomist.rug.runtime.js.v8

import java.lang.reflect.Method

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.{ExposeAsFunction, JavaScriptBackedGraphNode, ScriptObjectBackedTreeNode}
import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.{TerminalTreeNode, TreeNode}
import com.eclipsesource.v8._
import com.eclipsesource.v8.utils.MemoryManager
import org.apache.commons.lang3.ClassUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.ReflectionUtils

import scala.collection.JavaConverters._

/**
  * Create proxies for Java objects
  *
  * // TODO cache reflective calls for perf.
  */
object Proxy {

  def pushIfNeccessary(v8Array: V8Array, node: NodeWrapper, value: Any): V8Object = {
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => v8Array.push(o)
      case d: java.lang.Double => v8Array.push(d)
      case i: java.lang.Integer => v8Array.push(i)
      case s: String => v8Array.push(s)
      case v: V8Value => v8Array.push(v)
      case x => throw new RuntimeException(s"Could not push object: $x")
    }
  }

  def addIfNeccessary(v8Object: V8Object, node: NodeWrapper, name: String, value: Any): V8Object = {
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => v8Object.add(name, o)
      case d: java.lang.Double => v8Object.add(name, d)
      case i: java.lang.Integer => v8Object.add(name, i)
      case s: String => v8Object.add(name, s)
      case v: V8Value => v8Object.add(name, v)
      case x => throw new RuntimeException(s"Could not add object: $x")
    }
  }

  def addIfNeccessary(node: NodeWrapper, name: String, value: Any): V8Object = {
    val runtime = node.getRuntime
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => runtime.add(name, o)
      case d: java.lang.Double => runtime.add(name, d)
      case i: java.lang.Integer => runtime.add(name, i)
      case s: String => runtime.add(name, s)
      case v: V8Value => runtime.add(name, v)
      case x => throw new RuntimeException(s"Could not proxy object of type")
    }
  }

  def ifNeccessary(node: NodeWrapper, value: Any): AnyRef = value match {
    case o: AnyRef if ClassUtils.isPrimitiveWrapper(o.getClass) =>
      o match {
        case l: java.lang.Long => l.intValue().asInstanceOf[AnyRef]
        case _ => o
      }
    case j: ScriptObjectBackedTreeNode =>
      j.som.getNativeObject
    case s: String => s
    case v: V8Value => v
    case o: V8JavaScriptObject =>
      //      //o.getNativeObject.asInstanceOf[V8Object].release()
      o.getNativeObject
    case Some(r: AnyRef) => Proxy(node, r)
    case r: AnyRef => Proxy(node, r)
    case x => x.asInstanceOf[AnyRef]
  }

  // because for some reason we can't use classOf when matching!
  // AND they have to be upper case!!!!
  private val JList = classOf[java.util.List[_]]
  private val JString = classOf[String]
  private val JLong = classOf[Long]
  private val JDouble = classOf[Double]
  private val JInt = classOf[Int]
  private val JBoolean = classOf[Boolean]

  /**
    * Create a proxy around JVM obj
    *
    * @param obj
    * @return
    */
  private def apply(node: NodeWrapper, obj: AnyRef): V8Object = {

    if (ClassUtils.isPrimitiveWrapper(obj.getClass) || obj.isInstanceOf[String]) {
      throw new RuntimeException(s"Should not try to proxy primitive wrappers or strings: $obj")
    }

    val v8pmv = new V8Object(node.getRuntime)

    node.put(v8pmv, obj)

    obj match {
      case o: Seq[_] =>
        val arr = new V8Array(node.getRuntime)
        o.foreach {
          case item: AnyRef =>
            Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case l: java.util.List[_] =>
        val arr = new V8Array(node.getRuntime)
        l.asScala.foreach {
          case item: AnyRef => Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case l: Set[_] =>
        val arr = new V8Array(node.getRuntime)
        l.foreach {
          case item: AnyRef => Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case _ =>
        val methods = obj.getClass.getMethods.filter(m => !ReflectionUtils.isObjectMethod(m))

        methods.filter(m => exposeAsProperty(m)).foreach(m => {
          withMemoryManagement(node, {
            val callback = new V8Object(node.getRuntime)
            val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
            RegisterMethodProxy(callback, node, obj, "get", m)
            callback.add("configurable", true)
            theObject.executeJSFunction("defineProperty", v8pmv, m.getName, callback)
          })
        })

        // for overloaded methods in gherking stuff
        val grouped = methods.
          filter(m => exposeAsFunction(m)).
          groupBy(m => m.getName)

        grouped.foreach(mm => {
          RegisterMethodProxy(v8pmv, node, obj, mm._2.head.getName, mm._2: _*)
        })

        /**
          * Use getters for fields to so that we don't need to proxy recursively
          */
        obj.getClass.getFields.foreach(f => {
          withMemoryManagement(node, {
            val callback = new V8Object(node.getRuntime)
            callback.registerJavaMethod(new JavaCallback {
              override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
                Proxy.ifNeccessary(node, f.get(obj))
              }
            }, "get")
            callback.add("configurable", true)
            val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
            theObject.executeJSFunction("defineProperty", v8pmv, f.getName, callback)
          })
        })

        obj match {
          case n: GraphNode if n.hasTag(TreeNode.Dynamic) =>
            if (n.nodeName != "value" && n.relatedNodes.forall(p => p.nodeName != "value")) {
              withMemoryManagement(node, {
                n.relatedNodes.foreach {
                  case related: TerminalTreeNode =>
                    //just make sure the value is there
                    Proxy.addIfNeccessary(v8pmv, node, related.nodeName, related.value)
                  case related =>
                    val callback = new V8Object(node.getRuntime)
                    callback.registerJavaMethod(new JavaCallback {
                      override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
                        related match {
                          case t: TerminalTreeNode => Proxy.ifNeccessary(node, t.value)
                          case _ => Proxy.ifNeccessary(node, related)
                        }
                      }
                    }, "get")
                    callback.add("configurable", false)
                    val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
                    theObject.executeJSFunction("defineProperty", v8pmv, related.nodeName, callback)
                }

                n match {
                  case js: JavaScriptBackedGraphNode =>
                    js.traversableEdges.foreach {
                      case (edge, nodes) if nodes.exists(_.isInstanceOf[TerminalTreeNode]) && !v8pmv.contains(edge) =>
                        Proxy.addIfNeccessary(v8pmv, node, edge, nodes)
                      case (edge, nodes) if !v8pmv.contains(edge) =>
                        js.scriptObject.getMember(edge) match {
                          case o: V8JavaScriptObject if !o.isFunction =>
                            val callback = new V8Object(node.getRuntime)
                            callback.registerJavaMethod(new JavaCallback {
                              override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
                                nodes match {
                                  case Seq(t: TerminalTreeNode) => Proxy.ifNeccessary(node, t.value)
                                  case _ =>
                                    if(o.isSeq){
                                      Proxy.ifNeccessary(node, nodes)
                                    }else{
                                      Proxy.ifNeccessary(node, nodes.head)
                                    }
                                }
                              }
                            }, "get")
                            callback.add("configurable", true)
                            val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
                            theObject.executeJSFunction("defineProperty", v8pmv, edge, callback)
                          case _ =>
                        }
                      case _ =>
                    }
                  case _ =>
                }
              })
            }
          case _ =>
        }

        obj match {
          case n: GraphNode =>
            val srqlArray = new SquirrelFunction(node, n, true, DefaultTypeRegistry)
            v8pmv.registerJavaMethod(srqlArray, "$jumpInto")
            val srqlOne = new SquirrelFunction(node, n, false, DefaultTypeRegistry)
            v8pmv.registerJavaMethod(srqlOne, "$jumpIntoOne")
          case _ =>
        }
        v8pmv
    }
  }

  /**
    * Release any v8 resources after execution
    *
    * @param node
    * @param result
    * @return
    */
  def withMemoryManagement[T](node: NodeWrapper, result: => T): T = {
    val scope = new MemoryManager(node.getRuntime)
    try {
      result
    } finally {
      //scope.release()
    }
  }

  /**
    * Is method m to be exposed as a function?
    *
    * @param m
    * @return
    */
  def exposeAsFunction(m: Method): Boolean = {
    (AnnotationUtils.findAnnotation(m, classOf[ExportFunction]), AnnotationUtils.findAnnotation(m, classOf[ExposeAsFunction])) match {
      case (o: ExportFunction, null) => !o.exposeAsProperty()
      case (null, _: ExposeAsFunction) => true
      case _ => false
    }
  }

  /**
    * Is the read-only flag set?
    *
    * @param m
    * @return
    */
  def readOnly(m: Method): Boolean = {
    AnnotationUtils.findAnnotation(m, classOf[ExportFunction]) match {
      case o: ExportFunction => o.readOnly()
      case _ => false
    }
  }

  /**
    * Is method m to be exposed as a property?
    *
    * @param m
    * @return
    */
  def exposeAsProperty(m: Method): Boolean = {
    AnnotationUtils.findAnnotation(m, classOf[ExportFunction]) match {
      case o: ExportFunction => o.exposeAsProperty()
      case _ => false
    }
  }
}
