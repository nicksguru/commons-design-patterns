@design-patterns #@disabled
Feature: NestedBuilder pattern functionality
  Nested builder pattern allows building complex objects with hierarchical structure

  Scenario Outline: NestedBuilder builds object and returns to parent
    Given a parent builder with name "<parentName>"
    And a nested builder with name "<nestedName>" and value "<nestedValue>"
    When the nested builder builds the object
    And the nested builder returns to parent using and method
    Then the nested object should have name "<nestedName>" and value "<nestedValue>"
    And the parent builder should be returned
    And no exception should be thrown
    Examples:
      | parentName | nestedName | nestedValue |
      | MainObject | ChildOne   | ValueOne    |
      | RootItem   | SubItem    | TestValue   |
      | Container  | Element    | Data        |

  Scenario: NestedBuilder with null parent throws exception
    Given a null parent builder
    When creating a nested builder with the null parent
    Then NullPointerException should be thrown

  Scenario: NestedBuilder abstract methods work correctly
    Given a concrete nested builder implementation
    When the build method is called
    Then the concrete object should be built
    And the and method should return the parent builder
    And no exception should be thrown

  Scenario Outline: Multiple nested builders in chain
    Given a parent builder with name "<parentName>"
    And multiple nested builders are created:
      | name        | value       |
      | FirstChild  | FirstValue  |
      | SecondChild | SecondValue |
      | ThirdChild  | ThirdValue  |
    When each nested builder builds its object and returns to parent
    Then all nested objects should be built correctly
    And the final parent builder should be returned
    And no exception should be thrown
    Examples:
      | parentName |
      | MainParent |
      | RootParent |

  Scenario: NestedBuilder inheritance hierarchy
    Given a specialized nested builder that extends the base nested builder
    When the specialized builder is used to build an object
    Then the specialized functionality should work correctly
    And the parent builder should still be accessible
    And no exception should be thrown
