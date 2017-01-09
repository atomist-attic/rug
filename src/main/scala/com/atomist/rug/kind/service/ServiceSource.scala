package com.atomist.rug.kind.service

import com.atomist.project.ProjectOperation
import com.atomist.project.review.ReviewResult
import com.atomist.rug.runtime.js.interop.UserServices
import com.atomist.source.ArtifactSource

/**
  * Returns a group of services we can act on
  */
trait ServiceSource extends UserServices {

  def services: Seq[Service]

  def messageBuilder: MessageBuilder

  def projectOperations: Seq[ProjectOperation] = Nil

}

/**
  * Represents a service
  *
  * @param project project artifacts (source) for service
  * @param updatePersister Object that knows how to persist any changes artifacts to the backing store: For example,
  * GitHub or a local filesystem
  * @param reviewOutputPolicy policy for routing the output of ProjectReviewers. Should use issueRouter and userMessageRouter
  * @param issueRouter low-level support for raising issues. May raise an issue in an issue tracker or notify the user
  * via other means
  * @param messageBuilder enables us to send messages
  */
case class Service(
                    project: ArtifactSource,
                    updatePersister: UpdatePersister,
                    reviewOutputPolicy: ReviewOutputPolicy = IssueRaisingReviewOutputPolicy,
                    issueRouter: IssueRouter = ConsoleIssueRouter,
                    messageBuilder: MessageBuilder
                  ) {

  /**
    * Return a name that must be unique among present services. Default implementation
    * takes name from ArtifactSource: subclasses can override this if they wish
    * to do something different
    *
    * @return unique name among current services
    */
  def name: String = project.id.name

  def update(newContent: ArtifactSource, updateIdentifier: String): Unit = {
    updatePersister.update(this, newContent, updateIdentifier)
  }
}

/**
  * Implemented by objects that know how to update a service's representation:
  * For example, in a local file system or on GitHub.
  */
trait UpdatePersister {

  def update(service: Service, newContent: ArtifactSource, updateIdentifier: String)
}

trait ReviewOutputPolicy {

  def route(service: Service, rr: ReviewResult)
}

object IssueRaisingReviewOutputPolicy extends ReviewOutputPolicy {

  override def route(service: Service, rr: ReviewResult): Unit = {
    for (rc <- rr.comments) {
      service.issueRouter.raiseIssue(service, Issue(s"${rc.severity}: ${rc.comment}"))
    }
  }
}

trait IssueRouter {

  def raiseIssue(service: Service, issue: Issue): Unit
}

case class Issue(
                name: String
                )

object ConsoleIssueRouter extends IssueRouter {

  override def raiseIssue(service: Service, issue: Issue): Unit = {
    println(s"Raising issue [${issue.name}] against ${service.project.id}")
  }
}
