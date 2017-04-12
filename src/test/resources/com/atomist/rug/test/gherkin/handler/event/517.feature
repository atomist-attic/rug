Feature: Kubernetes Pods Lifecycle
  In order for Atomist to be able to react to Kubernetes Pod conditions
  As an Atomist user
  I want to be notified when a pod has been deployed or when it has entered a crash loop

  Scenario: Container image pulled
    Given pod container image pulled handler registered
    When a container image was pulled
    Then the handler is triggered
    Then we should receive a message

