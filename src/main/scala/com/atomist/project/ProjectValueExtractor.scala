package com.atomist.project

import _root_.java.util.{List => JList}

import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * Extracts one thing from another. Intended to be compasable.
  *
  * @tparam T
  * @tparam R
  */
trait Extractor[T, R] extends Function[T, R] {
}

/**
  * Get these out of the trait using an implicit class, to ensure
  * that Extractor isn't hard to implement in Java.
  */
object ExtractorOps {

  implicit class OpsExtractor[T, R](extractor: Extractor[T, R])
    extends Extractor[T, R] {

    import scala.collection.JavaConverters._

    /** Right associative, as names ends with : */
    def >-:(t: T) = apply(t): R

    def extractAll(ts: JList[T]): JList[R] = ts.asScala.map(apply).asJava

    def *:(ts: JList[T]): JList[R] = extractAll(ts)

    override def apply(t: T): R = extractor(t)
  }
}
