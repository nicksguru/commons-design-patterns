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
    Then the exception should be of type "RuntimeException"
    And the exception message should contain "Test exception"

  Scenario: Visitor method throwing a checked exception
    Given a reflection visitor is created with a method that throws a checked exception
    When an object that triggers the checked exception is visited
    Then the exception should be of type "Exception"
    And the exception message should contain "Checked test exception"

  Scenario: Un-annotated override of an annotated visitor method is invoked
    Given a reflection visitor overrides an annotated visitor method without re-annotating it
    When an object is visited
    Then the overridden visitor method should be invoked

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

  Scenario: Visitor dispatches to the first registered method for unrelated ancestor types
    Given a reflection visitor registers visit(UnrelatedBase) before visit(UnrelatedMarker)
    When an UnrelatedChild object is visited
    Then the result should be "Visited UnrelatedBase"

  Scenario: Visitor dispatches to the first registered method for unrelated ancestor types in reverse order
    Given a reflection visitor registers visit(UnrelatedMarker) before visit(UnrelatedBase)
    When an UnrelatedChild object is visited
    Then the result should be "Visited UnrelatedMarker"
