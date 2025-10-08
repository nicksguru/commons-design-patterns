@design-patterns #@disabled
Feature: SubclassBeforeSuperclassMap
  The SubclassBeforeSuperclassMap should maintain subclasses before superclasses
  So that class hierarchy is properly represented in the map order

  Background:
    Given an empty SubclassBeforeSuperclassMap

  Scenario: Map maintains subclass before superclass order
    When classes are added in the order: Number, String, Object, Integer
    Then the map keys should be in the order: String, Integer, Number, Object

  Scenario: Map maintains order when adding classes in reverse hierarchy
    When classes are added in the order: Object, String, Number, Integer
    Then the map keys should be in the order: String, Integer, Number, Object

  Scenario: Finding closest superclass
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | Object | object |
      | String | string |
    When the closest superclass for Integer is found
    Then the found entry should have key Number and value "number"

  Scenario: Finding direct class match
    Given a map with the following class entries:
      | class   | value   |
      | Integer | integer |
      | Number  | number  |
      | Object  | object  |
    When the closest superclass for Integer is found
    Then the found entry should have key Integer and value "integer"

  Scenario: Finding entry for class with no superclass in map
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | String | string |
    When the closest superclass for Boolean is found
    Then no entry should be found

  Scenario: Thread-safe operations
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | Object | object |
    When multiple threads simultaneously add and read from the map
    Then no concurrency exceptions should occur
    And the map should maintain its integrity

  Scenario: Thread-safe operations
    When multiple threads concurrently put and get values
    Then map should maintain thread-safe consistency
