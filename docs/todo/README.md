# Active TODOs

This directory holds project-level follow-up work. Source-local TODO comments
remain next to the code they describe. Completed work is removed from this list;
superseded AI handoff prompts are retained only in the [archive](archive/).

## Tracking diagnostics

- Improve the tracking diagnostics testbench output so failures are easier to
  connect to the originating Chisel source.
- Make tracking, logger, and requirement failure messages follow a consistent
  shape while preserving the information specific to each subsystem.

## Elaboration test migration

- Convert the composite-boundary and buffered-naming HDL generators into report-oriented
  elaboration tests, then remove their tracked generated artifacts. See the
  [elaboration-test migration checklist](elaboration-test-migration.md).

## Elastic tracking type coupling

- Consider removing `tpe` from `chext.elastic.tracking.Tracked`. The Elastic
  module graph is deliberately coupled to `chext.elastic.Interface`, so the
  interface type can be derived and serialized directly instead of making
  `Tracked` appear more generic than its closed-world use.
- Audit the same artificial genericity in related consumers, including the
  C++-side deadlock-detection code, and make the Elastic coupling explicit where
  it is inherent in the model.

## C++ deadlock detector migration

- Port the Arteris Interview C++ tracking loader and deadlock detector to the
  unified component hierarchy using the detailed
  [migration checklist](cpp-deadlock-unified-components.md).

## Requirement checks

- Preserve important `require`/`Require` invariants during refactors. Audit
  touched construction and connection APIs for checks that should remain or be
  made clearer rather than silently weakening them.

## AXI4 DataView tracking identity

- Make a raw AXI4 interface and its `.asFull`/`.asLite` DataView share one canonical tracking
  identity. Their properties and resolver registrations currently belong to distinct `Tracked`
  Scala objects, so metadata attached to the raw interface does not follow the view.

## Design investigations

The remaining pipelining and connection-API questions are collected in
[design-issues.md](design-issues.md).

The developing AXI4 interface-property and resolver model is recorded in
[axi4-tracking.md](axi4-tracking.md).

The component-by-component AXI4-Full resolver and shape-property flow is
specified in [axi4-resolvers.md](axi4-resolvers.md).

## Archived prompts

- [Tracking refactor prompt](archive/tracking-refactor-prompt.md)
- [Elastic interface and wire naming prompt](archive/wire-naming-prompt.md)
