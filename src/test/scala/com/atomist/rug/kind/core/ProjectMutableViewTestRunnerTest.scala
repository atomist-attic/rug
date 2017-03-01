package com.atomist.rug.kind.core

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.ArtifactSource
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class ProjectMutableViewTestRunnerTest extends FlatSpec with Matchers {

  // NOTES:
  // - Removing this.content = this.content + "otherstuff" from the view doesn't matter (test will still fail to replace text for last file in path)
  // - While debugging, we noticed that content of the file artifact view is different from parent files content for the same file
  // - Only last file doesn't get updated when replacing globally.  It looks like parent.replace actually changes files only when last file is getting processed and all
  // other calls to parent.replace get overridden by this.setPath and this.content modifications in between

  it should "Break a pom by replacing groupId with something broken using global replace" in
    doIt("Replacer.ts")

  it should "Break a pom by replacing groupId with something broken" in
    doIt("Replacer2.ts")

  private def doIt(prog: String) {
    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: SuccessfulModification => {
        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/PingController.java").get.content.contains("nonsense") should be(true)
        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/Test1Application.java").get.content.contains("nonsense") should be(true)
        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/Test1Configuration.java").get.content.contains("nonsense") should be(true)

        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/PingController.java").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/Test1Application.java").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/src/main/java/com/atomist/test1/Test1Configuration.java").get.content.contains("otherstuff") should be(true)

        val badFileContent = nmn.result.findFile("newroot/src/test/java/com/atomist/test1/Test1WebIntegrationTests.java").get.content
        badFileContent.contains("nonsense") should be(true)
        nmn.result.findFile("newroot/src/test/java/com/atomist/test1/Test1WebIntegrationTests.java").get.content.contains("otherstuff") should be(true)
      }
      case _ => ???
    }
  }

  it should "find string replacing type" in {
    assert(DefaultTypeRegistry.findByName("ReplacerClj").isDefined)
  }

  it should "change the contents of some clojure files" in {
    updateWith("ReplacerClj.ts", ClassPathArtifactSource.toArtifactSource("./lein_package_rename")) match {
      case nmn: SuccessfulModification => {
        nmn.result.findFile("newroot/src/com/atomist/sample/core.clj").get.content.contains("com.atomist.wassom") should be(true)
        nmn.result.findFile("newroot/src/com/atomist/sample/core.clj").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/src/com/atomist/sample/blah.clj").get.content.contains("com.atomist.wassom") should be(true)
        nmn.result.findFile("newroot/src/com/atomist/sample/blah.clj").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/test/com/atomist/sample/t_core.clj").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/test/com/atomist/sample/t_core.clj").get.content.contains("com.atomist.wassom") should be(true)
        nmn.result.findFile("newroot/test/com/atomist/sample/t_zzz.clj").get.content.contains("otherstuff") should be(true)
        nmn.result.findFile("newroot/test/com/atomist/sample/t_zzz.clj").get.content.contains("com.atomist.wassom") should be(true)
      }
      case _ => ???
    }
  }

  // Return new content
  private def updateWith(ts: String, project: ArtifactSource): ModificationAttempt = {
    TestUtils.editorInSideFile(this, ts).modify(project, SimpleParameterValues(Map(
      "foo" -> "bar"
    )))
  }
}
