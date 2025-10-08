@design-patterns #@disabled
Feature: Pipeline processing
  Process data through a series of steps

  Scenario: Pipeline executes all steps in order
    Given a pipeline with the following steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    When the pipeline is executed with input "test-input"
    Then the pipeline output should be "Third"
    And the pipeline should have executed 3 steps
    And the step durations should be recorded
    And the steps should have been executed in order:
      | stepName   |
      | FirstStep  |
      | SecondStep |
      | ThirdStep  |

  Scenario: Pipeline with custom step runner
    Given a pipeline with the following steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    And a custom step runner that logs step execution
    When the pipeline is executed with input "test-input"
    Then the pipeline output should be "Third"
    And each step should have been logged

  Scenario: Pipeline that stops early
    Given a pipeline with the following steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    And the pipeline is configured to stop after the second step
    When the pipeline is executed with input "test-input"
    Then the pipeline output should be "Second"
    And the pipeline should have executed 2 steps

  Scenario: Pipeline with exception in step
    Given a pipeline with a step that throws an exception
    When the pipeline is executed with input "test-input"
    Then the exception message should contain "Step failed"

  Scenario: Pipeline toString method
    Given a pipeline with the following steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    When the pipeline toString method is called
    Then the result should contain all step names

  Scenario: Pipeline with custom iterator
    Given a pipeline with custom iterator that reverses steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    When the pipeline is executed with input "test-input"
    Then the pipeline output should be "First"
    And the steps should have been executed in order:
      | stepName   |
      | ThirdStep  |
      | SecondStep |
      | FirstStep  |

  Scenario: Pipeline with null input
    Given a pipeline with the following steps:
      | stepName   | outputValue |
      | FirstStep  | First       |
      | SecondStep | Second      |
      | ThirdStep  | Third       |
    When the pipeline is executed with null input
    Then the pipeline output should be "Third"
    And the pipeline input should be null
