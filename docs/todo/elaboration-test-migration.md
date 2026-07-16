# Elaboration Test Migration

Convert the following HDL-generating testbench applications into report-oriented elaboration tests
using `chext.util.ElaborationTest`:

- `chext.elastic.CompositeBoundary_Tb` in
  `src/test/scala/chext/elastic/CompositeBoundary.tb.scala`;
- `chext.elastic.BufferedNaming_Tb`, `BufferedNamingSeq_Tb`, and
  `SourceBufferedManyNaming_Tb` in `src/test/scala/chext/elastic/BufferedNaming.tb.scala`;
- `chext.amba.axi4.RawBufferedManyNaming_Tb`, `FullBufferedManyNaming_Tb`, and
  `LiteBufferedManyNaming_Tb` in `src/test/scala/chext/amba/axi4/BufferNaming.tb.scala`;
- `chext.memory.BufferedManyNaming_Tb` in `src/test/scala/chext/memory/Buffer.tb.scala`.

The corresponding tracked SystemVerilog and HDL-info fixtures have been removed from `sysc_tb`, and
the generators are no longer part of `scripts/run_tb_objects.sh`. Each migration must use the
removed fixtures from Git history to reconstruct explicit checks for the names they captured. Once
those checks pass, remove or replace the old `chext.TestBench` application.
