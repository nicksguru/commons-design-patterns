@design-patterns #@disabled
Feature: Stateful Reflection Visitor Pattern
  The Stateful Reflection Visitor pattern allows objects to be visited
  with a state object that is passed to each visitor method.

  Background:
    Given a stateful reflection visitor is created

  Scenario: Visitor finds and applies the correct visitor method
    When a visitable object of type "SimpleVisitable" is visited
    Then the visitor should process the object
    And the state should be updated

  Scenario: Visitor handles inheritance hierarchy correctly
    When a visitable object of type "ChildVisitable" is visited
    Then the visitor should process the object using the most specific visitor method
    And the state should be updated

  Scenario: Visitor returns empty optional for null input
    When a null object is visited statefully
    Then an empty stateful Optional should be returned

  Scenario: Visitor throws exception for invalid visitor method
    Given a visitor with invalid visitor method is created
    Then the exception message should contain "must accept state"

  Scenario: Visitor method exception propagates unchanged
    Given a stateful reflection visitor is created with a method that throws an exception
    When an object that triggers the stateful visitor exception is visited
    Then the exception should be of type "IllegalStateException"
    And the exception message should contain "Stateful test exception"

  Scenario: Un-annotated override of an annotated visitor method is invoked
    Given a stateful reflection visitor overrides an annotated visitor method without re-annotating it
    When a visitable object of type "SimpleVisitable" is visited
    Then the stateful overridden visitor method should be invoked

  Scenario Outline: Visitor processes different types of objects
    When a visitable object of type "<type>" is visited
    Then the visitor should process the object
    And the state should contain the value "<value>"
    Examples:
      | type             | value             |
      | SimpleVisitable  | simple-processed  |
      | ChildVisitable   | child-processed   |
      | AnotherVisitable | another-processed |
