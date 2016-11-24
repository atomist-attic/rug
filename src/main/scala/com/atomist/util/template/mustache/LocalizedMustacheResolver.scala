package com.atomist.util.template.mustache

import java.io.{Reader, StringReader}

import com.github.mustachejava.resolver.DefaultResolver

class LocalizedMustacheResolver extends DefaultResolver {

  override def getReader(resourceName: String): Reader = {
    val reader = super.getReader(resourceName)
    if (reader == null) {
      new StringReader(resourceName)
    } else {
      reader
    }
  }
}
