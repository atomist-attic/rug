Feature: Real edit
  Test editing a real project

  Scenario: Edit a real project
    Given github atomist/rug/master
    Then file at src/main/scala/com/atomist/graph/GraphNode.scala should exist

