## ADDED Requirements

### Requirement: OpenSpec is the living behavior contract

The repository MUST keep current system behavior in `openspec/specs/<capability>/spec.md`. Schema MUST be `spec-driven-with-adr`.

#### Scenario: Agent reads current behavior
- GIVEN a clone of `Heavy-Rental/heavy-rental-mobile`
- WHEN an agent needs the current documentation or CI contract
- THEN it reads `openspec/specs/` and `openspec/config.yaml`

### Requirement: Changes follow proposal-specs-design-adr-tasks

Every behavioral or architectural change MUST produce OpenSpec artifacts in order `proposal → specs → design → adr → tasks`. Durable ADRs MUST live under `adr/`.

#### Scenario: New capability change
- GIVEN ADR-0001
- WHEN a change alters screens, HTTP usage, mock strategy, or CI callers
- THEN `openspec/changes/<slug>/` contains the five artifacts

### Requirement: OpenSPDD REASONS Canvas is the implementation contract

Implementation work that is more than a one-line comment fix MUST have a REASONS Canvas at `spdd/prompt/`.

#### Scenario: Docs alignment change
- GIVEN HR-242
- WHEN an agent updates the documentation stack
- THEN it follows the in-force canvas in `spdd/README.md`

### Requirement: specification/ remains mapped product SDD

`specification/` MUST remain detailed product SDD and MUST be indexed by `product-features`.

#### Scenario: Backend fact drifts
- GIVEN a product file disagrees with Spring OpenSpec
- WHEN the conflict is noticed
- THEN both the product file and OpenSpec are updated in the same change
