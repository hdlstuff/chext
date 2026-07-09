<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->
# Chext Synopsis

This is a working synopsis of the main Chext constructions, their intended use, and how they appear in the tracking/module graph. For the underlying component model, see [tracking.md](tracking.md). For elastic-specific tracking and AXI4 DataView recovery, see [tracking-elastic.md](tracking-elastic.md).

## Contents

- [Elastic Imports](#elastic-imports)
- [Elastic Interfaces and Operators](#elastic-interfaces-and-operators)
- [Elastic Components](#elastic-components)
- [Elastic Containers](#elastic-containers)
- [AXI4 Imports](#axi4-imports)
- [AXI4 Interfaces and Connects](#axi4-interfaces-and-connects)
- [AXI4 Buffers](#axi4-buffers)
- [AXI4 Full Components](#axi4-full-components)
- [AXI4 Lite Components](#axi4-lite-components)
- [Memory](#memory)
- [Stream](#stream)
- [Load/Store](#loadstore)
- [Float](#float)

<span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span>
<span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span>
<span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span>
<span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span>
<span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span>
<span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span>
<span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix</span>
<span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe</span>
<span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix</span>
<span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span>
<span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span>
<span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span>

<code>tpe</code> is the graph type. <code>namePrefix</code> is the construction's conceptual default prefix. Actual paths come from Chisel naming, <code>prefix(&hellip;)</code>, and <code>uniquePrefix(&hellip;)</code>.

## Elastic Imports

Elastic examples assume:

```scala
import chext.{elastic => e}
import e.ConnectOp._
```

## Elastic Interfaces and Operators

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-elastic-interfaces-001">
      <td style="vertical-align:top;"><code>e.Interface(gen)</code> <a href="#entry-elastic-interfaces-001" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">For Chisel types:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val genElastic = e.Interface(UInt(32.W))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Not a component. When materialized as hardware and tracked, appears as <code>chext.elastic.Interface[&hellip;]</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-002">
      <td style="vertical-align:top;"><code>e.Source(gen)</code> <a href="#entry-elastic-interfaces-002" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Producer endpoint from the module's perspective:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val source = IO(e.Source(UInt(32.W)))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sources</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-003">
      <td style="vertical-align:top;"><code>e.Sink(gen)</code> <a href="#entry-elastic-interfaces-003" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Consumer endpoint from the module's perspective:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sink = IO(e.Sink(UInt(32.W)))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sinks</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-004">
      <td style="vertical-align:top;"><code>e.EWire(gen)</code> <a href="#entry-elastic-interfaces-004" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Internal elastic wire:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire(UInt(32.W))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Usually emitted under graph <code>wires</code>; path follows the Chisel/Scala name, for example <code>/ewire0</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-005">
      <td style="vertical-align:top;"><code>e.Source.like</code>, <code>e.Sink.like</code>, <code>e.EWire.like</code> <a href="#entry-elastic-interfaces-005" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Preserve the payload type of an existing elastic interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire.like(source)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Creates a typed elastic interface wire.</td>
    </tr>
    <tr id="entry-elastic-interfaces-006">
      <td style="vertical-align:top;"><code>e.Source.many</code>, <code>e.Sink.many</code>, <code>e.Interface.many</code> <a href="#entry-elastic-interfaces-006" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Similar to the single-interface constructors, but returns <code>chext.util.NamedVec[e.Interface[T]]</code>:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sources = IO(e.Source.many(4, UInt(32.W)))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> <code>NamedVec</code> naming strictly uses underscores, for example <code>/sources_0</code>, <code>/sources_1</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-007">
      <td style="vertical-align:top;"><code>source :=&gt; sink</code> <a href="#entry-elastic-interfaces-007" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-007">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect elastic interfaces:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; sink</code></pre></td>
      <td style="vertical-align:top;">Creates <code>Connect</code>; <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connect</span>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-008">
      <td style="vertical-align:top;"><code>sources :=&gt; sinks</code> <a href="#entry-elastic-interfaces-008" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-008">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">sources :=&gt; sinks</code></pre></td>
      <td style="vertical-align:top;">Creates one <code>Connect</code> per pair; <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr id="entry-elastic-interfaces-009">
      <td style="vertical-align:top;"><code>e.Zip(&hellip;)</code> <a href="#entry-elastic-interfaces-009" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-009">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Zip.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Join sources into a bundle-valued elastic interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val zipped = e.Zip(sourceA, sourceB)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Not its own graph node; internally creates join-style wiring and a result wire.</td>
    </tr>
  </tbody>
</table>

## Elastic Components

Many elastic components support <code>fire { &hellip; }</code>, a protected hook that runs when the component's sink interface fires. The <code>Stall</code> row shows the pattern.

Buffer naming convention: <code>SourceBuffer</code>, <code>SinkBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used inline in a connection expression. <code>SourceBuffered</code> and <code>SinkBuffered</code> return a buffered interface directly; bind them to a <code>val</code> when the buffered interface is the thing you want to keep using.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-elastic-components-001">
      <td style="vertical-align:top;"><code>e.Connect</code> <a href="#entry-elastic-components-001" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Direct pass-through connection. Prefer <code>source :=&gt; sink</code> unless a named value is useful:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val connect0 = new e.Connect(source, sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Connect</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: connect</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-002">
      <td style="vertical-align:top;"><code>e.Transform</code> <a href="#entry-elastic-components-002" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transform.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Combinational payload transform:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val transform0 = new e.Transform(source, sink) {
  out := f(in)
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Transform</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: transform</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-003">
      <td style="vertical-align:top;"><code>e.Stall</code> <a href="#entry-elastic-components-003" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Stall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Conditionally hold a token; supports <code>fire</code>:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stall0 = new e.Stall(source, sink) {
  out := in
  cond { shouldStall }
  fire { didPass := true.B }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Stall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: stall</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-004">
      <td style="vertical-align:top;"><code>e.Drop</code> <a href="#entry-elastic-components-004" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Drop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Conditionally drop a token:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val drop0 = new e.Drop(source, sink) {
  out := in
  cond { shouldDrop }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Drop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: drop</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-005">
      <td style="vertical-align:top;"><code>e.Transducer</code> <a href="#entry-elastic-components-005" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transducer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Transducer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Transducer.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Stateful packet-level control:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val transducer0 = new e.Transducer(source, sink) {
  packet {
    when(done) {
      consume { state := 0.U }
    }.otherwise {
      accept { out := in }
    }
  }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Transducer</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: transducer</span> Ports: <code>source</code>, <code>sink</code>. Actions: <code>stall</code>, <code>accept</code>, <code>consume</code>, <code>produce</code>.</td>
    </tr>
    <tr id="entry-elastic-components-006">
      <td style="vertical-align:top;"><code>e.Queue</code> <a href="#entry-elastic-components-006" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Queue.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Queue.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Explicit queue between existing endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val queue0 =
  new e.Queue(source, sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: queue</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-007">
      <td style="vertical-align:top;"><code>e.SourceBuffer</code> <a href="#entry-elastic-components-007" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-007">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Insert a queue after a source, usually inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.SourceBuffer(source, count = 2) :=&gt; sink</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sourceBuffer</span>. Hierarchy: source -> <code>Queue</code> -> returned interface. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sourceBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-008">
      <td style="vertical-align:top;"><code>e.SinkBuffer</code> <a href="#entry-elastic-components-008" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-008">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Insert a queue before a sink, usually inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; e.SinkBuffer(sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sinkBuffer</span>. Hierarchy: returned interface -> <code>Queue</code> -> sink. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sinkBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-009">
      <td style="vertical-align:top;"><code>e.LeftBuffer</code> / <code>e.RightBuffer</code> <a href="#entry-elastic-components-009" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-009">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Generic aliases for source-side / sink-side buffering:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.LeftBuffer(source) :=&gt; sink
source :=&gt; e.RightBuffer(sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same hierarchy as <code>SourceBuffer</code> / <code>SinkBuffer</code>; default prefixes are <code>leftBuffer</code> / <code>rightBuffer</code>.</td>
    </tr>
    <tr id="entry-elastic-components-010">
      <td style="vertical-align:top;"><code>e.SourceBuffered</code> <a href="#entry-elastic-components-010" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-010">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Return a buffered source-side interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceBuffered0 =
  e.SourceBuffered(source, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence form uses <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sourceBufferedMany</span>.</td>
    </tr>
    <tr id="entry-elastic-components-011">
      <td style="vertical-align:top;"><code>e.SinkBuffered</code> <a href="#entry-elastic-components-011" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-011">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Return a buffered sink-side interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkBuffered0 =
  e.SinkBuffered(sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence form uses <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sinkBufferedMany</span>.</td>
    </tr>
    <tr id="entry-elastic-components-012">
      <td style="vertical-align:top;"><code>e.NullSink</code> <a href="#entry-elastic-components-012" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-012">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSink.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Consume/drop all tokens from a source:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSink0 =
  new e.NullSink(source)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSink</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSink</span> Port: <code>source</code>.</td>
    </tr>
    <tr id="entry-elastic-components-013">
      <td style="vertical-align:top;"><code>e.NullSource</code> <a href="#entry-elastic-components-013" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-013">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSource.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Drive a sink with no valid tokens:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSource0 =
  new e.NullSource(sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSource</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSource</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-014">
      <td style="vertical-align:top;"><code>e.Fork</code> <a href="#entry-elastic-components-014" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-014">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fork.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Branch one source to several sinks:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fork0 = new e.Fork(source) {
  fork(in.lo) :=&gt; sinkLo
  fork(in.hi) :=&gt; sinkHi
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fork</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fork</span> Ports: <code>source</code>, <code>sink_0</code>, <code>sink_1</code>, &hellip; The current implementation is eager, but the construction does not have to be conceptually limited to eager implementations.</td>
    </tr>
    <tr id="entry-elastic-components-015">
      <td style="vertical-align:top;"><code>e.Join</code> <a href="#entry-elastic-components-015" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-015">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Join.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Join several sources into one sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Join</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: join</span> Ports: <code>source_0</code>, <code>source_1</code>, &hellip;, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-016">
      <td style="vertical-align:top;"><code>e.Merger</code> <a href="#entry-elastic-components-016" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-016">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Merger.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple sources into one sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val merger0 =
  new e.Merger(Seq(source0, source1), sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Merger</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: merger</span> Ports: <code>source_i</code>, <code>sink</code>. Requires a strong external guarantee that at most one input is active at a given time.</td>
    </tr>
    <tr id="entry-elastic-components-017">
      <td style="vertical-align:top;"><code>e.Mux</code> <a href="#entry-elastic-components-017" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-017">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Select one source by an elastic select stream:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 =
  new e.Mux(sources, sink, sourceSelect)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Mux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: mux</span> Ports: <code>source_i</code>, <code>sourceSelect</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-018">
      <td style="vertical-align:top;"><code>e.Demux</code> <a href="#entry-elastic-components-018" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-018">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route one source to selected sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 =
  new e.Demux(source, sinks, sourceSelect)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Demux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demux</span> Ports: <code>source</code>, <code>sourceSelect</code>, <code>sink_i</code>.</td>
    </tr>
    <tr id="entry-elastic-components-019">
      <td style="vertical-align:top;"><code>e.Arbiter</code> <a href="#entry-elastic-components-019" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-019">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Arbiter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Arbiter.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Arbiter.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Select among sources and emit the selected index:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.Arbiter(sources, sink, sinkSelect, e.Chooser.rr)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Arbiter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>, <code>sinkSelect</code>.</td>
    </tr>
    <tr id="entry-elastic-components-020">
      <td style="vertical-align:top;"><code>e.ArbiterNs</code> <a href="#entry-elastic-components-020" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-020">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/ArbiterNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Select among sources without a selected-index stream:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.ArbiterNs(sources, sink, e.Chooser.rr)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: ArbiterNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-021">
      <td style="vertical-align:top;"><code>e.DemuxNs</code> <a href="#entry-elastic-components-021" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-021">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/DemuxNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route by a protected select function:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = new e.DemuxNs(source, sinks) {
  select { in =&gt; in.index }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: DemuxNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demuxNs</span> Ports: <code>source</code>, <code>sink_i</code>.</td>
    </tr>
    <tr id="entry-elastic-components-022">
      <td style="vertical-align:top;"><code>e.Count</code> <a href="#entry-elastic-components-022" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-022">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Stateful repeat/count primitive:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in =&gt; 0.U }
  cond { (in, state) =&gt; state =/= in.limit }
  next { (in, state) =&gt; state + 1.U }
  out { (in, state, first, last) =&gt; in }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Count</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: count</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-023">
      <td style="vertical-align:top;">Self-alias form <a href="#entry-elastic-components-023" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-023">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Useful when nesting components and you want a stable name for the outer anonymous instance:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) { count0 =&gt;
  count0.init { in =&gt; 0.U }
}</code></pre></td>
      <td style="vertical-align:top;">Scala construction style only; graph behavior is unchanged.</td>
    </tr>
    <tr id="entry-elastic-components-024">
      <td style="vertical-align:top;"><code>e.Once</code> <a href="#entry-elastic-components-024" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-024">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">One-shot source, explicit component form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkOnce = e.EWire(UInt(8.W))
val once0 = new e.Once(sinkOnce) {
  out := value
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Once</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: once</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-025">
      <td style="vertical-align:top;"><code>e.Once(value)</code> <a href="#entry-elastic-components-025" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-025">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Functional one-shot form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceOnce = e.Once(value)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Once</code> component named <code>once0</code> by local val.</td>
    </tr>
    <tr id="entry-elastic-components-026">
      <td style="vertical-align:top;"><code>e.Counter</code> <a href="#entry-elastic-components-026" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-026">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Explicit component form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkCounter = e.EWire(UInt(8.W))
val counter0 = new e.Counter(sinkCounter)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Counter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: counter</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-027">
      <td style="vertical-align:top;"><code>e.Counter(&hellip;)</code> / <code>e.Counter.fromWidth(&hellip;)</code> <a href="#entry-elastic-components-027" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-027">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Functional source form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceCounter =
  e.Counter(maxValueExclusive = 16)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Counter</code> component named <code>counter0</code> by local val.</td>
    </tr>
    <tr id="entry-elastic-components-028">
      <td style="vertical-align:top;"><code>e.Wrap</code> <a href="#entry-elastic-components-028" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-028">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Wrap.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Wrap delayed logic:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val wrap0 = new e.Wrap(source, sink) {
  protected def delay = 2
  out := f(in)
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Wrap</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: wrap</span> Nonzero delay internally creates <code>Counter</code>, <code>Queue</code>, and <code>Connect</code>.</td>
    </tr>
  </tbody>
</table>

## Elastic Containers

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-elastic-containers-001">
      <td style="vertical-align:top;"><code>e.Repeat</code> <a href="#entry-elastic-containers-001" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Repeat.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Repeat one input token multiple times:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in =&gt; in.length }
  out { (in, index, first, last) =&gt; in }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Repeat</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: repeat</span>. Hierarchy: creates a child <code>Count</code> under <code>withContainer(this)</code>, with observed path shape <code>/repeat0</code> -> <code>/repeat0_count</code>.</td>
    </tr>
    <tr id="entry-elastic-containers-002">
      <td style="vertical-align:top;"><code>e.Fold</code> <a href="#entry-elastic-containers-002" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fold.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Fold.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Fold.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Reduce a stream using user-provided fold logic:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in =&gt; in.data }
  last { in =&gt; in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fold</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fold</span>. User-facing internal wires: <code>sinkA</code>, <code>sinkB</code>, <code>sourceResult</code>. Hierarchy: stage wiring under <code>stage0</code> and <code>stage1</code>; children include <code>Transducer</code> or <code>Transform</code>, <code>Join</code>, <code>Connect</code>, and buffers attached to the <code>Fold</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-003">
      <td style="vertical-align:top;"><code>e.Loop</code> <a href="#entry-elastic-containers-003" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Loop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Iterative elastic loop with user-visible body endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state =&gt; state.done }
  sinkCurrent :=&gt; loopBody.source
  loopBody.sink :=&gt; sourceNext
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Loop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: loop</span>. User-facing internal wires: <code>sinkCurrent</code>, <code>sourceNext</code>. Hierarchy: creates <code>Stall</code>, <code>Connect</code>, <code>Fork</code>, <code>Demux</code>, <code>Merger</code>, and buffers attached to the <code>Loop</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-004">
      <td style="vertical-align:top;"><code>e.Scope</code> <a href="#entry-elastic-containers-004" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Scope.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Begin/end scoped elastic region:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in =&gt; started := true.B }
  exit { out =&gt; finished := true.B }
  sinkBegin :=&gt; body.source
  body.sink :=&gt; sourceEnd
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Scope</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: scope</span>. User-facing internal wires: <code>sinkBegin</code>, <code>sourceEnd</code>. Hierarchy: creates a <code>Stall</code>, a <code>Connect</code>, and sink buffers attached to the <code>Scope</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-005">
      <td style="vertical-align:top;"><code>e.RandomStall</code> <a href="#entry-elastic-containers-005" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/RandomStall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Test/diagnostic random backpressure:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val randomStall0 =
  new e.RandomStall(source, sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: RandomStall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: randomStall</span>. Hierarchy: creates <code>Stall</code> plus <code>SinkBuffer</code>; observed child paths include <code>/randomStall0_stall</code> and <code>/randomStall0_stall_sinkBuffer0_queue0</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Imports

AXI examples assume:

```scala
import chext.amba.axi4
import axi4.{full => axi4f, lite => axi4l}
import axi4.ConnectOp._
import chext.{elastic => e}
```

## AXI4 Interfaces and Connects

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-axi-connects-001">
      <td style="vertical-align:top;"><code>axi4.Config</code> <a href="#entry-axi-connects-001" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val axiCfg = axi4.Config(
  wId = 4,    // ID width; 0 is common for lite
  wAddr = 32, // byte address width
  wData = 64, // data bus width
  read = true,
  write = true,
  lite = false,
  hasLock = true,
  hasCache = true,
  hasProt = true,
  hasQos = true,
  hasRegion = true,
  axi3Compat = false,
  wUserAR = 0,
  wUserR = 0,
  wUserAW = 0,
  wUserW = 0,
  wUserB = 0
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for raw, full, and lite AXI interfaces. <code>lite = true</code> selects AXI4-Lite behavior; full AXI keeps <code>lite = false</code>.</td>
    </tr>
    <tr id="entry-axi-connects-001-full-connect-config">
      <td style="vertical-align:top;"><code>axi4f.ConnectConfig</code> <a href="#entry-axi-connects-001-full-connect-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-001-full-connect-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Configured full AXI connect diagnostics:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.ConnectConfig(
  tieOffMaster = true,
  tieOffSlave = true,
  warnReadWriteMismatch = true,
  warnAxi3Compat = true,
  warnIdWidth = true,
  warnAddrWidth = true,
  warnSideband = true,
  // SimulationCheck: Default, None, Printf, Assert.
  // Default resolves to the process-wide policy.
  simCheckAxi3Compat = chext.util.SimulationCheck.Default,
  simCheckIdWidth = chext.util.SimulationCheck.Default,
  simCheckAddrWidth = chext.util.SimulationCheck.Default
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>s_axi.connect(m_axi, cfg)</code>: controls tie-offs, warnings, and simulation checks for full AXI connections.</td>
    </tr>
    <tr id="entry-axi-connects-001-lite-connect-config">
      <td style="vertical-align:top;"><code>axi4l.ConnectConfig</code> <a href="#entry-axi-connects-001-lite-connect-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-001-lite-connect-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Configured AXI4-Lite connect diagnostics:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.ConnectConfig(
  tieOffMaster = true,
  tieOffSlave = true,
  warnReadWriteMismatch = true,
  warnAddrWidth = true,
  // SimulationCheck: Default, None, Printf, Assert.
  // Default resolves to the process-wide policy.
  simCheckAddrWidth = chext.util.SimulationCheck.Default
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>s_axil.connect(m_axil, cfg)</code>: controls tie-offs, warnings, and address-width simulation checks for AXI4-Lite connections.</td>
    </tr>
    <tr id="entry-axi-connects-002">
      <td style="vertical-align:top;"><code>axi4f.Slave</code> / <code>axi4f.Master</code> <a href="#entry-axi-connects-002" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Full AXI IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi = IO(axi4f.Slave(axiCfg))
val m_axi = IO(axi4f.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr id="entry-axi-connects-003">
      <td style="vertical-align:top;"><code>axi4l.Slave</code> / <code>axi4l.Master</code> <a href="#entry-axi-connects-003" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil = IO(axi4l.Slave(axiCfg))
val m_axil = IO(axi4l.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr id="entry-axi-connects-004">
      <td style="vertical-align:top;">Full single connect <a href="#entry-axi-connects-004" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one full AXI interface pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi :=&gt; m_axi</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4f.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4f_connect</span>; graph <code>tpe</code> is <code>Axi4f_Connect</code>.</td>
    </tr>
    <tr id="entry-axi-connects-005">
      <td style="vertical-align:top;">Full configured connect <a href="#entry-axi-connects-005" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Same, with diagnostics/tie-off config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi.connect(m_axi, axi4f.ConnectConfig())</code></pre></td>
      <td style="vertical-align:top;">Same component and prefix as full single connect.</td>
    </tr>
    <tr id="entry-axi-connects-006">
      <td style="vertical-align:top;">Full many connect <a href="#entry-axi-connects-006" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching full AXI sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_N :=&gt; m_axi_N</code></pre></td>
      <td style="vertical-align:top;">Creates one full connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr id="entry-axi-connects-007">
      <td style="vertical-align:top;">Lite single connect <a href="#entry-axi-connects-007" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-007">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one AXI4-Lite pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil :=&gt; m_axil</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4l.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4l_connect</span>; graph <code>tpe</code> is <code>Axi4l_Connect</code>.</td>
    </tr>
    <tr id="entry-axi-connects-008">
      <td style="vertical-align:top;">Lite configured connect <a href="#entry-axi-connects-008" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-008">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Same, with diagnostics/tie-off config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil.connect(m_axil, axi4l.ConnectConfig())</code></pre></td>
      <td style="vertical-align:top;">Same component and prefix as lite single connect.</td>
    </tr>
    <tr id="entry-axi-connects-009">
      <td style="vertical-align:top;">Lite many connect <a href="#entry-axi-connects-009" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-009">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching AXI4-Lite sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil_N :=&gt; m_axil_N</code></pre></td>
      <td style="vertical-align:top;">Creates one lite connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr id="entry-axi-connects-010">
      <td style="vertical-align:top;">Raw single connect <a href="#entry-axi-connects-010" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-010">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect raw AXI interfaces and dispatch to full/lite based on config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw :=&gt; m_axi_raw</code></pre></td>
      <td style="vertical-align:top;">Dispatch helper; creates full or lite connect components depending on interface config.</td>
    </tr>
    <tr id="entry-axi-connects-011">
      <td style="vertical-align:top;">Raw many connect <a href="#entry-axi-connects-011" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-011">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching raw AXI sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw_N :=&gt; m_axi_raw_N</code></pre></td>
      <td style="vertical-align:top;">Dispatch helper; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Buffers

AXI buffer naming follows the elastic convention: <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used anonymously inside a connection expression. <code>SlaveBuffered</code> and <code>MasterBuffered</code> return an interface directly; bind them to a <code>val</code> when the buffered AXI side is reused.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-axi-buffers-000">
      <td style="vertical-align:top;"><code>axi4.BufferConfig</code> <a href="#entry-axi-buffers-000" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-000">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Per-channel buffer depths:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = axi4.BufferConfig(
  aw = 2, // write address
  w = 2,  // write data
  b = 2,  // write response
  ar = 2, // read address
  r = 2   // read data
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration shared by full and lite AXI buffer helpers. <code>axi4.BufferConfig.all(2)</code> applies the same depth to all five channels.</td>
    </tr>
    <tr id="entry-axi-buffers-001">
      <td style="vertical-align:top;"><code>axi4f.SlaveBuffer</code> / <code>axi4f.MasterBuffer</code> <a href="#entry-axi-buffers-001" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer a full AXI side inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4f.SlaveBuffer(s_axi, bufferCfg) :=&gt; m_axi
s_axi :=&gt; axi4f.MasterBuffer(m_axi, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. Read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-002">
      <td style="vertical-align:top;"><code>axi4f.SlaveBuffered</code> / <code>axi4f.MasterBuffered</code> <a href="#entry-axi-buffers-002" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered full AXI side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>slaveBufferedMany</code> / <code>masterBufferedMany</code>.</td>
    </tr>
    <tr id="entry-axi-buffers-003">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffer</code> / <code>axi4l.MasterBuffer</code> <a href="#entry-axi-buffers-003" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer an AXI4-Lite side inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4l.SlaveBuffer(s_axil, bufferCfg) :=&gt; m_axil
s_axil :=&gt; axi4l.MasterBuffer(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. AXI4-Lite read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-004">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffered</code> / <code>axi4l.MasterBuffered</code> <a href="#entry-axi-buffers-004" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered AXI4-Lite side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>slaveBufferedMany</code> / <code>masterBufferedMany</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Full Components

AXI4 full components are Chisel modules. They are not Chext components themselves; graph content comes from the elastic components they instantiate internally.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-axi4-full-components-000-demux-config">
      <td style="vertical-align:top;"><code>axi4f.components.DemuxConfig</code> <a href="#entry-axi4-full-components-000-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-000-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.DemuxConfig(
  axiSlaveCfg = fullCfg,
  numMasters = 4, // output ports
  decodeFn = addr =&gt; addr(13, 12),
  numIdsTrackedRead = 4,
  numIdsTrackedWrite = 4,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  capacityPortQueueW = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Demux</code>; <code>decodeFn</code> maps each address to an output port.</td>
    </tr>
    <tr id="entry-axi4-full-components-001">
      <td style="vertical-align:top;"><code>axi4f.components.Demux</code> <a href="#entry-axi4-full-components-001" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Fan one full AXI slave-side port out to multiple master-side ports:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4f.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-out by address/routing selection. Internally uses channel-level elastic wiring around <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, and <code>b</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-001-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.MuxConfig</code> <a href="#entry-axi4-full-components-001-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-001-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.MuxConfig(
  axiSlaveCfg = fullCfg,
  numSlaves = 4, // input ports
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Mux</code>; output IDs are widened to include the selected input port.</td>
    </tr>
    <tr id="entry-axi4-full-components-002">
      <td style="vertical-align:top;"><code>axi4f.components.Mux</code> <a href="#entry-axi4-full-components-002" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple full AXI slave-side ports into one master-side port:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4f.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-in. Internally creates arbitration and channel-level elastic wiring for read and write paths.</td>
    </tr>
    <tr id="entry-axi4-full-components-002-id-demux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemuxConfig</code> <a href="#entry-axi4-full-components-002-id-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-002-id-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdDemuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // routes by 2 ID bits
  capacityPortQueueW = 8,
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdDemux</code>; <code>wIdSel</code> determines the number of output ports as <code>1 &lt;&lt; wIdSel</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-003">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemux</code> <a href="#entry-axi4-full-components-003" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route full AXI traffic by transaction ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idDemux0 = Module(new axi4f.components.IdDemux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-based full AXI fan-out. Internally tracks transactions and expands to elastic channel components.</td>
    </tr>
    <tr id="entry-axi4-full-components-003-id-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdMuxConfig</code> <a href="#entry-axi4-full-components-003-id-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-003-id-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdMuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // input port bits in ID
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdMux</code>; response routing uses ID bits to recover the input port.</td>
    </tr>
    <tr id="entry-axi4-full-components-004">
      <td style="vertical-align:top;"><code>axi4f.components.IdMux</code> <a href="#entry-axi4-full-components-004" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge full AXI traffic while preserving ID-based response routing:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idMux0 = Module(new axi4f.components.IdMux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-aware full AXI fan-in. Internally uses transaction tracking and channel-level elastic components.</td>
    </tr>
    <tr id="entry-axi4-full-components-004-id-serialize-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerializeConfig</code> <a href="#entry-axi4-full-components-004-id-serialize-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-004-id-serialize-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID serialization configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdSerializeConfig(
  axiSlaveCfg = fullCfg,
  numOutstandingRead = 4,
  numOutstandingWrite = 4,
  wIdSelect = 0
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdSerialize</code>; output ID width becomes zero.</td>
    </tr>
    <tr id="entry-axi4-full-components-005">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerialize</code> <a href="#entry-axi4-full-components-005" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdSerialize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdSerialize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Serialize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idSerialize0 =
  Module(new axi4f.components.IdSerialize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID serialization. Internally limits outstanding work and routes response channels through elastic control logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-005-id-parallelize-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdParallelizeConfig</code> <a href="#entry-axi4-full-components-005-id-parallelize-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-005-id-parallelize-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID parallelization configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdParallelizeConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0),
  wIdMaster = 3,
  wBufferIndex = 10,
  readUseSyncMem = true,
  writeUseSyncMem = true
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdParallelize</code>; creates a wider-ID master side from a zero-ID slave side.</td>
    </tr>
    <tr id="entry-axi4-full-components-006">
      <td style="vertical-align:top;"><code>axi4f.components.IdParallelize</code> <a href="#entry-axi4-full-components-006" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdParallelize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdParallelize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Parallelize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idParallelize0 =
  Module(new axi4f.components.IdParallelize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID parallelization. Internally distributes requests and rejoins responses with elastic channel logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-006-upscale-config">
      <td style="vertical-align:top;"><code>axi4f.components.UpscaleConfig</code> <a href="#entry-axi4-full-components-006-upscale-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-006-upscale-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Upscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Width upscale configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.UpscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 32),
  wDataMaster = 64,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Upscale</code>; master data width must be wider than the slave data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-007">
      <td style="vertical-align:top;"><code>axi4f.components.Upscale</code> <a href="#entry-axi4-full-components-007" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-007">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Upscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Upscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Upscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Adapt a narrower full AXI data bus to a wider one:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val upscale0 = Module(new axi4f.components.Upscale(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width upscaler. Internally adapts read/write data channels and keeps address/control channels aligned.</td>
    </tr>
    <tr id="entry-axi4-full-components-007-downscale-config">
      <td style="vertical-align:top;"><code>axi4f.components.DownscaleConfig</code> <a href="#entry-axi4-full-components-007-downscale-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-007-downscale-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Width downscale configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Downscale</code>; master data width must be narrower than the slave data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-008">
      <td style="vertical-align:top;"><code>axi4f.components.Downscale</code> <a href="#entry-axi4-full-components-008" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-008">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Adapt a wider full AXI data bus to a narrower one:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val downscale0 = Module(new axi4f.components.Downscale(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width downscaler. Internally splits wider beats and coordinates read/write data flow through elastic logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-008-unburst-config">
      <td style="vertical-align:top;"><code>axi4f.components.UnburstConfig</code> <a href="#entry-axi4-full-components-008-unburst-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-008-unburst-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Burst decomposition configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.UnburstConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Unburst</code>; input and output AXI configs are the same, but bursts are decomposed internally.</td>
    </tr>
    <tr id="entry-axi4-full-components-009">
      <td style="vertical-align:top;"><code>axi4f.components.Unburst</code> <a href="#entry-axi4-full-components-009" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-009">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Unburst.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Unburst.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Convert burst transactions into single-beat transactions:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val unburst0 = Module(new axi4f.components.Unburst(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI burst decomposition. Internally generates per-beat addresses and coordinates response/data channels.</td>
    </tr>
    <tr id="entry-axi4-full-components-009-widen-config">
      <td style="vertical-align:top;"><code>axi4f.components.WidenConfig</code> <a href="#entry-axi4-full-components-009-widen-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-009-widen-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Widen.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Narrow-transfer widening configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.WidenConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Widen</code>; converts narrow transfers to full-sized transfers on the same AXI data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-010">
      <td style="vertical-align:top;"><code>axi4f.components.Widen</code> <a href="#entry-axi4-full-components-010" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-010">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Widen.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Widen.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Widen.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Bridge full AXI width differences with widening behavior:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val widen0 = Module(new axi4f.components.Widen(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI data widening helper. Internally coordinates address, strobe, data, and response handling through channel logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-010-credit-buffer-config">
      <td style="vertical-align:top;"><code>axi4f.components.CreditBufferConfig</code> <a href="#entry-axi4-full-components-010-credit-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-010-credit-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Response-aware full AXI buffering configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.CreditBufferConfig(
  axiCfg = fullCfg,
  rBuffer = 32, // read response credits
  wBuffer = 8,  // write payload credits
  bBuffer = 32  // write response credits
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for full AXI <code>CreditBuffer</code>; address channels wait until local response/payload buffering has capacity.</td>
    </tr>
    <tr id="entry-axi4-full-components-010-credit-buffer">
      <td style="vertical-align:top;"><code>axi4f.components.CreditBuffer</code> <a href="#entry-axi4-full-components-010-credit-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-010-credit-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Add response-aware buffering for full AXI:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val creditBuffer0 =
  Module(new axi4f.components.CreditBuffer(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI credit-style response buffering around read/write channels.</td>
    </tr>
    <tr id="entry-axi4-full-components-010-protocol-converter-config">
      <td style="vertical-align:top;"><code>axi4f.components.ProtocolConverterConfig</code> <a href="#entry-axi4-full-components-010-protocol-converter-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-010-protocol-converter-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Staged protocol conversion configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.ProtocolConverterConfig(
  axiSlaveCfg = fullCfg.copy(wId = 4, wData = 32),
  axiMasterCfg = fullCfg.copy(wId = 2, wData = 64),
  slaveNeverBursts = false
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>ProtocolConverter</code>; internally selects ID, burst, and width conversion stages.</td>
    </tr>
    <tr id="entry-axi4-full-components-011">
      <td style="vertical-align:top;"><code>axi4f.components.ProtocolConverter</code> <a href="#entry-axi4-full-components-011" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-011">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Convert between supported full AXI protocol shapes:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val protocolConverter0 =
  Module(new axi4f.components.ProtocolConverter(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Staged full AXI protocol conversion. It composes lower-level full AXI component modules and their internal elastic channel components.</td>
    </tr>
  </tbody>
</table>

## AXI4 Lite Components

AXI4-Lite components are Chisel modules. They are not Chext components themselves; graph content comes from the internal channel-level elastic components.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-axi4-lite-components-000-demux-config">
      <td style="vertical-align:top;"><code>axi4l.components.DemuxConfig</code> <a href="#entry-axi4-lite-components-000-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-000-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.DemuxConfig(
  axiSlaveCfg = liteCfg,
  numMasters = 4, // output ports
  decodeFn = addr =&gt; addr(13, 12),
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for AXI4-Lite <code>Demux</code>; <code>decodeFn</code> maps each address to an output port.</td>
    </tr>
    <tr id="entry-axi4-lite-components-001">
      <td style="vertical-align:top;"><code>axi4l.components.Demux</code> <a href="#entry-axi4-lite-components-001" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan one AXI4-Lite slave-side port out to multiple master-side ports:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4l.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-out for address/data/control channels.</td>
    </tr>
    <tr id="entry-axi4-lite-components-001-mux-config">
      <td style="vertical-align:top;"><code>axi4l.components.MuxConfig</code> <a href="#entry-axi4-lite-components-001-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-001-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.MuxConfig(
  axiSlaveCfg = liteCfg,
  numSlaves = 4, // input ports
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for AXI4-Lite <code>Mux</code>; queue capacities tune per-channel port tracking.</td>
    </tr>
    <tr id="entry-axi4-lite-components-002">
      <td style="vertical-align:top;"><code>axi4l.components.Mux</code> <a href="#entry-axi4-lite-components-002" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple AXI4-Lite slave-side ports into one master-side port:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4l.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-in with channel-level elastic arbitration and routing.</td>
    </tr>
    <tr id="entry-axi4-lite-components-002-credit-buffer-config">
      <td style="vertical-align:top;"><code>axi4l.components.CreditBufferConfig</code> <a href="#entry-axi4-lite-components-002-credit-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-002-credit-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Response-aware buffering configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.CreditBufferConfig(
  axiCfg = liteCfg,
  rBuffer = 8, // read response credits
  wBuffer = 8, // write payload credits
  bBuffer = 8  // write response credits
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>CreditBuffer</code>; address channels are delayed until local response/payload buffering has capacity.</td>
    </tr>
    <tr id="entry-axi4-lite-components-003">
      <td style="vertical-align:top;"><code>axi4l.components.CreditBuffer</code> <a href="#entry-axi4-lite-components-003" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Add response-aware buffering for AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val creditBuffer0 =
  Module(new axi4l.components.CreditBuffer(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite response buffering with credit-style control around read/write channels.</td>
    </tr>
    <tr id="entry-axi4-lite-components-004">
      <td style="vertical-align:top;"><code>axi4l.components.MemController</code> <a href="#entry-axi4-lite-components-004" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/MemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a memory-like backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val memController0 =
  Module(new axi4l.components.MemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite memory controller. Internally bridges AXI4-Lite channels to memory-style request and response paths.</td>
    </tr>
    <tr id="entry-axi4-lite-components-005">
      <td style="vertical-align:top;"><code>axi4l.components.SyncReadMemController</code> <a href="#entry-axi4-lite-components-005" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/SyncReadMemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a synchronous-read memory backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val syncReadMemController0 =
  Module(new axi4l.components.SyncReadMemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite controller variant for synchronous-read memories.</td>
    </tr>
    <tr id="entry-axi4-lite-components-006">
      <td style="vertical-align:top;"><code>axi4l.components.RegisterBlock</code> <a href="#entry-axi4-lite-components-006" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/RegisterBlock.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Build an AXI4-Lite register block:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val regs = new axi4l.components.RegisterBlock(
  wAddr = 32, // AXI4-Lite address width
  wData = 32, // AXI4-Lite data width
  wMask = 12  // register address-space mask
)
val s_axil = IO(axi4l.Slave(regs.cfgAxi))
s_axil :=&gt; regs.s_axil</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite register block module with generated read/write register access behavior.</td>
    </tr>
  </tbody>
</table>

## Memory

Memory interfaces are bundle-level protocols whose request/response fields are elastic interfaces. RAM wrappers are Chisel modules; their graph visibility mostly comes from the elastic ports and components they instantiate or connect.

Memory buffer naming follows the same convention as elastic and AXI: <code>SlaveBuffer</code> / <code>MasterBuffer</code> are the named-prefix inline forms, while <code>SlaveBuffered</code> / <code>MasterBuffered</code> are for binding the returned interface to a <code>val</code>.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-memory-001">
      <td style="vertical-align:top;"><code>memory.ReadInterface</code>, <code>memory.WriteInterface</code> <a href="#entry-memory-001" style="text-decoration:none;" aria-label="Permalink to entry-memory-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Interfaces.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory read/write protocol bundles.</td>
      <td style="vertical-align:top;">Request/response bundle interfaces for memory-like ports. Their <code>req</code>/<code>resp</code> fields are elastic interfaces and can appear in elastic graph layers.</td>
    </tr>
    <tr id="entry-memory-002">
      <td style="vertical-align:top;"><code>memory.ConnectOp._</code> / <code>master :=&gt; slave</code> <a href="#entry-memory-002" style="text-decoration:none;" aria-label="Permalink to entry-memory-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect memory interfaces or sequences.</td>
      <td style="vertical-align:top;">Creates elastic <code>Connect</code> components on request/response channels; sequence form uses <code>connectMany</code>.</td>
    </tr>
    <tr id="entry-memory-003">
      <td style="vertical-align:top;"><code>memory.BufferConfig</code> <a href="#entry-memory-003" style="text-decoration:none;" aria-label="Permalink to entry-memory-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory interface buffer depths:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig(
  req = 2,  // request-channel queue depth
  resp = 2  // response-channel queue depth
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for memory <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>SlaveBuffered</code>, and <code>MasterBuffered</code>.</td>
    </tr>
    <tr id="entry-memory-004">
      <td style="vertical-align:top;"><code>memory.SlaveBuffer</code>, <code>memory.MasterBuffer</code> <a href="#entry-memory-004" style="text-decoration:none;" aria-label="Permalink to entry-memory-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer memory interfaces inline:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">memory.SlaveBuffer(readMaster, bufferCfg) :=&gt; readSlave
writeMaster :=&gt; memory.MasterBuffer(writeSlave, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates elastic <code>SourceBuffer</code> and <code>SinkBuffer</code> on <code>req</code>/<code>resp</code> channels.</td>
    </tr>
    <tr id="entry-memory-005">
      <td style="vertical-align:top;"><code>memory.SlaveBuffered</code>, <code>memory.MasterBuffered</code> <a href="#entry-memory-005" style="text-decoration:none;" aria-label="Permalink to entry-memory-005">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered memory interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig.all(2) // req and resp depth
val readBuffered = memory.SlaveBuffered(readMaster, bufferCfg)
val writeBuffered = memory.MasterBuffered(writeSlave, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same request/response buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>leftBufferedMany</code> / <code>rightBufferedMany</code>.</td>
    </tr>
    <tr id="entry-memory-006">
      <td style="vertical-align:top;"><code>memory.RawMemConfig</code> / <code>memory.PortConfig</code> <a href="#entry-memory-006" style="text-decoration:none;" aria-label="Permalink to entry-memory-006">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/RawMem.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/memory/RAM.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">RAM wrapper configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val rawCfg = memory.RawMemConfig(
  wAddr = 10,       // 1024 elements
  wData = 32,       // data width
  latencyRead = 2,
  latencyWrite = 1
)
val portCfg = memory.PortConfig(
  numOutstandingRead = 8,
  numOutstandingWrite = 8,
  arbiterFunc = memory.ReadWriteArbiter.defaultFunc
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> <code>RawMemConfig</code> describes the target memory shape and latency. <code>PortConfig</code> sets elastic buffering around RAM read/write ports.</td>
    </tr>
    <tr id="entry-memory-007">
      <td style="vertical-align:top;"><code>memory.SinglePortRAM</code>, <code>SimpleDualPortRAM</code>, <code>TrueDualPortRAM</code> <a href="#entry-memory-007" style="text-decoration:none;" aria-label="Permalink to entry-memory-007">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/RAM.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory wrappers around raw memory targets:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ram0 =
  Module(new memory.TrueDualPortRAM(rawCfg, portCfg, portCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> RAM wrappers with elastic read/write ports and raw memory bridges. Not tracking components; elastic ports participate when connected.</td>
    </tr>
  </tbody>
</table>

## Stream

Stream modules create AXI-facing work/result frontends. They are Chisel modules; graph visibility comes from their task/data/result elastic channels and internal elastic control components.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-stream-001">
      <td style="vertical-align:top;"><code>stream.ChunkConfig</code> <a href="#entry-stream-001" style="text-decoration:none;" aria-label="Permalink to entry-stream-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Chunk.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Chunking configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = stream.ChunkConfig(
  wAddress = axiCfg.wAddr,
  wLength = 32,        // request length width
  wData = axiCfg.wData,
  maxBurstLength = 256,
  genUser = UInt(0.W)
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>stream.Chunk</code>; controls address width, length width, data width, and maximum emitted burst length.</td>
    </tr>
    <tr id="entry-stream-002">
      <td style="vertical-align:top;"><code>stream.Chunk</code> <a href="#entry-stream-002" style="text-decoration:none;" aria-label="Permalink to entry-stream-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Chunk.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Split a variable-length stream request into AXI-sized chunks:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val chunk0 = Module(new stream.Chunk(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Chunks stream requests/results by repeatedly emitting per-beat work. Internally creates an elastic <code>Count</code>.</td>
    </tr>
    <tr id="entry-stream-003">
      <td style="vertical-align:top;"><code>stream.ReadConfig</code> / <code>stream.WriteConfig</code> <a href="#entry-stream-003" style="text-decoration:none;" aria-label="Permalink to entry-stream-003">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI stream frontend configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val readCfg = stream.ReadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  genUser = UInt(0.W),
  resultMode = stream.ReadResultMode.LastAlwaysInvalid,
  maxBurstLength = 256,
  wLength = 32,
  numOutstandingTasks = 8
)
val writeCfg = stream.WriteConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  genUser = UInt(0.W),
  resultMode = stream.WriteResultMode.KeepAll,
  maxBurstLength = 256,
  wLength = 32,
  numOutstandingTasks = 8
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> <code>ReadConfig</code> controls read result mode and outstanding task buffering. <code>WriteConfig</code> controls write result mode and outstanding task buffering.</td>
    </tr>
    <tr id="entry-stream-004">
      <td style="vertical-align:top;"><code>stream.Read</code>, <code>stream.Write</code> <a href="#entry-stream-004" style="text-decoration:none;" aria-label="Permalink to entry-stream-004">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/stream/Read.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../src/test/scala/chext/stream/Write.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Read.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Write.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">AXI stream read/write frontends:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val read0 = Module(new stream.Read(readCfg))
val write0 = Module(new stream.Write(writeCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI stream read/write engines. They create task/data/result elastic channels and internally use <code>Drop</code>, <code>Fork</code>, <code>Transform</code>, <code>Repeat</code>, <code>Join</code>, <code>Mux</code>, buffers, and null channel components.</td>
    </tr>
  </tbody>
</table>

## Load/Store

Load/store modules are single-beat AXI full frontends. They expose elastic task/result interfaces and a full AXI master port, then build the necessary AXI channel traffic internally.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-load-store-001">
      <td style="vertical-align:top;"><code>ldstr.LoadConfig</code> / <code>ldstr.StoreConfig</code> <a href="#entry-load-store-001" style="text-decoration:none;" aria-label="Permalink to entry-load-store-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Single-beat load/store frontend configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loadCfg = ldstr.LoadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  genUser = UInt(0.W),
  numOutstandingTasks = 8
)
val storeCfg = ldstr.StoreConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  genUser = UInt(0.W),
  numOutstandingTasks = 8
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>ldstr.Load</code> and <code>ldstr.Store</code>; <code>numOutstandingTasks</code> sizes the buffering around AXI read/write response paths.</td>
    </tr>
    <tr id="entry-load-store-002">
      <td style="vertical-align:top;"><code>ldstr.Load</code>, <code>ldstr.Store</code> <a href="#entry-load-store-002" style="text-decoration:none;" aria-label="Permalink to entry-load-store-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Load/store AXI frontends:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val load0 = Module(new ldstr.Load(loadCfg))
val store0 = Module(new ldstr.Store(storeCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Load/store frontends for AXI memory access. They internally use elastic <code>Fork</code>, <code>Transform</code>, <code>SinkBuffer</code>, <code>Join</code>, and null channel components around AXI <code>ar/r</code> or <code>aw/w/b</code> paths.</td>
    </tr>
  </tbody>
</table>

## Float

Floating-point elastic modules wrap floating-point datapaths with elastic source/sink interfaces. Their graph participation comes from the wrapper queues, joins, and surrounding elastic wiring.

<table style="table-layout:fixed;width:100%;">
  <colgroup>
    <col style="width:24%;">
    <col style="width:42%;">
    <col style="width:34%;">
  </colgroup>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr id="entry-float-001">
      <td style="vertical-align:top;"><code>float.FloatingPoint</code> <a href="#entry-float-001" style="text-decoration:none;" aria-label="Permalink to entry-float-001">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/float/FloatingPoint.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Floating-point format configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fp = float.FloatingPoint.ieee_fp32
val custom = float.FloatingPoint(
  exponent_width = 8,
  mantissa_width = 23
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Bundle/config object for floating-point datapaths. Common helpers include <code>ieee_fp16</code>, <code>ieee_fp32</code>, <code>ieee_fp64</code>, <code>fp18</code>, and <code>bfloat16</code>.</td>
    </tr>
    <tr id="entry-float-002">
      <td style="vertical-align:top;"><code>float.ElasticAdd</code>, <code>float.ElasticMultiply</code> <a href="#entry-float-002" style="text-decoration:none;" aria-label="Permalink to entry-float-002">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/float/Elastic.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/float/Elastic.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/float/src/ElasticTop.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Elastic floating-point operators:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val add0 =
  Module(new float.ElasticAdd(fp, combinational = false))
val mul0 =
  Module(new float.ElasticMultiply(fp, combinational = false))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Elastic wrappers around floating-point add/multiply datapaths. They expose elastic input/output interfaces; graph participation comes through surrounding elastic wiring and transforms.</td>
    </tr>
  </tbody>
</table>
