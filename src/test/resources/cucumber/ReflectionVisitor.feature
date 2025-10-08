@design-patterns #@disabled
Feature: ReflectionVisitor Pattern
  The ReflectionVisitor pattern should allow dynamic method selection based on the type of object being visited

  Scenario: Basic visitor functionality
    Given a reflection visitor is created with methods for different types
    When an object is visited
    Then the appropriate visitor method should be called
    And the result should be returned as an Optional

  Scenario: Visitor with inheritance hierarchy
    Given a reflection visitor is created with methods for a class hierarchy
    When a subclass object is visited
    Then the most specific visitor method should be called

  Scenario: Null object handling
    Given a reflection visitor is created with methods for different types
    When a null object is visited
    Then an empty Optional should be returned

  Scenario: Visitor method throwing exception
    Given a reflection visitor is created with a method that throws an exception
    When an object that triggers the exception is visited
    Then IllegalStateException should be thrown

  Scenario: Visitor with duplicate method signatures
    Given a reflection visitor is created with duplicate method signatures
    Then the exception message should contain "must be 'visit'"

  Scenario: Visitor with invalid method signature
    Given a reflection visitor is created with an invalid method signature
    Then the exception message should contain "must be Optional"

  Scenario: Visitor with multiple visitable types
    Given a reflection visitor is created with methods for different types
    When objects of different types are visited
    Then each object should be handled by the appropriate visitor method
