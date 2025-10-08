@design-patterns #@disabled
Feature: ThreadSafeInfiniteIterator with concurrency
  ThreadSafeInfiniteIterator should be used
  So that items from an Iterable can be cycled through infinitely in a thread-safe manner

  Scenario: Iterator should handle concurrent reads and resets
    Given a thread-safe infinite iterator with a dynamic source
    When multiple threads read and modify the iterator concurrently
    Then all operations should complete without exceptions
    And the iterator should remain in a consistent state

  Scenario: Iterator should handle source modification during iteration
    Given a thread-safe infinite iterator with a modifiable source
    When the source is modified during iteration
    Then the iterator should reflect the changes after reset

  Scenario: Iterator should handle high concurrency load
    Given a thread-safe infinite iterator with items:
      | value |
      | A     |
      | B     |
      | C     |
    When high concurrency load is applied
    Then no exception should be thrown
    And all threads should receive valid items

  Scenario: Iterator should handle concurrent hasNext and next calls
    Given a thread-safe infinite iterator with items:
      | value |
      | A     |
      | B     |
      | C     |
    When 'hasNext' and 'next' are called concurrently
    Then all operations should complete without exceptions
    And all retrieved items should be valid
