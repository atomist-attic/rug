package com.atomist.rug.kind.java.support

import com.atomist.rug.kind.java.GitHubJavaParserExtractor
import com.atomist.source.{ArtifactSource, FileArtifact, StringFileArtifact}

import scala.collection.JavaConverters._

/**
  * Utility methods to find packages in Java files
  */
object PackageFinder {

  def findPackage(src: String): String = {
    val cu = GitHubJavaParserExtractor(Seq(StringFileArtifact("Src.java", src).asInstanceOf[FileArtifact]).asJava)
      .head.compilationUnit
    if (!cu.getPackageDeclaration.isPresent) ""
    else cu.getPackageDeclaration.get.getNameAsString
  }

  /**
    * Return all packages found in this ArtifactSource.
    *
    * @param as ArtifactSource
    * @return all identified packages
    */
  def packages(as: ArtifactSource): Seq[PackageInfo] = {
    as.allFiles.filter(_.name.endsWith(".java"))
      .map(f => (findPackage(f.content), f))
      .groupBy(_._1)
      .map {
        case (pkg, files) => PackageInfo(pkg, files.size)
      }.toSeq
  }
}

/**
  * Information about a package.
  *
  * @param name FQN of the package
  * @param files number of Java files in the package
  */
case class PackageInfo(name: String, files: Int) {

  def isChild(pi: PackageInfo): Boolean = pi.name.contains(this.name)
}