@design-patterns #@disabled
Feature: PipelineStep advanced functionality
  As a developer
  I want to understand the advanced behavior of PipelineStep
  So that I can implement complex pipeline steps correctly

  Scenario: Pipeline step with multiple features with the same name should combine values
    Given a pipeline step with duplicate feature names
    When I call getFeaturesValues on the pipeline step
    Then the result should contain combined feature values

  Scenario: Pipeline step with iterable feature values should handle them correctly
    Given a pipeline step with iterable feature values
    When I call getFeaturesValues on the pipeline step
    Then the result should contain all iterable values

  Scenario: Pipeline step with complex object feature values should use toString
    Given a pipeline step with complex object feature values
    When I call toString on the pipeline step
    Then the result should contain the object's toString representation

  Scenario: Pipeline step with changing feature values should not use cached values
    Given a pipeline step with changing feature values
    When I call getFeaturesValues multiple times
    Then the same map instance should not be returned each time
    And the feature values should not reflect the initial state

  Scenario: Pipeline step with empty features should not include braces in toString
    Given a pipeline step with no features
    When I call toString on the pipeline step
    Then the result should not contain "{}"

  Scenario: Pipeline step with TreeMap features should maintain sorted order
    Given a pipeline step with multiple alphabetical features:
      | name   | value |
      | zebra  | z     |
      | apple  | a     |
      | monkey | m     |
    When I call toString on the pipeline step
    Then the features should appear in alphabetical order
