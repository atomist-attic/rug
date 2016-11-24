package com.atomist.rug.kind.java.spring

import com.atomist.rug.kind.java.GitHubJavaParserExtractor
import com.atomist.source.StringFileArtifact
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

class SpringTypeSelectorsTest extends FlatSpec with Matchers {

  import SpringTypeSelectors._

  it should "identify Spring Boot application class" in {
    val facu = GitHubJavaParserExtractor(Seq(StringFileArtifact("Test.java", "@SpringBootApplication public class Test {}")))
    val coit = facu.head.compilationUnit.getTypes.head.asInstanceOf[ClassOrInterfaceDeclaration]
    SpringBootApplicationClassSelector(coit) should be(true)
  }

  it should "reject non Spring Boot application class" in {
    val facu = GitHubJavaParserExtractor(Seq(StringFileArtifact("Test.java", "public class Test {}")))
    val coit = facu.head.compilationUnit.getTypes.head.asInstanceOf[ClassOrInterfaceDeclaration]
    SpringBootApplicationClassSelector(coit) should be(false)
  }
}
