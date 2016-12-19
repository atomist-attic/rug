package com.atomist.rug.test

import com.atomist.rug.parser.CommonRugTokens
import com.atomist.source.FileArtifact


object RugTestTokens extends CommonRugTokens {

  val ScenarioToken = "scenario"

  val GivenToken = "given"

  val WhenToken = "when"

  val InputToken = "input"

  val ThenToken = "then"

  val ArchiveRootToken = "ArchiveRoot"

  val EmptyToken = "Empty"

  val NoChangeToken = "NoChange"

  val NotApplicableToken = "NotApplicable"

  val ShouldFailToken = "ShouldFail"

  val MissingParameters = "MissingParameters"

  val InvalidParameters = "InvalidParameters"

  val FilesUnderToken = "files under"

  val KeywordsToAvoidInBody = CommonReservedWords ++
    Set(ScenarioToken,
      GivenToken, ThenToken, NoChangeToken)
}
