package com.atomist.project.archive

import com.atomist.project.edit.common.CompoundProjectEditor
import com.atomist.rug.kind.java.spring.{ApplicationPropertiesToApplicationYmlEditor, ApplicationYmlKeyAddingEditor}
import com.atomist.rug.rugdoc.TypeDoc
import com.atomist.rug.runtime.NamespaceUtils._

import scala.collection.JavaConversions._

/**
  * Well known project operations.
  */
object CoreProjectOperations {

  def apply(namespace: Option[String]): Operations = {
    val coreEditors = Seq(
      ApplicationPropertiesToApplicationYmlEditor,
      ApplicationYmlKeyAddingEditor,
      new TypeDoc()
    )

    val inDesiredNamespace = namespace match {
      case None => coreEditors
      case Some(ns) => coreEditors.map(ce =>
        new CompoundProjectEditor(namespaced(ce.name, namespace), ce.description, Seq(ce))
      )
    }

    val generators = Seq(new TypeDoc())
    Operations(generators, inDesiredNamespace, Nil, Nil)
  }
}
