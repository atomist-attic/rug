package com.atomist.project

import com.atomist.source.ArtifactSource

/**
  * Extended by classes that can check whether something is true
  * about a given project.
  */
trait ProjectAssertion extends ((ArtifactSource) => Boolean) {
}

class FunctionProjectAssertion(f: ArtifactSource => Boolean) extends ProjectAssertion {

  override def apply(project: ArtifactSource): Boolean = f(project)
}

/**
  * Operations on ProjectionAssertions
  * Usable by Java clients who can't benefit from operator overloading.
  * Import its members into Scala files to allow transparent use of logical ops.
  */
object ProjectAssertionOps {

  def and(a: ProjectAssertion, b: ProjectAssertion): ProjectAssertion =
    new FunctionProjectAssertion(p => a(p) && b(p))

  def or(a: ProjectAssertion, b: ProjectAssertion): ProjectAssertion =
    new FunctionProjectAssertion(p => a(p) || b(p))

  def not(a: ProjectAssertion): ProjectAssertion =
    new FunctionProjectAssertion(p => !a(p))

  def allOf(as: ProjectAssertion*): ProjectAssertion =
    new FunctionProjectAssertion(p => as.forall(a => a(p)))

  def anyOf(as: ProjectAssertion*): ProjectAssertion =
    new FunctionProjectAssertion(p => as.exists(a => a(p)))

  implicit class LogicalOpsProjectAssertion(pa: ProjectAssertion) extends ProjectAssertion {

    def and(that: ProjectAssertion): ProjectAssertion = ProjectAssertionOps.and(pa, that)

    def &&(that: ProjectAssertion): ProjectAssertion = and(that)

    def or(that: ProjectAssertion): ProjectAssertion = ProjectAssertionOps.or(pa, that)

    def ||(that: ProjectAssertion): ProjectAssertion = or(that)

    def not(): ProjectAssertion = ProjectAssertionOps.not(pa)

    def unary_!(): ProjectAssertion = not()

    override def apply(project: ArtifactSource): Boolean = pa(project)

    def ?(project: ArtifactSource) = apply(project)
  }
}
