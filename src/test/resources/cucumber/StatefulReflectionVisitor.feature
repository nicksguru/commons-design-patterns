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
    Then the exception message should contain "object to visit must not be null"

  Scenario: Visitor throws exception for invalid visitor method
    Given a visitor with invalid visitor method is created
    Then the exception message should contain "must accept state"

  Scenario Outline: Visitor processes different types of objects
    When a visitable object of type "<type>" is visited
    Then the visitor should process the object
    And the state should contain the value "<value>"
    Examples:
      | type             | value             |
      | SimpleVisitable  | simple-processed  |
      | ChildVisitable   | child-processed   |
      | AnotherVisitable | another-processed |
