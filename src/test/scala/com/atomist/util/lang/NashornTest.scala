package com.atomist.util.lang

import org.scalatest.{BeforeAndAfterEach, Suite}

/**
  * Sets a system property to enable nashorn instead of v8
  */
trait NashornTest extends Suite with BeforeAndAfterEach{

  override protected def beforeEach(): Unit = {
    System.setProperty("rug.javascript.engine", "nashorn")
  }

  override protected def afterEach(): Unit = {
    System.setProperty("rug.javascript.engine", "v8")
  }
}
