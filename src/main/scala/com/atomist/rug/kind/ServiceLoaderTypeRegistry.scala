package com.atomist.rug.kind

import _root_.java.util.ServiceLoader

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.typesafe.scalalogging.LazyLogging

import _root_.scala.collection.JavaConverters._

/**
  * Use JDK ServiceLocator to load Type classes. Each
  * JAR files needs a META-INF/services/com.atomist.rug.spi.Typed file containing
  * the FQNs of the types it defines.
  *
  * @see Type
  */
class ServiceLoaderTypeRegistry
  extends TypeRegistry
    with LazyLogging {

  private lazy val typesMap: Map[String, Typed] = {
    ServiceLoader.load(classOf[Typed]).asScala.map {
      case t: Typed =>
        logger.info(s"Registered type extension '${t.name}, with class ${t.getClass},description=${t.description}")
        t.typeInformation match {
          case st: StaticTypeInformation =>
            logger.debug(s"Found operations: ${st.operations.map(_.name).mkString(",")}")
          case _ =>
        }
        t.name -> t
      case wtf =>
        throw new RugRuntimeException("ExtensionType", s"Type class ${wtf.getClass} must implement Typed interface", null)
    }
  }.toMap

  override def findByName(kind: String): Option[Typed] = typesMap.get(kind)

  override def typeNames: Traversable[String] = typesMap.keys

  override def types: Seq[Typed] = typesMap.values.toSeq
}
