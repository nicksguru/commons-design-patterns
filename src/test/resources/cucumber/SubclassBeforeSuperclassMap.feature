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

  Scenario: Exact key with null value is returned as-is, without superclass scan
    Given a map with the following class entries:
      | class  | value  |
      | Object | object |
    And a null value is stored for Number
    When the closest superclass for Number is found
    Then the found entry should have key Number and a null value

  Scenario: Finding entry for class with no superclass in map
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | String | string |
    When the closest superclass for Boolean is found
    Then no entry should be found

  Scenario: Closest superclass resolution is unchanged for another subclass lookup
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | Object | object |
    When the closest superclass for Long is found
    Then the found entry should have key Number and value "number"

  Scenario: getOrDefault agrees with the locked get
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
      | Object | object |
    When Number is looked up with getOrDefault using default "default"
    Then the getOrDefault result should be "number"

  Scenario: getOrDefault returns the default value for a missing key
    Given a map with the following class entries:
      | class  | value  |
      | Number | number |
    When Boolean is looked up with getOrDefault using default "default"
    Then the getOrDefault result should be "default"

  Scenario: forEach observes the same entries and order as the locked views
    Given a map with the following class entries:
      | class   | value   |
      | Integer | integer |
      | Number  | number  |
      | Object  | object  |
    When the map is consumed with forEach
    Then the forEach-observed entries should be:
      | class   | value   |
      | Integer | integer |
      | Number  | number  |
      | Object  | object  |

  Scenario: putIfAbsent preserves subclass-before-superclass order and doesn't overwrite
    Given a map with the following class entries:
      | class  | value  |
      | Object | object |
    When Integer is put if absent with value "integer"
    Then the putIfAbsent result should be null
    When Integer is put if absent with value "other"
    Then the putIfAbsent result should be "integer"
    And the map keys should be in order:
      | class   |
      | Integer |
      | Object  |

  Scenario: computeIfAbsent computes only missing keys
    Given a map with the following class entries:
      | class  | value  |
      | Object | object |
    When computeIfAbsent is called for Integer computing "integer"
    Then the computeIfAbsent result should be "integer"
    When computeIfAbsent is called for Integer computing "other"
    Then the computeIfAbsent result should be "integer"
    And the map keys should be in order:
      | class   |
      | Integer |
      | Object  |

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

  Scenario: Maps with identical entries are equal with equal hash codes
    Given a map with the following class entries:
      | class   | value   |
      | Integer | integer |
      | Number  | number  |
    And another map with the same entries
    Then the two maps should be equal
    And their hash codes should be equal

  Scenario: Map equals a plain LinkedHashMap with the same entries
    Given a map with the following class entries:
      | class   | value   |
      | Integer | integer |
      | Number  | number  |
    And a plain LinkedHashMap with the same entries
    Then the map should equal the plain map
    And the plain map should equal the map

  Scenario: Maps with different entries are not equal
    Given a map with the following class entries:
      | class   | value   |
      | Integer | integer |
    And another map with different entries
    Then the two maps should not be equal
