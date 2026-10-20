# chext fixes

## RRChooser starves inputs when the input count is not a power of two

File: `src/main/scala/chext/elastic/Chooser.scala` (`class RRChooser`).

**Bug.** The round-robin choice wrapped back to input 0 only when `lastChoice` was all ones:

```scala
private val rrChoice = Mux(lastChoice === choiceMax, zeroChoice,
                           PriorityEncoder(v.zipWithIndex.map { case (x, i) => i.U > lastChoice && x }))
```

- **Power-of-two counts are fine.** With n inputs, n a power of two, the last index is all ones, so the wrap fires.
- **Other counts break.** For n = 3, 5, 7, 12, 13 and so on, the last index (n - 1) is never all ones. Once input
  n - 1 wins, no higher input exists. `PriorityEncoder` of an all-false vector returns n - 1, so input n - 1 wins
  again, for as long as it stays valid, and every other input starves.

**Observed effect.** In HardCilk designs generated for the CilkWeave evaluation, 12- and 13-input muxes appeared.
In a QuestaSim run of pageRank_overlap, 11 of 12 PEs froze after 189 tasks each (`ARVALID=1` / `ARREADY=0` on
their muxed memory port), and the last PE did the rest of the work alone.

**Fix.** Pick the next valid input above the last winner; if there is none, wrap to the lowest valid input:

```scala
private val priorityChoice = PriorityEncoder(v)
private val higher = VecInit(v.zipWithIndex.map { case (x, i) => i.U > lastChoice && x })
private val rrChoice = Mux(higher.asUInt.orR, PriorityEncoder(higher), priorityChoice)
```

- `priorityChoice` is declared before `rrChoice`, because Scala initialises vals in order.
- The unused `choiceMax` is removed.
- For power-of-two input counts the arbitration is unchanged in effect: still round robin.

**Validated.**
- All CilkWeave designs regenerated with the fix are bit-exact in QuestaSim and on the Alveo V80 card.
- In the generated Verilog, only the arbiters changed. For example, a 3-input arbiter that wrapped only at
  `lastChoice == 2'h3`, which never happens, now wraps when no higher input is valid.

**Release.** The fix is currently published locally under the existing version 0.1.1. Please release it as a new
version (e.g. 0.1.2), so that downstream builds (HardCilk) pick it up explicitly instead of depending on which
0.1.1 is in the local ivy cache.
