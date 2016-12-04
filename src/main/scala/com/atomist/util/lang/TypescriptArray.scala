package com.atomist.util.lang

import java.util
import java.util.Comparator

import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

import scala.collection.JavaConversions


/**
  * Decorate a java.util.List instance with anything required to implement the TS array methods:
  * https://www.tutorialspoint.com/typescript/typescript_arrays.htm
  * Created by kipz on 01/12/2016.
  */
class TypescriptArray[T](var lyst: java.util.List[T])
  extends AbstractJSObject
    with java.util.List[T]{

  //let's create a new mutable list as operating on an immutable array from JS is kinda pointless - can't even filter!
  //TODO - is there a way to check if collection is immutable? Would be nice if we could just pass the reference here for mutable lists
  this.lyst = new util.ArrayList[T](lyst)

  override def removeAll(collection: util.Collection[_]): Boolean = {
    lyst.removeAll(collection)
  }

  override def subList(i: Int, i1: Int): util.List[T] = {
    lyst.subList(i, i1)
  }

  override def set(i: Int, e: T): T = {
    lyst.set(i, e)
  }

  override def indexOf(o: scala.Any): Int = {
    lyst.indexOf(o)
  }

  override def get(i: Int): T = {
    lyst.get(i)
  }

  override def lastIndexOf(o: scala.Any): Int = {
    lyst.lastIndexOf(o)
  }

  override def retainAll(collection: util.Collection[_]): Boolean = {
    lyst.retainAll(collection)
  }

  override def clear(): Unit = {
    lyst.clear()
  }

  override def toArray: Array[AnyRef] = {
    lyst.toArray()
  }

  override def listIterator(): util.ListIterator[T] = {
    lyst.listIterator()
  }

  override def listIterator(i: Int): util.ListIterator[T] = {
    lyst.listIterator(i)
  }

  override def size(): Int = {
    lyst.size()
  }

  override def remove(o: scala.Any): Boolean = {
    lyst.remove(o)
  }

  override def remove(i: Int): T = {
    lyst.remove(i)
  }

  override def contains(o: scala.Any): Boolean = {
    lyst.contains(o)
  }

  override def iterator(): util.Iterator[T] = {
    lyst.iterator()
  }

  override def addAll(collection: util.Collection[_ <: T]): Boolean = {
    lyst.addAll(collection)
  }

  override def addAll(i: Int, collection: util.Collection[_ <: T]): Boolean = {
    lyst.addAll(i, collection)
  }

  override def isEmpty: Boolean = {
    lyst.isEmpty()
  }

  override def containsAll(collection: util.Collection[_]): Boolean = {
    lyst.containsAll(collection)
  }

  override def add(e: T): Boolean = {
    lyst.add(e)
  }

  override def add(i: Int, e: T): Unit = {
    lyst.add(i,e)
  }

  //TODO - odd that intellij doens't like this, but generates a guff implementation by default...
  override def toArray[X](ts: Array[X with Object]): Array[X with Object] = {
    lyst.toArray[X](ts)
  }

  override def getMember(name: String): AnyRef = {
    name match {
      case "filter" =>
         new AbstractJSObject {
           override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
             val filterfn = args.head.asInstanceOf[ScriptObjectMirror]
             val iter = lyst.listIterator()
             while (iter.hasNext) {
               val thing = iter.next()
               val thisArg = if(args.length > 1) args(1) else thiz
               if (!filterfn.call(thisArg, thing.asInstanceOf[Object]).asInstanceOf[Boolean]) {
                 iter.remove()
               }
             }
             lyst
           }
         }
      case "length" => lyst.size().asInstanceOf[AnyRef]
      case "toString" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          lyst.toString
        }
      }
      case "toLocaleString" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          lyst.toString
        }
      }

      case "push" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          args.foreach( a => lyst.add(a.asInstanceOf[T]))
          lyst.size().asInstanceOf[AnyRef]
        }
      }

      case "concat" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val newList = new util.ArrayList[T]()
          //ensure we can add util.List as well as pure JS arrays
          args.foreach {
            case ts: util.Collection[T] =>
              newList.addAll(ts)
            case a =>
              newList.addAll(a.asInstanceOf[ScriptObjectMirror].values().asInstanceOf[util.Collection[T]])
          }

          newList.addAll(lyst)
          new TypescriptArray(newList)
        }
      }
      case "pop" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          lyst.remove(lyst.size() -1).asInstanceOf[AnyRef]
        }
      }

      case "join" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val sep = if(args.nonEmpty) args.head.asInstanceOf[String] else ","
          import scala.collection.JavaConverters._
          lyst.iterator().asScala.mkString(sep)
        }
      }
      case "reverse"=> new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          util.Collections.reverse(lyst)
          lyst
        }
      }
      case "shift" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          lyst.remove(0).asInstanceOf[AnyRef]
        }
      }
      case "slice" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {

          args.length match {
            case 0 => new util.ArrayList[T](lyst)
            case _ => {
              val head = args.head.asInstanceOf[Int]
              val begin = if (head >= 0) head else lyst.size() + head
              args.length match {
                case 1 => lyst.subList(begin,lyst.size())
                case _ => {
                  val theEnd = args(1).asInstanceOf[Int]
                  val end = if (theEnd >= 0) theEnd else lyst.size() + theEnd
                  lyst.subList(begin,end)
                }
              }
            }
          }
        }
      }
      case "sort" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          args.length match {
            case 0 => {
              lyst.sort(new Comparator[T](){
                override def compare(t: T, t1: T): Int = {
                  t.toString.compareTo(t1.toString)
                }
              })
              lyst
            }
            case 1 => {
              val filterfn = args.head.asInstanceOf[ScriptObjectMirror]
              lyst.sort(new Comparator[T] {
                override def compare(t: T, t1: T): Int = {
                  val ret = filterfn.call(thiz, t.asInstanceOf[Object], t1.asInstanceOf[Object])
                  //TODO - why does replacing this with matching fail to compile?
                  if(ret.isInstanceOf[Double]){
                    ret.asInstanceOf[Double].toInt
                  }else{
                    throw new RuntimeException("Unrecognised return type from comparator: " + ret.getClass.getName)
                  }
                }
              })
              lyst
            }
          }
        }
      }
      case "splice" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          args.length match {
            case 0 => throw new RuntimeException("splice requires at least one parameter")
            case _ => {
              val head = args.head.asInstanceOf[Int]
              val begin = if (head >= lyst.size()) lyst.size() else if(head < 0) lyst.size() + head -1 else head
              args.length match {
                case 1 => {
                  val deleteCount = lyst.size() - begin
                  val result = new TypescriptArray[T](new util.ArrayList[T])
                  for(i <- 1 to deleteCount){
                    result.add(lyst.remove(begin))
                  }
                  result
                }
                case _ => {
                  val deleteCount = Math.min(args(1).asInstanceOf[Int],lyst.size())
                  val result = new TypescriptArray[T](new util.ArrayList[T])

                  for(i <- 1 to deleteCount){
                    result.add(lyst.remove(begin))
                  }
                  args.length match {
                    case 2 => result
                    case _ => {
                      args.drop(2).foreach(t => lyst.add(t.asInstanceOf[T]))
                      result
                    }
                  }
                }
              }
            }
          }
        }
      }
      case "unshift" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          args.reverse.foreach( t => lyst.add(0,t.asInstanceOf[T]))
          lyst.size().asInstanceOf[AnyRef]
        }
      }
      case "indexOf" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val item = args.head.asInstanceOf[T]
          args.length match {
            case 1 => {
              lyst.indexOf(item).asInstanceOf[AnyRef]
            }
            case _ => {
              val start = args(1).asInstanceOf[Int]
              val res = lyst.subList(start,lyst.size()).indexOf(item)
              if(res >= 0){
                (res + start).asInstanceOf[AnyRef]
              }else{
                res.asInstanceOf[AnyRef]
              }
            }
          }
        }
      }
      case "lastIndexOf" => new AbstractJSObject{
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val item = args.head.asInstanceOf[T]
          args.length match {
            case 1 => {
              lyst.lastIndexOf(item).asInstanceOf[AnyRef]
            }
            case _ => {
              val start = args(1).asInstanceOf[Int]
              val res = lyst.subList(start,lyst.size()).lastIndexOf(item)
              if(res >= 0){
                (res + start).asInstanceOf[AnyRef]
              }else{
                res.asInstanceOf[AnyRef]
              }

            }
          }
        }
      }

      case "every" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val everyfn = args.head.asInstanceOf[ScriptObjectMirror]

          //where's the every in scala?
          val iter = lyst.listIterator()
          var ret = true
          while (iter.hasNext) {
            val thing = iter.next()
            val thisArg = if(args.length > 1) args(1) else thiz
            if (!everyfn.call(thisArg, thing.asInstanceOf[Object]).asInstanceOf[Boolean]) {
              return false.asInstanceOf[AnyRef]
            }
          }
          true.asInstanceOf[AnyRef]
        }
      }

      case "some" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val some = args.head.asInstanceOf[ScriptObjectMirror]
          val iter = lyst.listIterator()
          var ret = true
          while (iter.hasNext) {
            val thing = iter.next()
            val thisArg = if(args.length > 1) args(1) else thiz
            if (some.call(thisArg, thing.asInstanceOf[Object]).asInstanceOf[Boolean]) {
              return true.asInstanceOf[AnyRef]
            }
          }
          false.asInstanceOf[AnyRef]
        }
      }

      case "forEach" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val forEach = args.head.asInstanceOf[ScriptObjectMirror]
          val iter = lyst.listIterator()
          while (iter.hasNext) {
            val thing = iter.next()
            val thisArg = if(args.length > 1) args(1) else thiz
            forEach.call(thisArg, thing.asInstanceOf[Object])
          }
          None.asInstanceOf[AnyRef]
        }
      }
      case "map" => new AbstractJSObject {
        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val map = args.head.asInstanceOf[ScriptObjectMirror]
          val iter = lyst.listIterator()
          val res = new TypescriptArray[AnyRef](new util.ArrayList[AnyRef]())
          while (iter.hasNext) {
            val thing = iter.next()
            val thisArg = if(args.length > 1) args(1) else thiz
            res.add(map.call(thisArg, thing.asInstanceOf[Object]))
          }
          res
        }
      }

      case "reduce" => new ReducerJSObject[T](lyst)

      case "reduceRight" => {
        val reversed = new TypescriptArray[T](new util.ArrayList[T](lyst))
        util.Collections.reverse(reversed)
        new ReducerJSObject[T](reversed)
      }

      case _ => super.getMember(name)
    }
  }

  override def isArray: Boolean = true

  override def getSlot(index: Int): AnyRef = lyst.get(index).asInstanceOf[AnyRef]
}

class ReducerJSObject[Y] (lst: util.List[Y]) extends AbstractJSObject{

  override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {

    //as per https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/Reduce

    if(lst.size() == 0 && args.length < 2){
      throw new RuntimeException("TypeError: empty array and no initialValue")
    }

    if(lst.size() == 1 && args.length < 2){
      return lst.get(0).asInstanceOf[AnyRef]
    }
    if(lst.size() == 0 && args.length > 1){
      return args(1)
    }

    val reduceFn = args.head.asInstanceOf[ScriptObjectMirror]
    var acc = if(args.length > 1) args(1) else lst.get(0)
    val idx = if(args.length > 1) 0 else 1

    for(i <- idx until lst.size() ){
      acc = reduceFn.call(thiz,acc.asInstanceOf[AnyRef], lst.get(i).asInstanceOf[AnyRef], i.asInstanceOf[AnyRef], lst)
    }
    acc.asInstanceOf[AnyRef]
  }
}
