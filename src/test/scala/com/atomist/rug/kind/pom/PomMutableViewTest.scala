package com.atomist.rug.kind.pom

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class PomMutableViewTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  import PomMutableViewTestSupport._

  lazy val pom = JavaTypeUsageTest.NewSpringBootProject.findFile("pom.xml").get

  var validPomUut: PomMutableView = _

  lazy val pomNoParent = JavaTypeUsageTest.NewSpringBootProject.findFile("pomNoParent.xml").get

  lazy val pomWithDependencyManagement = JavaTypeUsageTest.NewSpringBootProject.findFile("pomWithDependencyManagement.xml").get

  lazy val pomWithPluginManagement = JavaTypeUsageTest.NewSpringBootProject.findFile("pomWithPluginManagement.xml").get

  lazy val pomWithProfile = JavaTypeUsageTest.NewSpringBootProject.findFile("pomWithProfile.xml").get

  var validPomNoParent: PomMutableView = _

  var validPomWithDependencyManagement: PomMutableView = _

  var validPomWithPluginManagement: PomMutableView = _

  var validPomWithProfile: PomMutableView = _

  private def testConditions(uut: PomMutableView, response: String, expectedResponse: String, dirty: Boolean = false) = {
    response should be(expectedResponse)
    assert(uut.dirty === dirty)
  }

  override def beforeEach() {
    validPomUut = new PomMutableView(pom, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    validPomNoParent = new PomMutableView(pomNoParent, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    validPomWithDependencyManagement = new PomMutableView(pomWithDependencyManagement, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    validPomWithPluginManagement = new PomMutableView(pomWithPluginManagement, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))
    validPomWithProfile = new PomMutableView(pomWithProfile, new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.NewSpringBootProject))

  }

  it should "get the project group id" in {
    testConditions(validPomUut, validPomUut.groupId, "atomist")
  }

  it should "get the project artifact id" in {
    testConditions(validPomUut, validPomUut.artifactId, "test1")
  }

  it should "get the project version" in {
    testConditions(validPomUut, validPomUut.version, "0.0.1")
  }

  it should "get the project packaging" in {
    testConditions(validPomUut, validPomUut.packaging, "jar")
  }

  it should "get the project name" in {
    testConditions(validPomUut, validPomUut.name, "test1")
  }

  it should "get the project description" in {
    testConditions(validPomUut, validPomUut.description, "$description")
  }

  it should "get the current parent groupId when present" in {
    testConditions(validPomUut, validPomUut.parentGroupId, "org.springframework.boot")
  }

  it should "get the current parent artifactId when present" in {
    testConditions(validPomUut, validPomUut.parentArtifactId, "spring-boot-starter-parent")
  }

  it should "get the current parent version when present" in {
    testConditions(validPomUut, validPomUut.parentVersion, "1.3.5.RELEASE")
  }

  it should "respond sensibly when parent block is not present" in {
    testConditions(validPomNoParent, validPomNoParent.parentVersion, "")
  }

  it should "respond with Project property when present" in {
    testConditions(validPomUut, validPomUut.property("project.build.sourceEncoding"), "UTF-8")
  }

  it should "respond without project property sensibly when not present" in {
    testConditions(validPomUut, validPomUut.property("project.build.sourceEncodingDUMMY"), "")
  }

  it should "respond with a known dependency's version requested" in {
    testConditions(validPomUut, validPomUut.dependencyVersion("testgroup", "testartifact"), "0.0.2")
  }

  it should "respond sensibly with a known dependency's unknown version requested" in {
    testConditions(validPomUut, validPomUut.dependencyScope(springBootGroupId, springBootStarterWebArtifactId), "")
  }

  it should "respond with a known dependency's scope requested" in {
    testConditions(validPomUut, validPomUut.dependencyScope(springBootGroupId, springBootStarterTestArtifactId), "test")
  }

  it should "respond sensibly with a known dependency's unknown scope requested" in {
    testConditions(validPomUut, validPomUut.dependencyScope(springBootGroupId, springBootStarterWebArtifactId), "")
  }

  it should "respond successfully if a direct dependency is present based on artifactId and groupId" in {
    testConditions(validPomUut, validPomUut.isDependencyPresent(springBootGroupId, springBootStarterWebArtifactId).toString, true.toString)
  }

  it should "respond unsuccessfully if a direct dependency is not present based on incorrect artifactId and correct groupId" in {
    testConditions(validPomUut, validPomUut.isDependencyPresent(springBootGroupId, "spring-boot-starter-web-DUMMY").toString, false.toString)
  }

  it should "respond unsuccessfully if a direct dependency is not present based on correct artifactId and incorrect groupId" in {
    testConditions(validPomUut, validPomUut.isDependencyPresent("org.springframework.boot-DUMMY", springBootStarterWebArtifactId).toString, false.toString)
  }

  it should "respond unsuccessfully if a direct dependency is not present based on incorrect artifactId and incorrect groupId" in {
    testConditions(validPomUut, validPomUut.isDependencyPresent("spring-boot-starter-web-DUMMY", "org.springframework.boot-DUMMY").toString, false.toString)
  }

  it should "respond successfully if a project build plugin is present based on artifactId and groupId" in {
    testConditions(validPomUut, validPomUut.isBuildPluginPresent(gitCommitPluginGroupId, gitCommitPluginArtifactId).toString, true.toString)
  }

  it should "respond unsuccessfully if a project build plugin is not present based on incorrect artifactId and correct groupId" in {
    testConditions(validPomUut, validPomUut.isBuildPluginPresent(gitCommitPluginGroupId, "git-commit-id-plugin-DUMMY").toString, false.toString)
  }

  it should "respond unsuccessfully if a project build plugin is not present based on correct artifactId and incorrect groupId" in {
    testConditions(validPomUut, validPomUut.isBuildPluginPresent("pl.project13.maven-DUMMY", gitCommitPluginArtifactId).toString, false.toString)
  }

  it should "respond unsuccessfully if a project build plugin is not present based on incorrect artifactId and incorrect groupId" in {
    testConditions(validPomUut, validPomUut.isBuildPluginPresent("git-commit-id-plugin-DUMMY", "pl.project13.maven-DUMMY").toString, false.toString)
  }

  val shouldBeDirty = true
  val shouldNotBeDirty = false

  it should "update groupId" in {
    val originalValue = "atomist"
    testConditions(validPomUut, validPomUut.groupId, originalValue, shouldNotBeDirty)

    val newGroupId = "bowling-team"
    validPomUut.setGroupId(newGroupId)

    testConditions(validPomUut, validPomUut.groupId, newGroupId, shouldBeDirty)
  }

  it should "update artifactId" in {
    val originalValue = "test1"
    testConditions(validPomUut, validPomUut.artifactId, originalValue, shouldNotBeDirty)

    val newArtifactId = "white-russian"
    validPomUut.setArtifactId(newArtifactId)

    testConditions(validPomUut, validPomUut.artifactId, newArtifactId, shouldBeDirty)
  }

  it should "update version" in {
    val originalValue = "0.0.1"
    testConditions(validPomUut, validPomUut.version, originalValue, shouldNotBeDirty)

    val newVersion = "1.0.0"
    validPomUut.setVersion(newVersion)

    testConditions(validPomUut, validPomUut.version, newVersion, shouldBeDirty)
  }

  it should "update packaging" in {
    val originalValue = "jar"
    testConditions(validPomUut, validPomUut.packaging, originalValue, shouldNotBeDirty)

    val newPackaging = "pom"
    validPomUut.setPackaging(newPackaging)

    testConditions(validPomUut, validPomUut.packaging, newPackaging, shouldBeDirty)
  }

  it should "update project name" in {
    val originalValue = "test1"
    testConditions(validPomUut, validPomUut.name, originalValue, shouldNotBeDirty)

    val newName = "Dude"
    validPomUut.setProjectName(newName)

    testConditions(validPomUut, validPomUut.name, newName, shouldBeDirty)
  }

  it should "update description" in {
    val originalValue = "$description"
    testConditions(validPomUut, validPomUut.description, originalValue, shouldNotBeDirty)

    val newDescription = "Obviously you're not a golfer"
    validPomUut.setDescription(newDescription)

    testConditions(validPomUut, validPomUut.description, newDescription, shouldBeDirty)
  }

  it should "update existing parent groupId" in {

    val (_, originalParentArtifactId: String, originalParentVersion: String) = assertParentBlockInitialState

    val newParentGroupId = "bowling-team"
    validPomUut.setParentGroupId(newParentGroupId)

    testConditions(validPomUut, validPomUut.parentGroupId, newParentGroupId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentArtifactId, originalParentArtifactId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentVersion, originalParentVersion, shouldBeDirty)
  }

  it should "not update parent groupId when parent section is not present" in {
    val originalParentGroup = ""
    val originalParentArtifactId = ""
    val originalParentVersion = ""
    testConditions(validPomNoParent, validPomNoParent.parentGroupId, originalParentGroup, shouldNotBeDirty)
    testConditions(validPomNoParent, validPomNoParent.parentArtifactId, originalParentArtifactId, shouldNotBeDirty)
    testConditions(validPomNoParent, validPomNoParent.parentVersion, originalParentVersion, shouldNotBeDirty)

    val newParentGroupId = "bowling-team"
    validPomNoParent.setParentGroupId(newParentGroupId)

    testConditions(validPomNoParent, validPomNoParent.parentGroupId, originalParentGroup, shouldNotBeDirty)
    testConditions(validPomNoParent, validPomNoParent.parentArtifactId, originalParentArtifactId, shouldNotBeDirty)
    testConditions(validPomNoParent, validPomNoParent.parentVersion, originalParentVersion, shouldNotBeDirty)
  }

  it should "update existing parent artifactId" in {
    val (originalParentGroupId: String, _, originalParentVersion: String) = assertParentBlockInitialState

    val newParentArtifactId = "bowling-ball"
    validPomUut.setParentArtifactId(newParentArtifactId)

    testConditions(validPomUut, validPomUut.parentGroupId, originalParentGroupId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentArtifactId, newParentArtifactId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentVersion, originalParentVersion, shouldBeDirty)
  }

  it should "update existing parent version" in {
    val (originalParentGroupId: String, originalParentArtifactId: String, _) = assertParentBlockInitialState

    val newParentVersion = "1.0.1"
    validPomUut.setParentVersion(newParentVersion)

    testConditions(validPomUut, validPomUut.parentGroupId, originalParentGroupId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentArtifactId, originalParentArtifactId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentVersion, newParentVersion, shouldBeDirty)
  }

  it should "replace existing parent block" in {
    assertParentBlockInitialState

    val newParentGroupId = "bowling-team"
    val newParentArtifactId = "bowling-ball"
    val newParentVersion = "1.0.1"
    val newParentBlock =
      s"""
         |<parent>
         |		<groupId>$newParentGroupId</groupId>
         |		<artifactId>$newParentArtifactId</artifactId>
         |		<version>$newParentVersion</version>
         |		<relativePath/> <!-- lookup parent from repository -->
         |	</parent>
      """.stripMargin

    validPomUut.replaceParent(newParentBlock)

    testConditions(validPomUut, validPomUut.parentGroupId, newParentGroupId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentArtifactId, newParentArtifactId, shouldBeDirty)
    testConditions(validPomUut, validPomUut.parentVersion, newParentVersion, shouldBeDirty)
  }

  def assertParentBlockInitialState: (String, String, String) = {
    val originalParentGroupId = "org.springframework.boot"
    val originalParentArtifactId = "spring-boot-starter-parent"
    val originalParentVersion = "1.3.5.RELEASE"
    testConditions(validPomUut, validPomUut.parentGroupId, originalParentGroupId, shouldNotBeDirty)
    testConditions(validPomUut, validPomUut.parentArtifactId, originalParentArtifactId, shouldNotBeDirty)
    testConditions(validPomUut, validPomUut.parentVersion, originalParentVersion, shouldNotBeDirty)
    (originalParentGroupId, originalParentArtifactId, originalParentVersion)
  }

  it should "update an existing project property" in {
    val originalValue = "UTF-8"
    val propertyName = "project.build.sourceEncoding"
    testConditions(validPomUut, validPomUut.property(propertyName), originalValue, shouldNotBeDirty)

    val newValue = "ASCII"
    validPomUut.addOrReplaceProperty(propertyName, newValue)

    testConditions(validPomUut, validPomUut.property(propertyName), newValue, shouldBeDirty)
  }

  it should "add a new project property" in {
    validPomUut.contains("/project/properties/my.new.property") should be(false)
    assert(validPomUut.dirty === false)

    val propertyName = "my.new.property"
    val newValue = "mine-all-mine"
    validPomUut.addOrReplaceProperty(propertyName, newValue)

    testConditions(validPomUut, validPomUut.property(propertyName), newValue, shouldBeDirty)
  }

  it should "remove an existing project property" in {
    val propertyName = "project.build.sourceEncoding"
    validPomUut.property(propertyName) should not be ""

    validPomUut.removeProperty(propertyName)

    assert(validPomUut.dirty === true)

    validPomUut.property(propertyName) should be("")
  }

  it should "sensibly respond when attempting to remove a project property that is not present" in {
    val propertyName = "project.build.sourceEncoding-DUMMY"

    validPomUut.property(propertyName) should be("")

    validPomUut.removeProperty(propertyName)

    validPomUut.property(propertyName) should be("")
  }

  it should "ensure comments are maintained" in {
    val propertyName = "project.build.sourceEncoding"
    validPomUut.property(propertyName) should not be ""

    validPomUut.content.contains("<!-- Misc Comment for Testing -->") should be(true)

    validPomUut.removeProperty(propertyName)

    assert(validPomUut.dirty === true)

    validPomUut.property(propertyName) should be("")

    validPomUut.content.contains("<!-- Misc Comment for Testing -->") should be(true)
  }

  it should "add a dependency" in {
    val dependencyArtifactId = "atomist-artifact"
    val dependencyGroupId = "com.atomist"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(false)

    validPomUut.addOrReplaceDependency(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)
  }

  it should "add a dependency with scope" in {
    val dependencyArtifactId = "atomist-artifact"
    val dependencyGroupId = "com.atomist"
    val dependencyScope = "compile"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(false)

    validPomUut.addOrReplaceDependencyOfScope(dependencyGroupId, dependencyArtifactId, dependencyScope)

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be("compile")
  }

  it should "add a dependency with version and scope" in {
    val dependencyArtifactId = "atomist-artifact"
    val dependencyGroupId = "com.atomist"
    val dependencyVersion = "0.1.0"
    val dependencyScope = "compile"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(false)

    validPomUut.addOrReplaceDependencyOfVersionAndScope(dependencyGroupId, dependencyArtifactId, dependencyVersion, dependencyScope)

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be("0.1.0")

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be("compile")
  }

  it should "not add a further dependency if the dependency already exists" in {
    val dependencyArtifactId = "spring-boot-starter-web"
    val dependencyGroupId = "org.springframework.boot"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)

    validPomUut.addOrReplaceDependency(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)
  }

  it should "add a dependency's version" in {
    val dependencyArtifactId = "spring-boot-starter-web"
    val dependencyGroupId = "org.springframework.boot"
    val newVersion = "0.0.1"

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be("")

    validPomUut.addOrReplaceDependencyVersion(dependencyGroupId, dependencyArtifactId, newVersion)

    assert(validPomUut.dirty === true)

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be(newVersion)
  }

  it should "replace an existing dependency's version" in {
    val dependencyArtifactId = "testartifact"
    val dependencyGroupId = "testgroup"
    val originalVersion = "0.0.2"
    val newVersion = "0.0.3"

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be(originalVersion)

    validPomUut.addOrReplaceDependencyVersion(dependencyGroupId, dependencyArtifactId, newVersion)

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be(newVersion)
  }

  it should "remove an existing dependency's version" in {
    val dependencyArtifactId = "testartifact"
    val dependencyGroupId = "testgroup"
    val originalVersion = "0.0.2"
    val newVersion = ""

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be(originalVersion)

    validPomUut.removeDependencyVersion(dependencyGroupId, dependencyArtifactId)

    validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) should be(newVersion)
  }

  it should "add a dependency's scope" in {
    val dependencyArtifactId = "spring-boot-starter-web"
    val dependencyGroupId = "org.springframework.boot"
    val newScope = "test"

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be("")

    validPomUut.addOrReplaceDependencyScope(dependencyGroupId, dependencyArtifactId, newScope)

    assert(validPomUut.dirty === true)

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(newScope)
  }

  it should "replace a dependency's existing scope" in {
    val dependencyArtifactId = "spring-boot-starter-test"
    val dependencyGroupId = "org.springframework.boot"
    val originalScope = "test"
    val newScope = "compile"

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(originalScope)

    validPomUut.addOrReplaceDependencyScope(dependencyGroupId, dependencyArtifactId, newScope)

    assert(validPomUut.dirty === true)

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(newScope)
  }

  it should "add a scope to a new dependency: https://github.com/atomist/rug/issues/194" in {
    val dependencyArtifactId = "junit"
    val dependencyGroupId = "org.junit"
    val dependencyVersion = "1.2.3"
    val newScope = "test"

    validPomUut.addOrReplaceDependencyOfVersion(dependencyGroupId, dependencyArtifactId, dependencyVersion)
    validPomUut.addOrReplaceDependencyScope(dependencyGroupId, dependencyArtifactId, newScope)

    assert(validPomUut.dirty)
    assert(validPomUut.dependencyVersion(dependencyGroupId, dependencyArtifactId) == dependencyVersion)
    assert(validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) == newScope)
  }
  it should "remove an existing dependency's existing scope" in {
    val dependencyArtifactId = "spring-boot-starter-test"
    val dependencyGroupId = "org.springframework.boot"
    val originalScope = "test"
    val expectedOutcomeScope = ""

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(originalScope)

    validPomUut.removeDependencyScope(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === true)

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(expectedOutcomeScope)
  }

  it should "sensibly not remove an existing dependency's scope when it doesn't have one" in {
    val dependencyArtifactId = "spring-boot-starter-web"
    val dependencyGroupId = "org.springframework.boot"
    val originalScope = ""
    val expectedOutcomeScope = ""

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(originalScope)

    validPomUut.removeDependencyScope(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === false)

    validPomUut.dependencyScope(dependencyGroupId, dependencyArtifactId) should be(expectedOutcomeScope)
  }

  it should "delete an existing dependency" in {
    val dependencyArtifactId = "spring-boot-starter-web"
    val dependencyGroupId = "org.springframework.boot"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(true)

    validPomUut.removeDependency(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(false)
  }

  it should "sensibly respond at an attempted deletion of a dependency that is not present" in {
    val dependencyArtifactId = "spring-boot-starter-web-DUMMY"
    val dependencyGroupId = "org.springframework.boot"

    validPomUut.isDependencyPresent(dependencyGroupId, dependencyArtifactId) should be(false)

    validPomUut.removeDependency(dependencyGroupId, dependencyArtifactId)

    assert(validPomUut.dirty === false)

    validPomUut.isDependencyPresent(dependencyArtifactId, dependencyArtifactId) should be(false)
  }

  it should "add a new build plugin when one was not present before" in {
    val artifactId = "atomist-build-plugin"
    val groupId = "atomistgroup"
    val plugin = s"""<plugin><groupId>$groupId</groupId><artifactId>$artifactId</artifactId></plugin>"""

    validPomUut.isBuildPluginPresent(groupId, artifactId) should be(false)

    validPomUut.addOrReplaceBuildPlugin(groupId, artifactId, plugin)

    assert(validPomUut.dirty === true)

    validPomUut.isBuildPluginPresent(groupId, artifactId) should be(true)
  }

  it should "replace an existing build plugin" in {
    val artifactId = "git-commit-id-plugin"
    val groupId = "pl.project13.maven"
    val newArtifactId = "atomist-build-plugin"
    val newGroupId = "atomistgroup"
    val plugin = s"""<plugin><groupId>$newGroupId</groupId><artifactId>$newArtifactId</artifactId></plugin>"""

    validPomUut.isBuildPluginPresent(groupId, artifactId) should be(true)
    validPomUut.isBuildPluginPresent(newGroupId, newArtifactId) should be(false)

    validPomUut.addOrReplaceBuildPlugin(groupId, artifactId, plugin)

    assert(validPomUut.dirty === true)

    validPomUut.isBuildPluginPresent(newGroupId, newArtifactId) should be(true)
    validPomUut.isBuildPluginPresent(groupId, artifactId) should be(false)
  }

  it should "be able to test that a dependency management dependency is present when no dependency management section" in {
    val groupId = "org.springframework.cloud"
    val artifactId = "spring-cloud-dependencies"

    validPomUut.isDependencyManagementDependencyPresent(groupId, artifactId) should be(false)

    assert(validPomUut.dirty === false)
  }

  it should "be able to test that a dependency management dependency is present when it is" in {
    val groupId = "org.springframework.cloud"
    val artifactId = "spring-cloud-dependencies"

    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, artifactId) should be(true)

    assert(validPomWithDependencyManagement.dirty === false)
  }

  it should "add a dependency management dependency when no dependency management section is present" in {
    val groupId = "org.springframework.cloud"
    val artifactId = "spring-cloud-dependencies"
    val dependencyContent = s"""<dependency><groupId>$groupId</groupId><artifactId>$artifactId</artifactId><version>Brixton.SR4</version><type>pom</type><scope>import</scope></dependency>"""

    validPomUut.addOrReplaceDependencyManagementDependency(groupId, artifactId, dependencyContent)

    validPomUut.contains("/project/dependencyManagement")

    assert(validPomUut.dirty === true)

    validPomUut.isDependencyManagementDependencyPresent(groupId, artifactId) should be(true)
  }

  it should "replace a dependency management dependency" in {
    val groupId = "org.springframework.cloud"
    val artifactId = "spring-cloud-dependencies"
    val dependencyContent =
      s"""<dependency><groupId>$groupId</groupId><artifactId>$artifactId</artifactId><version>Brixton.SR4</version><type>pom</type><scope>import</scope></dependency>""".stripMargin

    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, artifactId) should be(true)

    validPomWithDependencyManagement.addOrReplaceDependencyManagementDependency(groupId, artifactId, dependencyContent)

    assert(validPomWithDependencyManagement.dirty === true)

    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, artifactId) should be(true)
  }

  it should "not replace a dependency management section if it already exists" in {
    val groupId = "org.springframework.cloud"
    val artifactId = "spring-cloud-dependencies-other"
    val originalArtifactId = "spring-cloud-dependencies"
    val dependencyContent =
      s"""<dependency><groupId>$groupId</groupId><artifactId>$artifactId</artifactId><version>Brixton.SR4</version><type>pom</type><scope>import</scope></dependency>""".stripMargin

    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, originalArtifactId) should be(true)
    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, artifactId) should be(false)

    validPomWithDependencyManagement.addOrReplaceDependencyManagementDependency(groupId, artifactId, dependencyContent)

    assert(validPomWithDependencyManagement.dirty === true)

    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, originalArtifactId) should be(true)
    validPomWithDependencyManagement.isDependencyManagementDependencyPresent(groupId, artifactId) should be(true)
  }

  it should "be able to test that a plugin management plugin is present when no plugin management section" in {
    val groupId = "com.spotify"
    val artifactId = "docker-maven-plugin"

    validPomUut.isBuildPluginManagementPresent(groupId, artifactId) should be(false)

    assert(validPomUut.dirty === false)
  }

  it should "be able to test that a plugin management plugin is present when it is" in {
    val groupId = "com.spotify"
    val artifactId = "docker-maven-plugin"

    validPomWithPluginManagement.isBuildPluginManagementPresent(groupId, artifactId) should be(true)

    assert(validPomWithPluginManagement.dirty === false)
  }

  it should "add a build plugin management plugin" in {
    val groupId = "com.spotify"
    val artifactId = "docker-maven-plugin"
    val plugin = s"""<plugin><groupId>$groupId</groupId><artifactId>$artifactId</artifactId></plugin>"""

    validPomUut.isBuildPluginManagementPresent(groupId, artifactId) should be(false)

    validPomUut.addOrReplaceBuildPluginManagementPlugin(groupId, artifactId, plugin)

    assert(validPomUut.dirty === true)

    validPomUut.isBuildPluginManagementPresent(groupId, artifactId) should be(true)
  }

  it should "be able to test that a profile is present when no profiles section" in {
    val profileId = "docker"

    validPomUut.isProfilePresent(profileId) should be(false)

    assert(validPomUut.dirty === false)
  }

  it should "be able to test that a profule is present when it is" in {
    val profileId = "docker"

    validPomWithProfile.isProfilePresent(profileId) should be(true)

    assert(validPomWithProfile.dirty === false)
  }

  it should "add a profile" in {
    val profileId = "docker"
    val profile = s"""<profile><id>$profileId</id><activation><activeByDefault>true</activeByDefault></activation></profile>"""

    validPomUut.isProfilePresent(profileId) should be(false)

    validPomUut.addOrReplaceProfile(profileId, profile)

    assert(validPomUut.dirty === true)

    validPomUut.isProfilePresent(profileId) should be(true)
  }
}

object PomMutableViewTestSupport {
  val springBootGroupId = "org.springframework.boot"
  val springBootStarterWebArtifactId = "spring-boot-starter-web"
  val springBootStarterTestArtifactId = "spring-boot-starter-test"
  val gitCommitPluginArtifactId = "git-commit-id-plugin"
  val gitCommitPluginGroupId = "pl.project13.maven"
}
