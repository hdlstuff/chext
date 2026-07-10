# Active Design Issues

These are unresolved design investigations extracted from the former general
issues document. Naming questions and API work already reflected in the current
implementation and [naming guide](../naming.md) are intentionally omitted.

## Pipelining

### Elastic components

Investigate pipelined forms of `elastic.Demux` and `elastic.Arbiter`, informed by
the corresponding FlexiMem implementations. A plain `elastic.Mux` has
centralized control and is not naturally pipelinable; `elastic.Distributor` is
expected to become obsolete, while `elastic.Replicate` and `elastic.Transform`
do not currently need pipelined variants.

### AXI components

Many AXI components depend on `elastic.Mux`. An AXI mux can potentially be
structured as a tree with AXI buffers between levels. A pipelined AXI demux also
needs a scalable address decoder: each tree node must recognize the union of
the address spaces downstream without creating an unnecessarily large logic
function. Investigate a helper representation for hierarchical address-space
encoding.

## Remaining `<>` connections

Audit remaining uses of Chisel's `<>` operator and migrate protocol-level
connections to the canonical Chext connection APIs where those APIs express the
intended direction and semantics. Do not mechanically replace aggregate
connections for which `<>` remains the correct Chisel operation; start with
locations that already carry connection-cleanup TODOs, particularly memory and
bridge code.
