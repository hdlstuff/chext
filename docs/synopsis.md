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
<span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span>
<span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span>
<span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix</span>
<span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe</span>
<span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix</span>
<span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span>
<span style="background:#e8f5e9;color:#1b5e20;padding:2px 6px;border-radius:4px;">SysC</span>

<code>tpe</code> is the graph type. <code>namePrefix</code> is the construction's conceptual default prefix. Actual paths come from Chisel naming, <code>prefix(&hellip;)</code>, and <code>uniquePrefix(&hellip;)</code>.

## Elastic Imports

Elastic examples assume:

```scala
import chext.{elastic => e}
import e.ConnectOp._
```

## Elastic Interfaces and Operators

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>e.Interface(gen)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>For Chisel types:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val genElastic = e.Interface(UInt(32.W))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Not a component. When materialized as hardware and tracked, appears as <code>chext.elastic.Interface[&hellip;]</code>.</td>
    </tr>
    <tr>
      <td><code>e.Source(gen)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Producer endpoint from the module's perspective:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val source = IO(e.Source(UInt(32.W)))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sources</code>.</td>
    </tr>
    <tr>
      <td><code>e.Sink(gen)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Consumer endpoint from the module's perspective:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sink = IO(e.Sink(UInt(32.W)))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sinks</code>.</td>
    </tr>
    <tr>
      <td><code>e.EWire(gen)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Internal elastic wire:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire(UInt(32.W))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Usually emitted under graph <code>wires</code>; path follows the Chisel/Scala name, for example <code>/ewire0</code>.</td>
    </tr>
    <tr>
      <td><code>e.Source.like</code>, <code>e.Sink.like</code>, <code>e.EWire.like</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Preserve the payload type of an existing elastic interface:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire.like(source)</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Creates a typed elastic interface wire.</td>
    </tr>
    <tr>
      <td><code>e.Source.many</code>, <code>e.Sink.many</code>, <code>e.Interface.many</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Similar to the single-interface constructors, but returns <code>chext.util.NamedVec[e.Interface[T]]</code>:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sources = IO(e.Source.many(4, UInt(32.W)))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> <code>NamedVec</code> naming strictly uses underscores, for example <code>/sources_0</code>, <code>/sources_1</code>.</td>
    </tr>
    <tr>
      <td><code>source :=&gt; sink</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect elastic interfaces:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; sink</code></pre></td>
      <td>Creates <code>Connect</code>; <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connect</span>.</td>
    </tr>
    <tr>
      <td><code>sources :=&gt; sinks</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect matching sequences:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">sources :=&gt; sinks</code></pre></td>
      <td>Creates one <code>Connect</code> per pair; <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr>
      <td><code>e.Zip(&hellip;)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Zip.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Join sources into a bundle-valued elastic interface:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val zipped = e.Zip(sourceA, sourceB)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Not its own graph node; internally creates join-style wiring and a result wire.</td>
    </tr>
  </tbody>
</table>

## Elastic Components

Many elastic components support <code>fire { &hellip; }</code>, a protected hook that runs when the component's sink interface fires. The <code>Stall</code> row shows the pattern.

Buffer naming convention: <code>SourceBuffer</code>, <code>SinkBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used inline in a connection expression. <code>SourceBuffered</code> and <code>SinkBuffered</code> return a buffered interface directly; bind them to a <code>val</code> when the buffered interface is the thing you want to keep using.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>e.Connect</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Direct pass-through connection. Prefer <code>source :=&gt; sink</code> unless a named value is useful:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val connect0 = new e.Connect(source, sink)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Connect</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: connect</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Transform</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transform.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Combinational payload transform:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val transform0 = new e.Transform(source, sink) {
  out := f(in)
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Transform</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: transform</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Stall</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Stall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Conditionally hold a token; supports <code>fire</code>:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stall0 = new e.Stall(source, sink) {
  out := in
  cond { shouldStall }
  fire { didPass := true.B }
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Stall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: stall</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Drop</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Drop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Conditionally drop a token:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val drop0 = new e.Drop(source, sink) {
  out := in
  cond { shouldDrop }
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Drop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: drop</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Transducer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transducer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Transducer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Transducer.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Stateful packet-level control:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val transducer0 = new e.Transducer(source, sink) {
  packet {
    when(done) {
      consume { state := 0.U }
    }.otherwise {
      accept { out := in }
    }
  }
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Transducer</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: transducer</span> Ports: <code>source</code>, <code>sink</code>. Actions: <code>stall</code>, <code>accept</code>, <code>consume</code>, <code>produce</code>.</td>
    </tr>
    <tr>
      <td><code>e.Queue</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Queue.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Queue.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Explicit queue between existing endpoints:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val queue0 =
  new e.Queue(source, sink, count = 2)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: queue</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.SourceBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Insert a queue after a source, usually inline in a connection:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.SourceBuffer(source, count = 2) :=&gt; sink</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sourceBuffer</span>. Hierarchy: source -> <code>Queue</code> -> returned interface. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sourceBuffer0_queue0</code>.</td>
    </tr>
    <tr>
      <td><code>e.SinkBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Insert a queue before a sink, usually inline in a connection:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; e.SinkBuffer(sink, count = 2)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sinkBuffer</span>. Hierarchy: returned interface -> <code>Queue</code> -> sink. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sinkBuffer0_queue0</code>.</td>
    </tr>
    <tr>
      <td><code>e.LeftBuffer</code> / <code>e.RightBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Generic aliases for source-side / sink-side buffering:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.LeftBuffer(source) :=&gt; sink
source :=&gt; e.RightBuffer(sink)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same hierarchy as <code>SourceBuffer</code> / <code>SinkBuffer</code>; default prefixes are <code>leftBuffer</code> / <code>rightBuffer</code>.</td>
    </tr>
    <tr>
      <td><code>e.SourceBuffered</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Return a buffered source-side interface for reuse as a named value:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceBuffered0 =
  e.SourceBuffered(source, count = 2)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence form uses <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sourceBufferedMany</span>.</td>
    </tr>
    <tr>
      <td><code>e.SinkBuffered</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Return a buffered sink-side interface for reuse as a named value:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkBuffered0 =
  e.SinkBuffered(sink, count = 2)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence form uses <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sinkBufferedMany</span>.</td>
    </tr>
    <tr>
      <td><code>e.NullSink</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSink.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Consume/drop all tokens from a source:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSink0 =
  new e.NullSink(source)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSink</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSink</span> Port: <code>source</code>.</td>
    </tr>
    <tr>
      <td><code>e.NullSource</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSource.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Drive a sink with no valid tokens:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSource0 =
  new e.NullSource(sink)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSource</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSource</span> Port: <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Fork</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fork.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Branch one source to several sinks:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fork0 = new e.Fork(source) {
  fork(in.lo) :=&gt; sinkLo
  fork(in.hi) :=&gt; sinkHi
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fork</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fork</span> Ports: <code>source</code>, <code>sink_0</code>, <code>sink_1</code>, &hellip; The current implementation is eager, but the construction does not have to be conceptually limited to eager implementations.</td>
    </tr>
    <tr>
      <td><code>e.Join</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Join.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Join several sources into one sink:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Join</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: join</span> Ports: <code>source_0</code>, <code>source_1</code>, &hellip;, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Merger</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Merger.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Merge multiple sources into one sink:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val merger0 =
  new e.Merger(Seq(source0, source1), sink)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Merger</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: merger</span> Ports: <code>source_i</code>, <code>sink</code>. Requires a strong external guarantee that at most one input is active at a given time.</td>
    </tr>
    <tr>
      <td><code>e.Mux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Select one source by an elastic select stream:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 =
  new e.Mux(sources, sink, sourceSelect)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Mux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: mux</span> Ports: <code>source_i</code>, <code>sourceSelect</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Demux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Route one source to selected sink:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 =
  new e.Demux(source, sinks, sourceSelect)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Demux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demux</span> Ports: <code>source</code>, <code>sourceSelect</code>, <code>sink_i</code>.</td>
    </tr>
    <tr>
      <td><code>e.Arbiter</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Arbiter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Arbiter.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Arbiter.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Select among sources and emit the selected index:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.Arbiter(sources, sink, sinkSelect, e.Chooser.rr)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Arbiter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>, <code>sinkSelect</code>.</td>
    </tr>
    <tr>
      <td><code>e.ArbiterNs</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/ArbiterNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Select among sources without a selected-index stream:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.ArbiterNs(sources, sink, e.Chooser.rr)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: ArbiterNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.DemuxNs</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/DemuxNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Route by a protected select function:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = new e.DemuxNs(source, sinks) {
  select { in =&gt; in.index }
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: DemuxNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demuxNs</span> Ports: <code>source</code>, <code>sink_i</code>.</td>
    </tr>
    <tr>
      <td><code>e.Count</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Stateful repeat/count primitive:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in =&gt; 0.U }
  cond { (in, state) =&gt; state =/= in.limit }
  next { (in, state) =&gt; state + 1.U }
  out { (in, state, first, last) =&gt; in }
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Count</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: count</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr>
      <td>Self-alias form<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Useful when nesting components and you want a stable name for the outer anonymous instance:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) { count0 =&gt;
  count0.init { in =&gt; 0.U }
}</code></pre></td>
      <td>Scala construction style only; graph behavior is unchanged.</td>
    </tr>
    <tr>
      <td><code>e.Once</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>One-shot source, explicit component form:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkOnce = e.EWire(UInt(8.W))
val once0 = new e.Once(sinkOnce) {
  out := value
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Once</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: once</span> Port: <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Once(value)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Functional one-shot form:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceOnce = e.Once(value)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Once</code> component named <code>once0</code> by local val.</td>
    </tr>
    <tr>
      <td><code>e.Counter</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Explicit component form:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkCounter = e.EWire(UInt(8.W))
val counter0 = new e.Counter(sinkCounter)</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Counter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: counter</span> Port: <code>sink</code>.</td>
    </tr>
    <tr>
      <td><code>e.Counter(&hellip;)</code> / <code>e.Counter.fromWidth(&hellip;)</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Functional source form:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceCounter =
  e.Counter(maxValueExclusive = 16)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Counter</code> component named <code>counter0</code> by local val.</td>
    </tr>
    <tr>
      <td><code>e.Wrap</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Wrap.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Wrap delayed logic:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val wrap0 = new e.Wrap(source, sink) {
  protected def delay = 2
  out := f(in)
}</code></pre></td>
      <td><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Wrap</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: wrap</span> Nonzero delay internally creates <code>Counter</code>, <code>Queue</code>, and <code>Connect</code>.</td>
    </tr>
  </tbody>
</table>

## Elastic Containers

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>e.Repeat</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Repeat.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Repeat one input token multiple times:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in =&gt; in.length }
  out { (in, index, first, last) =&gt; in }
}</code></pre></td>
      <td><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Repeat</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: repeat</span>. Hierarchy: creates a child <code>Count</code> under <code>withContainer(this)</code>, with observed path shape <code>/repeat0</code> -> <code>/repeat0_count</code>.</td>
    </tr>
    <tr>
      <td><code>e.Fold</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fold.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Fold.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Fold.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Reduce a stream using user-provided fold logic:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in =&gt; in.data }
  last { in =&gt; in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}</code></pre></td>
      <td><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fold</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fold</span>. User-facing internal wires: <code>sinkA</code>, <code>sinkB</code>, <code>sourceResult</code>. Hierarchy: stage wiring under <code>stage0</code> and <code>stage1</code>; children include <code>Transducer</code> or <code>Transform</code>, <code>Join</code>, <code>Connect</code>, and buffers attached to the <code>Fold</code> container.</td>
    </tr>
    <tr>
      <td><code>e.Loop</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Loop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Iterative elastic loop with user-visible body endpoints:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state =&gt; state.done }
  sinkCurrent :=&gt; loopBody.source
  loopBody.sink :=&gt; sourceNext
}</code></pre></td>
      <td><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Loop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: loop</span>. User-facing internal wires: <code>sinkCurrent</code>, <code>sourceNext</code>. Hierarchy: creates <code>Stall</code>, <code>Connect</code>, <code>Fork</code>, <code>Demux</code>, <code>Merger</code>, and buffers attached to the <code>Loop</code> container.</td>
    </tr>
    <tr>
      <td><code>e.Scope</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Scope.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Begin/end scoped elastic region:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in =&gt; started := true.B }
  exit { out =&gt; finished := true.B }
  sinkBegin :=&gt; body.source
  body.sink :=&gt; sourceEnd
}</code></pre></td>
      <td><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Scope</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: scope</span>. User-facing internal wires: <code>sinkBegin</code>, <code>sourceEnd</code>. Hierarchy: creates a <code>Stall</code>, a <code>Connect</code>, and sink buffers attached to the <code>Scope</code> container.</td>
    </tr>
    <tr>
      <td><code>e.RandomStall</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/RandomStall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Test/diagnostic random backpressure:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val randomStall0 =
  new e.RandomStall(source, sink)</code></pre></td>
      <td><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: RandomStall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: randomStall</span>. Hierarchy: creates <code>Stall</code> plus <code>SinkBuffer</code>; observed child paths include <code>/randomStall0_stall</code> and <code>/randomStall0_stall_sinkBuffer0_queue0</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Imports

AXI examples assume:

```scala
import chext.amba.axi4
import axi4.{full => axi4f, lite => axi4l}
import axi4.ConnectOp._
```

## AXI4 Interfaces and Connects

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>axi4.Config</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Configuration:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val axiCfg = axi4.Config(wAddr = 32, wData = 64)</code></pre></td>
      <td>Configuration only.</td>
    </tr>
    <tr>
      <td><code>axi4f.Slave</code> / <code>axi4f.Master</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Full AXI IO:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi = IO(axi4f.Slave(axiCfg))
val m_axi = IO(axi4f.Master(axiCfg))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr>
      <td><code>axi4l.Slave</code> / <code>axi4l.Master</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>AXI4-Lite IO:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil = IO(axi4l.Slave(axiCfg))
val m_axil = IO(axi4l.Master(axiCfg))</code></pre></td>
      <td><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr>
      <td>Full single connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Connect one full AXI interface pair:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi :=&gt; m_axi</code></pre></td>
      <td>Creates <code>axi4f.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4f_connect</span>; graph <code>tpe</code> is <code>Axi4f_Connect</code>.</td>
    </tr>
    <tr>
      <td>Full configured connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Same, with diagnostics/tie-off config:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi.connect(m_axi, axi4f.ConnectConfig())</code></pre></td>
      <td>Same component and prefix as full single connect.</td>
    </tr>
    <tr>
      <td>Full many connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect matching full AXI sequences:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_N :=&gt; m_axi_N</code></pre></td>
      <td>Creates one full connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr>
      <td>Lite single connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Connect one AXI4-Lite pair:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil :=&gt; m_axil</code></pre></td>
      <td>Creates <code>axi4l.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4l_connect</span>; graph <code>tpe</code> is <code>Axi4l_Connect</code>.</td>
    </tr>
    <tr>
      <td>Lite configured connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Same, with diagnostics/tie-off config:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil.connect(m_axil, axi4l.ConnectConfig())</code></pre></td>
      <td>Same component and prefix as lite single connect.</td>
    </tr>
    <tr>
      <td>Lite many connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect matching AXI4-Lite sequences:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil_N :=&gt; m_axil_N</code></pre></td>
      <td>Creates one lite connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes.</td>
    </tr>
    <tr>
      <td>Raw single connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect raw AXI interfaces and dispatch to full/lite based on config:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw :=&gt; m_axi_raw</code></pre></td>
      <td>Dispatch helper; creates full or lite connect components depending on interface config.</td>
    </tr>
    <tr>
      <td>Raw many connect<div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect matching raw AXI sequences:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw_N :=&gt; m_axi_raw_N</code></pre></td>
      <td>Dispatch helper; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Buffers

AXI buffer naming follows the elastic convention: <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used anonymously inside a connection expression. <code>SlaveBuffered</code> and <code>MasterBuffered</code> return an interface directly; bind them to a <code>val</code> when the buffered AXI side is reused.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>axi4f.SlaveBuffer</code> / <code>axi4f.MasterBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Buffer a full AXI side inline in a connection:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4f.SlaveBuffer(s_axi, bufferCfg) :=&gt; m_axi
s_axi :=&gt; axi4f.MasterBuffer(m_axi, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. Read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr>
      <td><code>axi4f.SlaveBuffered</code> / <code>axi4f.MasterBuffered</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Create a buffered full AXI side for reuse as a named value:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>slaveBufferedMany</code> / <code>masterBufferedMany</code>.</td>
    </tr>
    <tr>
      <td><code>axi4l.SlaveBuffer</code> / <code>axi4l.MasterBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Buffer an AXI4-Lite side inline in a connection:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4l.SlaveBuffer(s_axil, bufferCfg) :=&gt; m_axil
s_axil :=&gt; axi4l.MasterBuffer(m_axil, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. AXI4-Lite read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr>
      <td><code>axi4l.SlaveBuffered</code> / <code>axi4l.MasterBuffered</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Create a buffered AXI4-Lite side for reuse as a named value:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>slaveBufferedMany</code> / <code>masterBufferedMany</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Full Components

AXI4 full components are Chisel modules. They are not Chext components themselves; graph content comes from the elastic components they instantiate internally.

Typical config shapes:
<pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fullCfg = axi4.Config(
  wAddr = 32,        // byte address width
  wData = 64,        // data bus width
  wId = 4,           // ID width for full AXI
  read = true,
  write = true,
  lite = false
)

val demuxCfg = axi4f.components.DemuxConfig(
  axiSlaveCfg = fullCfg,
  numMasters = 4,                // number of output ports
  decodeFn = addr =&gt; addr(13, 12) // address-to-port mapping
)

val widthCfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0),
  wDataMaster = 32,              // output data width
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre>

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>axi4f.components.Demux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Fan one full AXI slave-side port out to multiple master-side ports:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Demux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-out by address/routing selection. Internally uses channel-level elastic wiring around <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, and <code>b</code>.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.Mux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Merge multiple full AXI slave-side ports into one master-side port:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Mux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-in. Internally creates arbitration and channel-level elastic wiring for read and write paths.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.IdDemux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Route full AXI traffic by transaction ID:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.IdDemux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-based full AXI fan-out. Internally tracks transactions and expands to elastic channel components.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.IdMux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Merge full AXI traffic while preserving ID-based response routing:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.IdMux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-aware full AXI fan-in. Internally uses transaction tracking and channel-level elastic components.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.IdSerialize</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdSerialize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdSerialize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Serialize transactions by ID:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.IdSerialize(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID serialization. Internally limits outstanding work and routes response channels through elastic control logic.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.IdParallelize</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdParallelize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdParallelize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Parallelize transactions by ID:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.IdParallelize(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID parallelization. Internally distributes requests and rejoins responses with elastic channel logic.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.Upscale</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Upscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Upscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Upscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Adapt a narrower full AXI data bus to a wider one:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Upscale(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width upscaler. Internally adapts read/write data channels and keeps address/control channels aligned.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.Downscale</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Adapt a wider full AXI data bus to a narrower one:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Downscale(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width downscaler. Internally splits wider beats and coordinates read/write data flow through elastic logic.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.Unburst</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Unburst.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Unburst.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Convert burst transactions into single-beat transactions:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Unburst(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI burst decomposition. Internally generates per-beat addresses and coordinates response/data channels.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.Widen</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Widen.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Widen.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Widen.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Bridge full AXI width differences with widening behavior:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.Widen(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI data widening helper. Internally coordinates address, strobe, data, and response handling through channel logic.</td>
    </tr>
    <tr>
      <td><code>axi4f.components.ProtocolConverter</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Convert between supported full AXI protocol shapes:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4f.components.ProtocolConverter(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Staged full AXI protocol conversion. It composes lower-level full AXI component modules and their internal elastic channel components.</td>
    </tr>
  </tbody>
</table>

## AXI4 Lite Components

AXI4-Lite components are Chisel modules. They are not Chext components themselves; graph content comes from the internal channel-level elastic components.

Typical config shapes:
<pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val liteCfg = axi4.Config(
  wAddr = 32,   // byte address width
  wData = 32,   // data bus width
  lite = true,
  read = true,
  write = true
)

val demuxCfg = axi4l.components.DemuxConfig(
  axiSlaveCfg = liteCfg,
  numMasters = 4,                // number of output ports
  decodeFn = addr =&gt; addr(13, 12) // address-to-port mapping
)

Module(new axi4l.components.MemController(
  log2numElements = 10, // 1024 words
  axiCfg = liteCfg,
  debugEnabled = false
))</code></pre>

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>axi4l.components.Demux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Fan one AXI4-Lite slave-side port out to multiple master-side ports:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4l.components.Demux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-out for address/data/control channels.</td>
    </tr>
    <tr>
      <td><code>axi4l.components.Mux</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Merge multiple AXI4-Lite slave-side ports into one master-side port:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4l.components.Mux(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-in with channel-level elastic arbitration and routing.</td>
    </tr>
    <tr>
      <td><code>axi4l.components.CreditBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Add response-aware buffering for AXI4-Lite:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4l.components.CreditBuffer(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite response buffering with credit-style control around read/write channels.</td>
    </tr>
    <tr>
      <td><code>axi4l.components.MemController</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/MemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Expose a memory-like backend as AXI4-Lite:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4l.components.MemController(
  log2numElements = 10, // 1024 words
  axiCfg = liteCfg,
  debugEnabled = false
))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite memory controller. Internally bridges AXI4-Lite channels to memory-style request and response paths.</td>
    </tr>
    <tr>
      <td><code>axi4l.components.SyncReadMemController</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/SyncReadMemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Expose a synchronous-read memory backend as AXI4-Lite:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">Module(new axi4l.components.SyncReadMemController(
  log2numElements = 10, // 1024 words
  axiCfg = liteCfg,
  debugEnabled = false
))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite controller variant for synchronous-read memories.</td>
    </tr>
    <tr>
      <td><code>axi4l.components.RegisterBlock</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/RegisterBlock.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Build an AXI4-Lite register block:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val regs = new axi4l.components.RegisterBlock(
  wAddr = 32, // AXI4-Lite address width
  wData = 32, // AXI4-Lite data width
  wMask = 12  // register address-space mask
)
val s_axil = IO(axi4l.Slave(regs.cfgAxi))
s_axil :=&gt; regs.s_axil</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite register block module with generated read/write register access behavior.</td>
    </tr>
  </tbody>
</table>

## Memory

Memory interfaces are bundle-level protocols whose request/response fields are elastic interfaces. RAM wrappers are Chisel modules; their graph visibility mostly comes from the elastic ports and components they instantiate or connect.

Memory buffer naming follows the same convention as elastic and AXI: <code>SlaveBuffer</code> / <code>MasterBuffer</code> are the named-prefix inline forms, while <code>SlaveBuffered</code> / <code>MasterBuffered</code> are for binding the returned interface to a <code>val</code>.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>memory.ReadInterface</code>, <code>memory.WriteInterface</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Interfaces.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Memory read/write protocol bundles.</td>
      <td>Request/response bundle interfaces for memory-like ports. Their <code>req</code>/<code>resp</code> fields are elastic interfaces and can appear in elastic graph layers.</td>
    </tr>
    <tr>
      <td><code>memory.ConnectOp._</code> / <code>master :=&gt; slave</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Connect memory interfaces or sequences.</td>
      <td>Creates elastic <code>Connect</code> components on request/response channels; sequence form uses <code>connectMany</code>.</td>
    </tr>
    <tr>
      <td><code>memory.SlaveBuffer</code>, <code>memory.MasterBuffer</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Buffer memory interfaces inline:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig(
  req = 2,  // request-channel queue depth
  resp = 2  // response-channel queue depth
)
memory.SlaveBuffer(readMaster, bufferCfg) :=&gt; readSlave
writeMaster :=&gt; memory.MasterBuffer(writeSlave, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates elastic <code>SourceBuffer</code> and <code>SinkBuffer</code> on <code>req</code>/<code>resp</code> channels.</td>
    </tr>
    <tr>
      <td><code>memory.SlaveBuffered</code>, <code>memory.MasterBuffered</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td>Create a buffered memory interface for reuse as a named value:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig.all(2) // req and resp depth
val readBuffered = memory.SlaveBuffered(readMaster, bufferCfg)
val writeBuffered = memory.MasterBuffered(writeSlave, bufferCfg)</code></pre></td>
      <td><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same request/response buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface form does not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence forms use <code>leftBufferedMany</code> / <code>rightBufferedMany</code>.</td>
    </tr>
    <tr>
      <td><code>memory.SinglePortRAM</code>, <code>SimpleDualPortRAM</code>, <code>TrueDualPortRAM</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/RAM.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Memory wrappers around raw memory targets:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val rawCfg = memory.RawMemConfig(
  wAddr = 10,       // 1024 addressable elements
  wData = 32,       // 32-bit data
  latencyRead = 2,  // read response latency
  latencyWrite = 1  // write response latency
)
val portCfg = memory.PortConfig(
  numOutstandingRead = 8,
  numOutstandingWrite = 8
)
Module(new memory.TrueDualPortRAM(rawCfg, portCfg, portCfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> RAM wrappers with elastic read/write ports and raw memory bridges. Not tracking components; elastic ports participate when connected.</td>
    </tr>
  </tbody>
</table>

## Stream

Stream modules create AXI-facing work/result frontends. They are Chisel modules; graph visibility comes from their task/data/result elastic channels and internal elastic control components.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>stream.Chunk</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Chunk.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Split a variable-length stream request into AXI-sized chunks:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = stream.ChunkConfig(
  wAddress = axiCfg.wAddr, // address width
  wLength = 32,            // request length width
  wData = axiCfg.wData,    // data bus width
  maxBurstLength = 256     // maximum AXI burst length
)
Module(new stream.Chunk(cfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Chunks stream requests/results by repeatedly emitting per-beat work. Internally creates an elastic <code>Count</code>.</td>
    </tr>
    <tr>
      <td><code>stream.Read</code>, <code>stream.Write</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/stream/Read.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../src/test/scala/chext/stream/Write.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Read.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Write.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>AXI stream read/write frontends:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val readCfg = stream.ReadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  resultMode = stream.ReadResultMode.LastAlwaysInvalid,
  maxBurstLength = 256,
  numOutstandingTasks = 8
)
val writeCfg = stream.WriteConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  resultMode = stream.WriteResultMode.KeepAll,
  maxBurstLength = 256,
  numOutstandingTasks = 8
)
Module(new stream.Read(readCfg))
Module(new stream.Write(writeCfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI stream read/write engines. They create task/data/result elastic channels and internally use <code>Drop</code>, <code>Fork</code>, <code>Transform</code>, <code>Repeat</code>, <code>Join</code>, <code>Mux</code>, buffers, and null channel components.</td>
    </tr>
  </tbody>
</table>

## Load/Store

Load/store modules are single-beat AXI full frontends. They expose elastic task/result interfaces and a full AXI master port, then build the necessary AXI channel traffic internally.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>ldstr.Load</code>, <code>ldstr.Store</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td>Load/store AXI frontends:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loadCfg = ldstr.LoadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  numOutstandingTasks = 8 // read-side buffering
)
val storeCfg = ldstr.StoreConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  numOutstandingTasks = 8 // write response buffering
)
Module(new ldstr.Load(loadCfg))
Module(new ldstr.Store(storeCfg))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Load/store frontends for AXI memory access. They internally use elastic <code>Fork</code>, <code>Transform</code>, <code>SinkBuffer</code>, <code>Join</code>, and null channel components around AXI <code>ar/r</code> or <code>aw/w/b</code> paths.</td>
    </tr>
  </tbody>
</table>

## Float

Floating-point elastic modules wrap floating-point datapaths with elastic source/sink interfaces. Their graph participation comes from the wrapper queues, joins, and surrounding elastic wiring.

<table>
  <thead>
    <tr>
      <th>Construct</th>
      <th>Intent and Usage</th>
      <th>Details</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>float.ElasticAdd</code>, <code>float.ElasticMultiply</code><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/float/Elastic.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/float/Elastic.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/float/src/ElasticTop.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td>Elastic floating-point operators:<br><pre style="white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fp = float.FloatingPoint.ieee_fp32
Module(new float.ElasticAdd(fp, combinational = false))
Module(new float.ElasticMultiply(fp, combinational = false))</code></pre></td>
      <td><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Elastic wrappers around floating-point add/multiply datapaths. They expose elastic input/output interfaces; graph participation comes through surrounding elastic wiring and transforms.</td>
    </tr>
  </tbody>
</table>
