package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileMutableView
import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

class jsSafeCommittingProxyTest extends FlatSpec with Matchers {

  it should "not allow invocation of non export function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("bla")
    }
  }

  it should "fail for unregistered command function" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    val sc = new jsSafeCommittingProxy(fmv, DefaultTypeRegistry)
    intercept[RugRuntimeException] {
      sc.getMember("delete")
    }
  }

}
