@design-patterns #@disabled
Feature: ThreadSafeListIterator

  Background:
    Given list to iterate:
      | a |
      | b |
      | c |
      | d |

  Scenario: Iteration from index 0
    Given iteration start index is 0
    When iteration finishes
    Then iteration result should be the original list

  Scenario: Iteration from index 1
    Given iteration start index is 1
    When iteration finishes
    Then iteration result should be:
      | b |
      | c |
      | d |
      | a |

  Scenario: Iteration from index 2
    Given iteration start index is 2
    When iteration finishes
    Then iteration result should be:
      | c |
      | d |
      | a |
      | b |

  Scenario: Iteration from index 3
    Given iteration start index is 3
    When iteration finishes
    Then iteration result should be:
      | d |
      | a |
      | b |
      | c |

  Scenario Outline: Iteration from a non-existing index falls back to index 0
    Given iteration start index is <index>
    When iteration finishes
    Then iteration result should be the original list
    Examples:
      | index |
      | 4     |
      | -1    |
      | 1000  |

  Scenario Outline: Iteration of a single-element list, from any index, produces the original list
    Given list to iterate:
      | a |
    And iteration start index is <index>
    When iteration finishes
    Then iteration result should be the original list
    Examples:
      | index |
      | 0     |
      | 1     |
      | -1    |
      | 1000  |

  Scenario: Iterating an empty list
    Given list to iterate:
      |  |
    And iteration start index is 0
    When iteration finishes
    Then iteration result should be:
      |  |
