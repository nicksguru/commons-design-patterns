@design-patterns #@disabled
Feature: Pipeline with and without phases

  Scenario Outline: Process pipeline having no phases
    Given pipeline input is "<Input>"
    And first pipeline step appends "<Suffix 1>"
    And second pipeline step appends "<Suffix 2>"
    And third pipeline step appends "<Suffix 3>"
    When pipeline is run
    Then pipeline output should be "<Output>"
    And pipeline as string should contain "{dummyFeature=test, testFeature=yes}"
    Examples:
      | Input  | Suffix 1 | Suffix 2 | Suffix 3 | Output  |
      | red    | 1        | 2        | 3        | red123  |
      | green  |          | 2        | 3        | green23 |
      | blue   |          |          | 3        | blue3   |
      | orange |          |          |          | orange  |
      | purple |          | 2        |          | purple2 |
      | cyan   | 1        |          | 3        | cyan13  |

  Scenario Outline: Process pipeline having phases
    Given pipeline input is "<Input>"
    And first pipeline step appends "<Suffix 1>"
    And first pipeline step phase is "<Phase 1>"
    And second pipeline step appends "<Suffix 2>"
    And second pipeline step phase is "<Phase 2>"
    And third pipeline step appends "<Suffix 3>"
    And third pipeline step phase is "<Phase 3>"
    When pipeline is run
    Then pipeline output should be "<Output>"
    Examples:
      | Input | Suffix 1 | Phase 1 | Suffix 2 | Phase 2 | Suffix 3 | Phase 3 | Output  | Comment                 |
      | test  | 1        | PHASE1  | 2        | PHASE2  | 3        | PHASE3  | test123 |                         |
      | test  | 1        | PHASE3  | 2        | PHASE2  | 3        | PHASE1  | test321 |                         |
      | test  | 1        | PHASE2  | 2        | PHASE1  | 3        | PHASE3  | test213 |                         |
      | test  | 1        |         | 2        |         | 3        |         | test123 | No phases               |
      | test  | 1        |         | 2        | PHASE2  | 3        | PHASE3  | test231 | No phase for step 1     |
      | test  | 1        | PHASE3  | 2        |         | 3        | PHASE1  | test312 | No phase for step 2     |
      | test  | 1        | PHASE2  | 2        | PHASE1  | 3        |         | test213 | No phase for step 3     |
      | test  | 1        |         | 2        | PHASE2  | 3        |         | test213 | No phase for steps 1, 3 |
      | test  | 1        |         | 2        |         | 3        | PHASE2  | test312 | No phase for steps 1, 2 |
      | test  | 1        | PHASE3  | 2        |         | 3        |         | test123 | No phase for steps 2, 3 |
