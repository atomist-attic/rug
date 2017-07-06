package com.eclipsesource.v8

/**
  * Wrap with some stuff with similar scope
  */
class NodeWrapper(val node: NodeJS) {

  private val refs = scala.collection.mutable.HashMap[Int,AnyRef]()

  // to avoid infinite loops around properties
  // didn't happen with nashorn because we didn't need to proxy everything before the call
  // but instead on the fly for each jvm object
  private val classes = scala.collection.mutable.Set[Class[_]]()

  def getRuntime: V8 = node.getRuntime

  def put(v8Object: V8Object, obj: AnyRef): Unit = {
    val code = obj.hashCode()
    v8Object.add("__object_reference", code)
    refs.put(code,obj)
    classes.add(obj.getClass)
  }

  def alreadyProxied(obj: AnyRef): Boolean = {
    classes.contains(obj.getClass)
  }

  def get(v8Object: V8Object): Option[AnyRef] = {
    v8Object.get("__object_reference") match {
      case o: Integer => refs.get(o)
      case _ => None
    }
  }
}
