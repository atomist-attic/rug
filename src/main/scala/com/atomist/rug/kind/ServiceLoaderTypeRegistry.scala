package com.atomist.rug.kind

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.typesafe.scalalogging.LazyLogging
import _root_.java.util.ServiceLoader

import _root_.scala.collection.mutable.WeakHashMap
import _root_.scala.collection.JavaConverters._

/**
  * Use JDK ServiceLocator to load Type classes. Each
  * JAR files needs a META-INF/services/com.atomist.rug.spi.Typed file containing
  * the FQNs of the types it defines.
  *
  * @see [[Type]]
  */
class ServiceLoaderTypeRegistry
  extends TypeRegistry
    with LazyLogging {

  // Sharing Rug in a hierarchy of CLassLoaders requires the ServiceLoader to be triggered
  // for each ClassLoader; using WeakHashMap so ClassLoader references can be garbage collected
  private val typesMapByClassLoader = WeakHashMap[ClassLoader, Map[String, Typed]]()

  private def typesMap: Map[String, Typed] = {
    typesMapByClassLoader.get(Thread.currentThread().getContextClassLoader) match {
      case Some(tm) => tm
      case _ =>
        val typesMap: Map[String, Typed] = ServiceLoader.load(classOf[Typed]).asScala.map {
          case t: Typed =>
            logger.info(s"Registered type extension '${t.name}, with class ${t.getClass},description=${t.description}")
            val st = t
            logger.debug(s"Found operations: ${st.allOperations.map(_.name).mkString(",")}")
            t.name -> t
          case wtf =>
            throw new RugRuntimeException("ExtensionType", s"Type class ${wtf.getClass} must implement Typed interface", null)
        }.toMap
        typesMapByClassLoader += Thread.currentThread().getContextClassLoader -> typesMap
        typesMap
    }
  }

  override def findByName(kind: String): Option[Typed] = typesMap.get(kind)

  override def typeNames: Traversable[String] = typesMap.keys

  override def types: Seq[Typed] = typesMap.values.toSeq
}
