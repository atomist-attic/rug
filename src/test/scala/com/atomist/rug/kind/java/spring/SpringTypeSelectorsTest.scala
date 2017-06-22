package com.atomist.rug.kind.java.spring

import com.atomist.rug.kind.java.GitHubJavaParserExtractor
import com.atomist.source.{FileArtifact, StringFileArtifact}
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class SpringTypeSelectorsTest extends FlatSpec with Matchers {

  import SpringTypeSelectors._

  it should "identify Spring Boot application class" in {
    val facu = GitHubJavaParserExtractor(
      Seq(StringFileArtifact("Test.java", "@SpringBootApplication public class Test {}").asInstanceOf[FileArtifact]).asJava
    )
    val coit = facu.head.compilationUnit.getTypes.asScala.head.asInstanceOf[ClassOrInterfaceDeclaration]
    SpringBootApplicationClassSelector(coit) shouldBe true
  }

  it should "reject non Spring Boot application class" in {
    val facu = GitHubJavaParserExtractor(
      Seq(StringFileArtifact("Test.java", "public class Test {}").asInstanceOf[FileArtifact]).asJava
    )
    val coit = facu.head.compilationUnit.getTypes.asScala.head.asInstanceOf[ClassOrInterfaceDeclaration]
    SpringBootApplicationClassSelector(coit) shouldBe false
  }
}
