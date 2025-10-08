@design-patterns #@disabled
Feature: ChainOfResponsibility
  Test chain behavior under different circumstances

  Scenario Outline: Process chain
    Given input is "<Input>"
    And first chain step returns "<Result of Step 1>"
    And second chain step returns "<Result of Step 2>"
    And third chain step returns "<Result of Step 3>"
    When chain is run
    Then output should be "<Output>"
    Examples:
      | Input    | Result of Step 1 | Result of Step 2 | Result of Step 3 | Output | Comment                     |
      | whatever | test1            | test2            | test3            | test1  | Success from first step     |
      | whatever |                  | test2            | test3            | test2  | Success from second step    |
      | whatever |                  |                  | test3            | test3  | Success from third step     |
      | whatever |                  |                  |                  |        | No step took responsibility |
