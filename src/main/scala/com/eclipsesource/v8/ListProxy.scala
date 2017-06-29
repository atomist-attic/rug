package com.eclipsesource.v8

import java.lang.reflect.Method

/**
  * Fronts a List or Seq to v8
  */
class ListProxy(node: NodeWrapper, array: AnyRef, method: Method)
  extends V8Array(node.getRuntime) {

  override def getType(index: Int): Int = super.getType(index)

  override def getType: Int = super.getType

  override def getType(index: Int, length: Int): Int = super.getType(index, length)

  override def getStrings(index: Int, length: Int): Array[String] = super.getStrings(index, length)

  override def getStrings(index: Int, length: Int, resultArray: Array[String]): Int = super.getStrings(index, length, resultArray)

  override def pushNull(): V8Array = super.pushNull()

  override def getIntegers(index: Int, length: Int): Array[Int] = super.getIntegers(index, length)

  override def getIntegers(index: Int, length: Int, resultArray: Array[Int]): Int = super.getIntegers(index, length, resultArray)

  override def getDoubles(index: Int, length: Int): Array[Double] = super.getDoubles(index, length)

  override def getDoubles(index: Int, length: Int, resultArray: Array[Double]): Int = super.getDoubles(index, length, resultArray)

  override def get(index: Int): AnyRef = super.get(index)

  override def getDouble(index: Int): Double = super.getDouble(index)

  override def getArray(index: Int): V8Array = super.getArray(index)

  override def getBooleans(index: Int, length: Int): Array[Boolean] = super.getBooleans(index, length)

  override def getBooleans(index: Int, length: Int, resultArray: Array[Boolean]): Int = super.getBooleans(index, length, resultArray)

  override def length(): Int = super.length()

  override def pushUndefined(): V8Array = super.pushUndefined()

  override def createTwin(): V8Value = super.createTwin()

  override def push(value: Int): V8Array = super.push(value)

  override def push(value: Boolean): V8Array = super.push(value)

  override def push(value: Double): V8Array = super.push(value)

  override def push(value: String): V8Array = super.push(value)

  override def push(value: V8Value): V8Array = super.push(value)

  override def initialize(runtimePtr: Long, data: scala.Any): Unit = super.initialize(runtimePtr, data)

  override def twin(): V8Array = super.twin()

  override def getByte(index: Int): Byte = super.getByte(index)

  override def getBoolean(index: Int): Boolean = super.getBoolean(index)

  override def getObject(index: Int): V8Object = super.getObject(index)

  override def getInteger(index: Int): Int = super.getInteger(index)

  override def getBytes(index: Int, length: Int): Array[Byte] = super.getBytes(index, length)

  override def getBytes(index: Int, length: Int, resultArray: Array[Byte]): Int = super.getBytes(index, length, resultArray)

  override def getString(index: Int): String = super.getString(index)

  override def getType(key: String): Int = super.getType(key)

  override def get(key: String): AnyRef = super.get(key)

  override def getArray(key: String): V8Array = super.getArray(key)

  override def getKeys: Array[String] = super.getKeys
}
