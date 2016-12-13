package com.atomist.util

import scala.collection.JavaConverters.seqAsJavaList

object Converters {

  implicit class AsJava[A](val a: Seq[A]) extends AnyVal {

    def asJavaColl[B >: A]: java.util.List[B] = {
      seqAsJavaList(a)
    }
  }
}
