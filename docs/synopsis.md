<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->
# Chext Synopsis

This is a working synopsis of the main Chext constructions, their intended use, and how they appear in the tracking/module graph. For the underlying component model, see [tracking.md](tracking.md). For elastic-specific tracking and AXI4 DataView recovery, see [tracking-elastic.md](tracking-elastic.md). For detailed AXI4 component guides, see [axi4-full.md](axi4-full.md) and [axi4-lite.md](axi4-lite.md).

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

<code>tpe</code> is the graph type. <code>namePrefix</code> is the construction family's conceptual default prefix. Actual paths come from Chisel naming, <code>prefix(&hellip;)</code>, and <code>uniquePrefix(&hellip;)</code>; tracking warns when the latest prefix does not recognizably start with <code>namePrefix</code>.

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
    <tr id="entry-elastic-interfaces-e-interface">
      <td style="vertical-align:top;"><code>e.Interface(gen)</code> <a href="#entry-elastic-interfaces-e-interface" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-interface">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">For Chisel types:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val genElastic = e.Interface(UInt(32.W))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Not a component. When materialized as hardware and tracked, appears as <code>chext.elastic.Interface[&hellip;]</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-source">
      <td style="vertical-align:top;"><code>e.Source(gen)</code> <a href="#entry-elastic-interfaces-e-source" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-source">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Producer endpoint from the module's perspective:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val source = IO(e.Source(UInt(32.W)))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sources</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-sink">
      <td style="vertical-align:top;"><code>e.Sink(gen)</code> <a href="#entry-elastic-interfaces-e-sink" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-sink">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Consumer endpoint from the module's perspective:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sink = IO(e.Sink(UInt(32.W)))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Root IO appears under graph <code>sinks</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-ewire">
      <td style="vertical-align:top;"><code>e.EWire(gen)</code> <a href="#entry-elastic-interfaces-e-ewire" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-ewire">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Internal elastic wire:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire(UInt(32.W))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Usually emitted under graph <code>wires</code>; path follows the Chisel/Scala name, for example <code>/ewire0</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-source-like-e-sink-like-e-ewire-like-e-source-manylike-e-sink-manylike-e-ewire-manylike">
      <td style="vertical-align:top;"><code>e.Source.like</code>, <code>e.Sink.like</code>, <code>e.EWire.like</code>, <code>e.Source.manyLike</code>, <code>e.Sink.manyLike</code>, <code>e.EWire.manyLike</code> <a href="#entry-elastic-interfaces-e-source-like-e-sink-like-e-ewire-like-e-source-manylike-e-sink-manylike-e-ewire-manylike" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-source-like-e-sink-like-e-ewire-like-e-source-manylike-e-sink-manylike-e-ewire-manylike">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Preserve the payload type of an existing elastic interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val ewire0 = e.EWire.like(source)
val wires = e.EWire.manyLike(4, source)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Creates typed elastic interfaces from existing hardware. <code>e.EWire.like</code> and <code>e.EWire.manyLike</code> return internal wires.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-source-many-e-sink-many-e-interface-many-e-ewire-many">
      <td style="vertical-align:top;"><code>e.Source.many</code>, <code>e.Sink.many</code>, <code>e.Interface.many</code>, <code>e.EWire.many</code> <a href="#entry-elastic-interfaces-e-source-many-e-sink-many-e-interface-many-e-ewire-many" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-source-many-e-sink-many-e-interface-many-e-ewire-many">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Similar to the single-interface constructors, but returns <code>chext.util.NamedVec[e.Interface[T]]</code>. <code>e.EWire.many</code> returns an internal <code>Wire</code> of that shape:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sources = IO(e.Source.many(4, UInt(32.W)))
val wires = e.EWire.many(4, UInt(32.W))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> <code>NamedVec</code> naming strictly uses underscores, for example <code>/sources_0</code>, <code>/sources_1</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-source-connect-sink">
      <td style="vertical-align:top;"><code>source :=&gt; sink</code> <a href="#entry-elastic-interfaces-source-connect-sink" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-source-connect-sink">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect elastic interfaces:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; sink</code></pre></td>
      <td style="vertical-align:top;">Creates <code>Connect</code>; payload uses Chisel's standard <code>:=</code> connection and name-based aggregate field matching; no user-defined transformation is applied. <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connect</span>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-sources-connect-sinks">
      <td style="vertical-align:top;"><code>sources :=&gt; sinks</code> <a href="#entry-elastic-interfaces-sources-connect-sinks" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-sources-connect-sinks">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">sources :=&gt; sinks</code></pre></td>
      <td style="vertical-align:top;">Creates one <code>Connect</code> per pair; <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: connectMany</span> with per-index prefixes and an innermost <code>connect0</code> value prefix, for example <code>connectMany0_0_connect0</code>.</td>
    </tr>
    <tr id="entry-elastic-interfaces-e-zip">
      <td style="vertical-align:top;"><code>e.Zip(&hellip;)</code> <a href="#entry-elastic-interfaces-e-zip" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-e-zip">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Zip.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Join sources into a bundle-valued elastic interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val zipped = e.Zip(sourceA, sourceB)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Not its own graph node; internally creates join-style wiring and a result wire.</td>
    </tr>
  </tbody>
</table>

## Elastic Components

Many elastic components support <code>fire { &hellip; }</code>, a protected hook that runs when the component's sink interface fires. The <code>Stall</code> row shows the pattern.

Buffer naming convention: <code>SourceBuffer</code>, <code>SinkBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used inline in a connection expression. <code>SourceBuffered</code> and <code>SinkBuffered</code> return a buffered interface directly and do not add an internal <code>uniquePrefix</code>, including for sequence overloads; bind them to a <code>val</code> when the buffered interface is the thing you want to keep using.

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
    <tr id="entry-elastic-components-e-connect">
      <td style="vertical-align:top;"><code>e.Connect</code> <a href="#entry-elastic-components-e-connect" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Standard Chisel payload connection with no user-defined transformation. Prefer <code>source :=&gt; sink</code> unless a named value is useful:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val connect0 = new e.Connect(source, sink)</code></pre></td>
      <td style="vertical-align:top;">Uses <code>sink.bits := source.bits</code>, including Chisel's name-based matching for compatible aggregates. <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Connect</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: connect</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-transform">
      <td style="vertical-align:top;"><code>e.Transform</code> <a href="#entry-elastic-components-e-transform" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-transform">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transform.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Explicit combinational payload transformation:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val transform0 = new e.Transform(source, sink) {
  out := f(in)
}</code></pre></td>
      <td style="vertical-align:top;">Forwards ready/valid but does not connect the payload automatically; the body must assign protected <code>out</code> from protected <code>in</code>. <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Transform</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: transform</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-stall">
      <td style="vertical-align:top;"><code>e.Stall</code> <a href="#entry-elastic-components-e-stall" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-stall">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Stall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Conditionally hold a token; supports <code>fire</code>:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stall0 = new e.Stall(source, sink) {
  out := in
  cond { shouldStall }
  fire { didPass := true.B }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Stall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: stall</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-drop">
      <td style="vertical-align:top;"><code>e.Drop</code> <a href="#entry-elastic-components-e-drop" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-drop">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Drop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Conditionally drop a token:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val drop0 = new e.Drop(source, sink) {
  out := in
  cond { shouldDrop }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Drop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: drop</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-transducer">
      <td style="vertical-align:top;"><code>e.Transducer</code> <a href="#entry-elastic-components-e-transducer" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-transducer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Transducer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Transducer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Transducer.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
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
    <tr id="entry-elastic-components-e-queue">
      <td style="vertical-align:top;"><code>e.Queue</code> <a href="#entry-elastic-components-e-queue" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-queue">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Queue.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Queue.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Explicit queue between existing endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val queue0 =
  new e.Queue(source, sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: queue</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-source-buffer">
      <td style="vertical-align:top;"><code>e.SourceBuffer</code> <a href="#entry-elastic-components-e-source-buffer" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-source-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Insert a queue after a source, usually inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.SourceBuffer(source, count = 2) :=&gt; sink</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sourceBuffer</span>. Hierarchy: source -> <code>Queue</code> -> returned interface. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sourceBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-sink-buffer">
      <td style="vertical-align:top;"><code>e.SinkBuffer</code> <a href="#entry-elastic-components-e-sink-buffer" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-sink-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Insert a queue before a sink, usually inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">source :=&gt; e.SinkBuffer(sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a returned interface wrapped in <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: sinkBuffer</span>. Hierarchy: returned interface -> <code>Queue</code> -> sink. Underlying graph node is <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Queue</span>, often with a path like <code>/sinkBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-left-buffer-e-right-buffer">
      <td style="vertical-align:top;"><code>e.LeftBuffer</code> / <code>e.RightBuffer</code> <a href="#entry-elastic-components-e-left-buffer-e-right-buffer" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-left-buffer-e-right-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Generic aliases for source-side / sink-side buffering:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">e.LeftBuffer(source) :=&gt; sink
source :=&gt; e.RightBuffer(sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same hierarchy as <code>SourceBuffer</code> / <code>SinkBuffer</code>; default prefixes are <code>leftBuffer</code> / <code>rightBuffer</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-source-buffered">
      <td style="vertical-align:top;"><code>e.SourceBuffered</code> <a href="#entry-elastic-components-e-source-buffered" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-source-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Return a buffered source-side interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceBuffered0 =
  e.SourceBuffered(source, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface or sequence form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add only per-element index prefixes, for example <code>sourceBuffered_0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-sink-buffered">
      <td style="vertical-align:top;"><code>e.SinkBuffered</code> <a href="#entry-elastic-components-e-sink-buffered" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-sink-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/BufferedNaming.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Return a buffered sink-side interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkBuffered0 =
  e.SinkBuffered(sink, count = 2)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates a <code>Queue</code> but does not wrap the single-interface or sequence form in <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add only per-element index prefixes, for example <code>sinkBuffered_0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-null-sink">
      <td style="vertical-align:top;"><code>e.NullSink</code> <a href="#entry-elastic-components-e-null-sink" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-null-sink">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSink.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Consume/drop all tokens from a source:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSink0 =
  new e.NullSink(source)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSink</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSink</span> Port: <code>source</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-null-source">
      <td style="vertical-align:top;"><code>e.NullSource</code> <a href="#entry-elastic-components-e-null-source" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-null-source">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/NullSource.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Null.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Drive a sink with no valid tokens:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val nullSource0 =
  new e.NullSource(sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: NullSource</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: nullSource</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-fork">
      <td style="vertical-align:top;"><code>e.Fork</code> <a href="#entry-elastic-components-e-fork" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-fork">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fork.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Branch one source to several sinks:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fork0 = new e.Fork(source) {
  fork(in.lo) :=&gt; sinkLo
  fork(in.hi) :=&gt; sinkHi
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fork</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fork</span> Ports: <code>source</code>, <code>sink_0</code>, <code>sink_1</code>, &hellip; The current implementation is eager, but the construction does not have to be conceptually limited to eager implementations.</td>
    </tr>
    <tr id="entry-elastic-components-e-join">
      <td style="vertical-align:top;"><code>e.Join</code> <a href="#entry-elastic-components-e-join" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-join">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Join.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Join several sources into one sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Join</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: join</span> Ports: <code>source_0</code>, <code>source_1</code>, &hellip;, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-merger">
      <td style="vertical-align:top;"><code>e.Merger</code> <a href="#entry-elastic-components-e-merger" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-merger">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Merger.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple sources into one sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val merger0 =
  new e.Merger(Seq(source0, source1), sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Merger</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: merger</span> Ports: <code>source_i</code>, <code>sink</code>. Requires a strong external guarantee that at most one input is active at a given time.</td>
    </tr>
    <tr id="entry-elastic-components-e-mux">
      <td style="vertical-align:top;"><code>e.Mux</code> <a href="#entry-elastic-components-e-mux" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Select one source by an elastic select stream:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 =
  new e.Mux(sources, sink, sourceSelect)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Mux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: mux</span> Ports: <code>source_i</code>, <code>sourceSelect</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-demux">
      <td style="vertical-align:top;"><code>e.Demux</code> <a href="#entry-elastic-components-e-demux" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route one source to selected sink:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 =
  new e.Demux(source, sinks, sourceSelect)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Demux</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demux</span> Ports: <code>source</code>, <code>sourceSelect</code>, <code>sink_i</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-arbiter">
      <td style="vertical-align:top;"><code>e.Arbiter</code> <a href="#entry-elastic-components-e-arbiter" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-arbiter">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Arbiter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Arbiter.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Arbiter.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Select among sources and emit the selected index:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.Arbiter(sources, sink, sinkSelect, e.Chooser.rr)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Arbiter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>, <code>sinkSelect</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-arbiter-ns">
      <td style="vertical-align:top;"><code>e.ArbiterNs</code> <a href="#entry-elastic-components-e-arbiter-ns" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-arbiter-ns">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/ArbiterNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Select among sources without a selected-index stream:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val arbiter0 =
  new e.ArbiterNs(sources, sink, e.Chooser.rr)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: ArbiterNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: arbiter</span> Ports: <code>source_i</code>, <code>sink</code>. The no-select implementation shares the <code>arbiter</code> naming family.</td>
    </tr>
    <tr id="entry-elastic-components-e-demux-ns">
      <td style="vertical-align:top;"><code>e.DemuxNs</code> <a href="#entry-elastic-components-e-demux-ns" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-demux-ns">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/DemuxNs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route by a protected select function:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = new e.DemuxNs(source, sinks) {
  select { in =&gt; in.index }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: DemuxNs</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: demux</span> Ports: <code>source</code>, <code>sink_i</code>. The no-select implementation shares the <code>demux</code> naming family.</td>
    </tr>
    <tr id="entry-elastic-components-e-count">
      <td style="vertical-align:top;"><code>e.Count</code> <a href="#entry-elastic-components-e-count" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-count">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Stateful repeat/count primitive:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in =&gt; 0.U }
  cond { (in, state) =&gt; state =/= in.limit }
  next { (in, state) =&gt; state + 1.U }
  out { (in, state, first, last) =&gt; in }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Count</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: count</span> Ports: <code>source</code>, <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-self-alias-form">
      <td style="vertical-align:top;">Self-alias form <a href="#entry-elastic-components-self-alias-form" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-self-alias-form">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Count.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Useful when nesting components and you want a stable name for the outer anonymous instance:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val count0 = new e.Count(source, sink, UInt(8.W)) { count0 =&gt;
  count0.init { in =&gt; 0.U }
}</code></pre></td>
      <td style="vertical-align:top;">Scala construction style only; graph behavior is unchanged.</td>
    </tr>
    <tr id="entry-elastic-components-e-once">
      <td style="vertical-align:top;"><code>e.Once</code> <a href="#entry-elastic-components-e-once" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-once">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">One-shot source, explicit component form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkOnce = e.EWire(UInt(8.W))
val once0 = new e.Once(sinkOnce) {
  out := value
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Once</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: once</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-once-2">
      <td style="vertical-align:top;"><code>e.Once(value)</code> <a href="#entry-elastic-components-e-once-2" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-once-2">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Once.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Functional one-shot form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceOnce = e.Once(value)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Once</code> component named <code>once0</code> by local val.</td>
    </tr>
    <tr id="entry-elastic-components-e-counter">
      <td style="vertical-align:top;"><code>e.Counter</code> <a href="#entry-elastic-components-e-counter" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-counter">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Explicit component form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sinkCounter = e.EWire(UInt(8.W))
val counter0 = new e.Counter(sinkCounter)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Counter</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: counter</span> Port: <code>sink</code>.</td>
    </tr>
    <tr id="entry-elastic-components-e-counter-e-counter-from-width">
      <td style="vertical-align:top;"><code>e.Counter(&hellip;)</code> / <code>e.Counter.fromWidth(&hellip;)</code> <a href="#entry-elastic-components-e-counter-e-counter-from-width" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-counter-e-counter-from-width">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Counter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Functional source form:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val sourceCounter =
  e.Counter(maxValueExclusive = 16)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Creates an internal <code>EWire</code> and a child <code>Counter</code> component named <code>counter0</code> by local val.</td>
    </tr>
    <tr id="entry-elastic-components-e-wrap">
      <td style="vertical-align:top;"><code>e.Wrap</code> <a href="#entry-elastic-components-e-wrap" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-wrap">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Wrap.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-elastic-containers-e-repeat">
      <td style="vertical-align:top;"><code>e.Repeat</code> <a href="#entry-elastic-containers-e-repeat" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-e-repeat">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Repeat.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Repeat one input token multiple times:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in =&gt; in.length }
  out { (in, index, first, last) =&gt; in }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Repeat</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: repeat</span>. Hierarchy: creates a child <code>Count</code> under <code>withContainer(this)</code>, with observed path shape <code>/repeat0</code> -> <code>/repeat0_count</code>.</td>
    </tr>
    <tr id="entry-elastic-containers-e-fold">
      <td style="vertical-align:top;"><code>e.Fold</code> <a href="#entry-elastic-containers-e-fold" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-e-fold">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fold.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Fold.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Fold.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Reduce a stream using user-provided fold logic:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in =&gt; in.data }
  last { in =&gt; in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fold</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fold</span>. User-facing internal wires: <code>sinkA</code>, <code>sinkB</code>, <code>sourceResult</code>. Hierarchy: stage wiring under <code>stage0</code> and <code>stage1</code>; children include <code>Transducer</code> or <code>Transform</code>, <code>Join</code>, <code>Connect</code>, and buffers attached to the <code>Fold</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-e-loop">
      <td style="vertical-align:top;"><code>e.Loop</code> <a href="#entry-elastic-containers-e-loop" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-e-loop">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Loop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Iterative elastic loop with user-visible body endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state =&gt; state.done }
  sinkCurrent :=&gt; loopBody.source
  loopBody.sink :=&gt; sourceNext
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Loop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: loop</span>. User-facing internal wires: <code>sinkCurrent</code>, <code>sourceNext</code>. Hierarchy: creates <code>Stall</code>, <code>Connect</code>, <code>Fork</code>, <code>Demux</code>, <code>Merger</code>, and buffers attached to the <code>Loop</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-e-scope">
      <td style="vertical-align:top;"><code>e.Scope</code> <a href="#entry-elastic-containers-e-scope" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-e-scope">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Scope.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Begin/end scoped elastic region:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in =&gt; started := true.B }
  exit { out =&gt; finished := true.B }
  sinkBegin :=&gt; body.source
  body.sink :=&gt; sourceEnd
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#eaf7ea;color:#137333;padding:2px 6px;border-radius:4px;">Container</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Scope</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: scope</span>. User-facing internal wires: <code>sinkBegin</code>, <code>sourceEnd</code>. Hierarchy: creates a <code>Stall</code>, a <code>Connect</code>, and sink buffers attached to the <code>Scope</code> container.</td>
    </tr>
    <tr id="entry-elastic-containers-e-random-stall">
      <td style="vertical-align:top;"><code>e.RandomStall</code> <a href="#entry-elastic-containers-e-random-stall" style="text-decoration:none;" aria-label="Permalink to entry-elastic-containers-e-random-stall">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/RandomStall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi-connects-axi4-config">
      <td style="vertical-align:top;"><code>axi4.Config</code> <a href="#entry-axi-connects-axi4-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-axi4-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi-connects-full-connect-config">
      <td style="vertical-align:top;"><code>axi4f.ConnectConfig</code> <a href="#entry-axi-connects-full-connect-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-connect-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi-connects-lite-connect-config">
      <td style="vertical-align:top;"><code>axi4l.ConnectConfig</code> <a href="#entry-axi-connects-lite-connect-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-connect-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi-connects-full-slave-master">
      <td style="vertical-align:top;"><code>axi4f.Slave</code> / <code>axi4f.Master</code> <a href="#entry-axi-connects-full-slave-master" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-slave-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Full AXI IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi = IO(axi4f.Slave(axiCfg))
val m_axi = IO(axi4f.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr id="entry-axi-connects-lite-slave-master">
      <td style="vertical-align:top;"><code>axi4l.Slave</code> / <code>axi4l.Master</code> <a href="#entry-axi-connects-lite-slave-master" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-slave-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil = IO(axi4l.Slave(axiCfg))
val m_axil = IO(axi4l.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>.</td>
    </tr>
    <tr id="entry-axi-connects-full-single-connect">
      <td style="vertical-align:top;">Full single connect <a href="#entry-axi-connects-full-single-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-single-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one full AXI interface pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi :=&gt; m_axi</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4f.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4fConnect</span>; graph <code>tpe</code> is <code>Axi4f_Connect</code>.</td>
    </tr>
    <tr id="entry-axi-connects-full-configured-connect">
      <td style="vertical-align:top;">Full configured connect <a href="#entry-axi-connects-full-configured-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-configured-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Same, with diagnostics/tie-off config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi.connect(m_axi, axi4f.ConnectConfig())</code></pre></td>
      <td style="vertical-align:top;">Same component and prefix as full single connect.</td>
    </tr>
    <tr id="entry-axi-connects-full-many-connect">
      <td style="vertical-align:top;">Full many connect <a href="#entry-axi-connects-full-many-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-many-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching full AXI sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_N :=&gt; m_axi_N</code></pre></td>
      <td style="vertical-align:top;">Creates one full connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4fConnectMany</span> with per-index prefixes and an innermost <code>axi4fConnect0</code> value prefix, for example <code>axi4fConnectMany0_0_axi4fConnect0</code>.</td>
    </tr>
    <tr id="entry-axi-connects-lite-single-connect">
      <td style="vertical-align:top;">Lite single connect <a href="#entry-axi-connects-lite-single-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-single-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one AXI4-Lite pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil :=&gt; m_axil</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4l.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4lConnect</span>; graph <code>tpe</code> is <code>Axi4l_Connect</code>.</td>
    </tr>
    <tr id="entry-axi-connects-lite-configured-connect">
      <td style="vertical-align:top;">Lite configured connect <a href="#entry-axi-connects-lite-configured-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-configured-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Same, with diagnostics/tie-off config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil.connect(m_axil, axi4l.ConnectConfig())</code></pre></td>
      <td style="vertical-align:top;">Same component and prefix as lite single connect.</td>
    </tr>
    <tr id="entry-axi-connects-lite-many-connect">
      <td style="vertical-align:top;">Lite many connect <a href="#entry-axi-connects-lite-many-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-many-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching AXI4-Lite sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil_N :=&gt; m_axil_N</code></pre></td>
      <td style="vertical-align:top;">Creates one lite connect per pair; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4lConnectMany</span> with per-index prefixes and an innermost <code>axi4lConnect0</code> value prefix, for example <code>axi4lConnectMany0_0_axi4lConnect0</code>.</td>
    </tr>
    <tr id="entry-axi-connects-raw-single-connect">
      <td style="vertical-align:top;">Raw single connect <a href="#entry-axi-connects-raw-single-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-raw-single-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect raw AXI interfaces and dispatch to full/lite based on config:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw :=&gt; m_axi_raw</code></pre></td>
      <td style="vertical-align:top;">Dispatch helper; creates full or lite connect components depending on interface config.</td>
    </tr>
    <tr id="entry-axi-connects-raw-many-connect">
      <td style="vertical-align:top;">Raw many connect <a href="#entry-axi-connects-raw-many-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-raw-many-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect matching raw AXI sequences:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi_raw_N :=&gt; m_axi_raw_N</code></pre></td>
      <td style="vertical-align:top;">Dispatch helper; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4ConnectMany</span>. Each element receives an innermost <code>axi4fConnect0</code> or <code>axi4lConnect0</code> value prefix according to its interface configuration.</td>
    </tr>
  </tbody>
</table>

## AXI4 Buffers

AXI buffer naming follows the elastic convention: <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used anonymously inside a connection expression. <code>SlaveBuffered</code> and <code>MasterBuffered</code> return an interface directly and do not add an internal <code>uniquePrefix</code>, including for sequence overloads; bind them to a <code>val</code> when the buffered AXI side is reused.

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
    <tr id="entry-axi-buffers-axi4-buffer-config">
      <td style="vertical-align:top;"><code>axi4.BufferConfig</code> <a href="#entry-axi-buffers-axi4-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-axi4-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Per-channel buffer depths:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = axi4.BufferConfig(
  aw = 2, // write address
  w = 2,  // write data
  b = 2,  // write response
  ar = 2, // read address
  r = 2   // read data
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration shared by full and lite AXI buffer helpers. <code>axi4.BufferConfig.all(2)</code> applies the same depth to all five channels.</td>
    </tr>
    <tr id="entry-axi-buffers-full-slave-master-buffer">
      <td style="vertical-align:top;"><code>axi4f.SlaveBuffer</code> / <code>axi4f.MasterBuffer</code> <a href="#entry-axi-buffers-full-slave-master-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-full-slave-master-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer a full AXI side inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4f.SlaveBuffer(s_axi, bufferCfg) :=&gt; m_axi
s_axi :=&gt; axi4f.MasterBuffer(m_axi, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. Read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-full-slave-master-buffered">
      <td style="vertical-align:top;"><code>axi4f.SlaveBuffered</code> / <code>axi4f.MasterBuffered</code> <a href="#entry-axi-buffers-full-slave-master-buffered" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-full-slave-master-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered full AXI side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface and sequence forms do not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add only per-element index prefixes, for example <code>slaveBuffered_0_arBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-axi-buffers-lite-slave-master-buffer">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffer</code> / <code>axi4l.MasterBuffer</code> <a href="#entry-axi-buffers-lite-slave-master-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-lite-slave-master-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer an AXI4-Lite side inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4l.SlaveBuffer(s_axil, bufferCfg) :=&gt; m_axil
s_axil :=&gt; axi4l.MasterBuffer(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code>. AXI4-Lite read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-lite-slave-master-buffered">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffered</code> / <code>axi4l.MasterBuffered</code> <a href="#entry-axi-buffers-lite-slave-master-buffered" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-lite-slave-master-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered AXI4-Lite side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface and sequence forms do not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add only per-element index prefixes, for example <code>slaveBuffered_0_arBuffer0_queue0</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Full Components

AXI4 full components are Chisel modules. They are not Chext components themselves; graph content comes from the elastic components they instantiate internally. For the detailed guide and examples for every full AXI component/helper, see [axi4-full.md](axi4-full.md).

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
    <tr id="entry-axi4-full-components-demux-config">
      <td style="vertical-align:top;"><code>axi4f.components.DemuxConfig</code> <a href="#entry-axi4-full-components-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi4-full-components-demux">
      <td style="vertical-align:top;"><code>axi4f.components.Demux</code> <a href="#entry-axi4-full-components-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Fan one full AXI slave-side port out to multiple master-side ports:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4f.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-out by address/routing selection. Internally uses channel-level elastic wiring around <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, and <code>b</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.MuxConfig</code> <a href="#entry-axi4-full-components-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.MuxConfig(
  axiSlaveCfg = fullCfg,
  numSlaves = 4, // input ports
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Mux</code>; output IDs are widened to include the selected input port.</td>
    </tr>
    <tr id="entry-axi4-full-components-mux">
      <td style="vertical-align:top;"><code>axi4f.components.Mux</code> <a href="#entry-axi4-full-components-mux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple full AXI slave-side ports into one master-side port:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4f.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-in. Internally creates arbitration and channel-level elastic wiring for read and write paths.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-demux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemuxConfig</code> <a href="#entry-axi4-full-components-id-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdDemuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // routes by 2 ID bits
  capacityPortQueueW = 8,
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdDemux</code>; <code>wIdSel</code> determines the number of output ports as <code>1 &lt;&lt; wIdSel</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-demux">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemux</code> <a href="#entry-axi4-full-components-id-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route full AXI traffic by transaction ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idDemux0 = Module(new axi4f.components.IdDemux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-based full AXI fan-out. Internally tracks transactions and expands to elastic channel components.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdMuxConfig</code> <a href="#entry-axi4-full-components-id-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdMuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // input port bits in ID
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdMux</code>; response routing uses ID bits to recover the input port.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-mux">
      <td style="vertical-align:top;"><code>axi4f.components.IdMux</code> <a href="#entry-axi4-full-components-id-mux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge full AXI traffic while preserving ID-based response routing:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idMux0 = Module(new axi4f.components.IdMux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-aware full AXI fan-in. Internally uses transaction tracking and channel-level elastic components.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-serialize-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerializeConfig</code> <a href="#entry-axi4-full-components-id-serialize-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-serialize-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID serialization configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdSerializeConfig(
  axiSlaveCfg = fullCfg,
  numOutstandingRead = 4,
  numOutstandingWrite = 4,
  wIdSelect = 0
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdSerialize</code>; output ID width becomes zero.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-serialize">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerialize</code> <a href="#entry-axi4-full-components-id-serialize" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-serialize">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdSerialize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdSerialize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Serialize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idSerialize0 =
  Module(new axi4f.components.IdSerialize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID serialization. Internally limits outstanding work and routes response channels through elastic control logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-parallelize-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdParallelizeConfig</code> <a href="#entry-axi4-full-components-id-parallelize-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-parallelize-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID parallelization configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdParallelizeConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0),
  wIdMaster = 3,
  wBufferIndex = 10,
  readUseSyncMem = true,
  writeUseSyncMem = true
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdParallelize</code>; creates a wider-ID master side from a zero-ID slave side.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-parallelize">
      <td style="vertical-align:top;"><code>axi4f.components.IdParallelize</code> <a href="#entry-axi4-full-components-id-parallelize" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-parallelize">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdParallelize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdParallelize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Parallelize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idParallelize0 =
  Module(new axi4f.components.IdParallelize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID parallelization. Internally distributes requests and rejoins responses with elastic channel logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-upscale-config">
      <td style="vertical-align:top;"><code>axi4f.components.UpscaleConfig</code> <a href="#entry-axi4-full-components-upscale-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-upscale-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Upscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Width upscale configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.UpscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 32),
  wDataMaster = 64,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Upscale</code>; master data width must be wider than the slave data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-upscale">
      <td style="vertical-align:top;"><code>axi4f.components.Upscale</code> <a href="#entry-axi4-full-components-upscale" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-upscale">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Upscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Upscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Upscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Adapt a narrower full AXI data bus to a wider one:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val upscale0 = Module(new axi4f.components.Upscale(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width upscaler. Internally adapts read/write data channels and keeps address/control channels aligned.</td>
    </tr>
    <tr id="entry-axi4-full-components-downscale-config">
      <td style="vertical-align:top;"><code>axi4f.components.DownscaleConfig</code> <a href="#entry-axi4-full-components-downscale-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-downscale-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Width downscale configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Downscale</code>; master data width must be narrower than the slave data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-downscale">
      <td style="vertical-align:top;"><code>axi4f.components.Downscale</code> <a href="#entry-axi4-full-components-downscale" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-downscale">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Adapt a wider full AXI data bus to a narrower one:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val downscale0 = Module(new axi4f.components.Downscale(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width downscaler. Internally splits wider beats and coordinates read/write data flow through elastic logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-unburst-config">
      <td style="vertical-align:top;"><code>axi4f.components.UnburstConfig</code> <a href="#entry-axi4-full-components-unburst-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-unburst-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Burst decomposition configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.UnburstConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Unburst</code>; input and output AXI configs are the same, but bursts are decomposed internally.</td>
    </tr>
    <tr id="entry-axi4-full-components-unburst">
      <td style="vertical-align:top;"><code>axi4f.components.Unburst</code> <a href="#entry-axi4-full-components-unburst" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-unburst">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Unburst.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Unburst.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Convert burst transactions into single-beat transactions:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val unburst0 = Module(new axi4f.components.Unburst(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI burst decomposition. Internally generates per-beat addresses and coordinates response/data channels.</td>
    </tr>
    <tr id="entry-axi4-full-components-widen-config">
      <td style="vertical-align:top;"><code>axi4f.components.WidenConfig</code> <a href="#entry-axi4-full-components-widen-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-widen-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Widen.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Narrow-transfer widening configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.WidenConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Widen</code>; converts narrow transfers to full-sized transfers on the same AXI data width.</td>
    </tr>
    <tr id="entry-axi4-full-components-widen">
      <td style="vertical-align:top;"><code>axi4f.components.Widen</code> <a href="#entry-axi4-full-components-widen" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-widen">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Widen.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Widen.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Widen.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Bridge full AXI width differences with widening behavior:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val widen0 = Module(new axi4f.components.Widen(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI data widening helper. Internally coordinates address, strobe, data, and response handling through channel logic.</td>
    </tr>
    <tr id="entry-axi4-full-components-credit-buffer-config">
      <td style="vertical-align:top;"><code>axi4f.components.CreditBufferConfig</code> <a href="#entry-axi4-full-components-credit-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-credit-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Response-aware full AXI buffering configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.CreditBufferConfig(
  axiCfg = fullCfg,
  rBuffer = 32, // read response credits
  wBuffer = 8,  // write payload credits
  bBuffer = 32  // write response credits
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for full AXI <code>CreditBuffer</code>; address channels wait until local response/payload buffering has capacity.</td>
    </tr>
    <tr id="entry-axi4-full-components-credit-buffer">
      <td style="vertical-align:top;"><code>axi4f.components.CreditBuffer</code> <a href="#entry-axi4-full-components-credit-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-credit-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Add response-aware buffering for full AXI:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val creditBuffer0 =
  Module(new axi4f.components.CreditBuffer(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI credit-style response buffering around read/write channels.</td>
    </tr>
    <tr id="entry-axi4-full-components-protocol-converter-config">
      <td style="vertical-align:top;"><code>axi4f.components.ProtocolConverterConfig</code> <a href="#entry-axi4-full-components-protocol-converter-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-protocol-converter-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Staged protocol conversion configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.ProtocolConverterConfig(
  axiSlaveCfg = fullCfg.copy(wId = 4, wData = 32),
  axiMasterCfg = fullCfg.copy(wId = 2, wData = 64),
  slaveNeverBursts = false
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>ProtocolConverter</code>; internally selects ID, burst, and width conversion stages.</td>
    </tr>
    <tr id="entry-axi4-full-components-protocol-converter">
      <td style="vertical-align:top;"><code>axi4f.components.ProtocolConverter</code> <a href="#entry-axi4-full-components-protocol-converter" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-protocol-converter">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Convert between supported full AXI protocol shapes:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val protocolConverter0 =
  Module(new axi4f.components.ProtocolConverter(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Staged full AXI protocol conversion. It composes lower-level full AXI component modules and their internal elastic channel components.</td>
    </tr>
  </tbody>
</table>

## AXI4 Lite Components

AXI4-Lite components are Chisel modules. They are not Chext components themselves; graph content comes from the internal channel-level elastic components. For the detailed guide and examples for every Lite AXI component/helper, see [axi4-lite.md](axi4-lite.md).

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
    <tr id="entry-axi4-lite-components-demux-config">
      <td style="vertical-align:top;"><code>axi4l.components.DemuxConfig</code> <a href="#entry-axi4-lite-components-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi4-lite-components-demux">
      <td style="vertical-align:top;"><code>axi4l.components.Demux</code> <a href="#entry-axi4-lite-components-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan one AXI4-Lite slave-side port out to multiple master-side ports:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4l.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-out for address/data/control channels.</td>
    </tr>
    <tr id="entry-axi4-lite-components-mux-config">
      <td style="vertical-align:top;"><code>axi4l.components.MuxConfig</code> <a href="#entry-axi4-lite-components-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-axi4-lite-components-mux">
      <td style="vertical-align:top;"><code>axi4l.components.Mux</code> <a href="#entry-axi4-lite-components-mux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge multiple AXI4-Lite slave-side ports into one master-side port:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4l.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-in with channel-level elastic arbitration and routing.</td>
    </tr>
    <tr id="entry-axi4-lite-components-credit-buffer-config">
      <td style="vertical-align:top;"><code>axi4l.components.CreditBufferConfig</code> <a href="#entry-axi4-lite-components-credit-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-credit-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Response-aware buffering configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.CreditBufferConfig(
  axiCfg = liteCfg,
  rBuffer = 8, // read response credits
  wBuffer = 8, // write payload credits
  bBuffer = 8  // write response credits
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>CreditBuffer</code>; address channels are delayed until local response/payload buffering has capacity.</td>
    </tr>
    <tr id="entry-axi4-lite-components-credit-buffer">
      <td style="vertical-align:top;"><code>axi4l.components.CreditBuffer</code> <a href="#entry-axi4-lite-components-credit-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-credit-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Add response-aware buffering for AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val creditBuffer0 =
  Module(new axi4l.components.CreditBuffer(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite response buffering with credit-style control around read/write channels.</td>
    </tr>
    <tr id="entry-axi4-lite-components-mem-controller">
      <td style="vertical-align:top;"><code>axi4l.components.MemController</code> <a href="#entry-axi4-lite-components-mem-controller" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-mem-controller">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/MemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a memory-like backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val memController0 =
  Module(new axi4l.components.MemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite memory controller. Internally bridges AXI4-Lite channels to memory-style request and response paths.</td>
    </tr>
    <tr id="entry-axi4-lite-components-sync-read-mem-controller">
      <td style="vertical-align:top;"><code>axi4l.components.SyncReadMemController</code> <a href="#entry-axi4-lite-components-sync-read-mem-controller" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-sync-read-mem-controller">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/SyncReadMemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a synchronous-read memory backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val syncReadMemController0 =
  Module(new axi4l.components.SyncReadMemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite controller variant for synchronous-read memories.</td>
    </tr>
    <tr id="entry-axi4-lite-components-register-block">
      <td style="vertical-align:top;"><code>axi4l.components.RegisterBlock</code> <a href="#entry-axi4-lite-components-register-block" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-register-block">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/RegisterBlock.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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

Memory buffer naming follows the same convention as elastic and AXI: <code>SlaveBuffer</code> / <code>MasterBuffer</code> are the named-prefix inline forms, while <code>SlaveBuffered</code> / <code>MasterBuffered</code> are for binding the returned interface to a <code>val</code>. <code>SlaveBuffered</code> / <code>MasterBuffered</code> do not add an internal <code>uniquePrefix</code>, including for sequence overloads.

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
    <tr id="entry-memory-read-interface-memory-write-interface">
      <td style="vertical-align:top;"><code>memory.ReadInterface</code>, <code>memory.WriteInterface</code> <a href="#entry-memory-read-interface-memory-write-interface" style="text-decoration:none;" aria-label="Permalink to entry-memory-read-interface-memory-write-interface">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Interfaces.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory read/write protocol bundles.</td>
      <td style="vertical-align:top;">Request/response bundle interfaces for memory-like ports. Their <code>req</code>/<code>resp</code> fields are elastic interfaces and can appear in elastic graph layers.</td>
    </tr>
    <tr id="entry-memory-connect-op-master-connect-slave">
      <td style="vertical-align:top;"><code>memory.ConnectOp._</code> / <code>master :=&gt; slave</code> <a href="#entry-memory-connect-op-master-connect-slave" style="text-decoration:none;" aria-label="Permalink to entry-memory-connect-op-master-connect-slave">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Connect memory interfaces or sequences.</td>
      <td style="vertical-align:top;">Creates elastic <code>Connect</code> components on request/response channels; sequence forms use prefixes <code>memoryReadConnectMany</code> or <code>memoryWriteConnectMany</code>.</td>
    </tr>
    <tr id="entry-memory-buffer-config">
      <td style="vertical-align:top;"><code>memory.BufferConfig</code> <a href="#entry-memory-buffer-config" style="text-decoration:none;" aria-label="Permalink to entry-memory-buffer-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory interface buffer depths:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig(
  req = 2,  // request-channel queue depth
  resp = 2  // response-channel queue depth
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for memory <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>SlaveBuffered</code>, and <code>MasterBuffered</code>.</td>
    </tr>
    <tr id="entry-memory-slave-buffer-memory-master-buffer">
      <td style="vertical-align:top;"><code>memory.SlaveBuffer</code>, <code>memory.MasterBuffer</code> <a href="#entry-memory-slave-buffer-memory-master-buffer" style="text-decoration:none;" aria-label="Permalink to entry-memory-slave-buffer-memory-master-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer memory interfaces inline:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">memory.SlaveBuffer(readMaster, bufferCfg) :=&gt; readSlave
writeMaster :=&gt; memory.MasterBuffer(writeSlave, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates elastic <code>SourceBuffer</code> and <code>SinkBuffer</code> on <code>req</code>/<code>resp</code> channels.</td>
    </tr>
    <tr id="entry-memory-slave-buffered-memory-master-buffered">
      <td style="vertical-align:top;"><code>memory.SlaveBuffered</code>, <code>memory.MasterBuffered</code> <a href="#entry-memory-slave-buffered-memory-master-buffered" style="text-decoration:none;" aria-label="Permalink to entry-memory-slave-buffered-memory-master-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/memory/Buffer.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered memory interface for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val bufferCfg = memory.BufferConfig.all(2) // req and resp depth
val readBuffered = memory.SlaveBuffered(readMaster, bufferCfg)
val writeBuffered = memory.MasterBuffered(writeSlave, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same request/response buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface and sequence forms do not add a <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add only per-element index prefixes, for example <code>slaveBufferedRead_0_reqBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-memory-raw-mem-config-memory-port-config">
      <td style="vertical-align:top;"><code>memory.RawMemConfig</code> / <code>memory.PortConfig</code> <a href="#entry-memory-raw-mem-config-memory-port-config" style="text-decoration:none;" aria-label="Permalink to entry-memory-raw-mem-config-memory-port-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/RawMem.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/memory/RAM.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-memory-single-port-ram-simple-dual-port-ram-true-dual-port-ram">
      <td style="vertical-align:top;"><code>memory.SinglePortRAM</code>, <code>SimpleDualPortRAM</code>, <code>TrueDualPortRAM</code> <a href="#entry-memory-single-port-ram-simple-dual-port-ram-true-dual-port-ram" style="text-decoration:none;" aria-label="Permalink to entry-memory-single-port-ram-simple-dual-port-ram-true-dual-port-ram">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/memory/RAM.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-stream-chunk-config">
      <td style="vertical-align:top;"><code>stream.ChunkConfig</code> <a href="#entry-stream-chunk-config" style="text-decoration:none;" aria-label="Permalink to entry-stream-chunk-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Chunk.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Chunking configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = stream.ChunkConfig(
  wAddress = axiCfg.wAddr,
  wLength = 32,        // request length width
  wData = axiCfg.wData,
  maxBurstLength = 256,
  genUser = UInt(0.W)
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>stream.Chunk</code>; controls address width, length width, data width, and maximum emitted burst length.</td>
    </tr>
    <tr id="entry-stream-chunk">
      <td style="vertical-align:top;"><code>stream.Chunk</code> <a href="#entry-stream-chunk" style="text-decoration:none;" aria-label="Permalink to entry-stream-chunk">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Chunk.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Split a variable-length stream request into AXI-sized chunks:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val chunk0 = Module(new stream.Chunk(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Chunks stream requests/results by repeatedly emitting per-beat work. Internally creates an elastic <code>Count</code>.</td>
    </tr>
    <tr id="entry-stream-read-config-stream-write-config">
      <td style="vertical-align:top;"><code>stream.ReadConfig</code> / <code>stream.WriteConfig</code> <a href="#entry-stream-read-config-stream-write-config" style="text-decoration:none;" aria-label="Permalink to entry-stream-read-config-stream-write-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-stream-read-stream-write">
      <td style="vertical-align:top;"><code>stream.Read</code>, <code>stream.Write</code> <a href="#entry-stream-read-stream-write" style="text-decoration:none;" aria-label="Permalink to entry-stream-read-stream-write">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/stream/Read.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../src/test/scala/chext/stream/Write.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Read.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Write.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
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
    <tr id="entry-load-store-ldstr-load-config-ldstr-store-config">
      <td style="vertical-align:top;"><code>ldstr.LoadConfig</code> / <code>ldstr.StoreConfig</code> <a href="#entry-load-store-ldstr-load-config-ldstr-store-config" style="text-decoration:none;" aria-label="Permalink to entry-load-store-ldstr-load-config-ldstr-store-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-load-store-ldstr-load-ldstr-store">
      <td style="vertical-align:top;"><code>ldstr.Load</code>, <code>ldstr.Store</code> <a href="#entry-load-store-ldstr-load-ldstr-store" style="text-decoration:none;" aria-label="Permalink to entry-load-store-ldstr-load-ldstr-store">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
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
    <tr id="entry-float-floating-point">
      <td style="vertical-align:top;"><code>float.FloatingPoint</code> <a href="#entry-float-floating-point" style="text-decoration:none;" aria-label="Permalink to entry-float-floating-point">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/float/FloatingPoint.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Floating-point format configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fp = float.FloatingPoint.ieeeFp32
val custom = float.FloatingPoint(
  wExponent = 8,
  wMantissa = 23
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Bundle/config object for floating-point datapaths. Common helpers include <code>ieeeFp16</code>, <code>ieeeFp32</code>, <code>ieeeFp64</code>, <code>fp18</code>, and <code>bfloat16</code>.</td>
    </tr>
    <tr id="entry-float-elastic-add-float-elastic-multiply">
      <td style="vertical-align:top;"><code>float.ElasticAdd</code>, <code>float.ElasticMultiply</code> <a href="#entry-float-elastic-add-float-elastic-multiply" style="text-decoration:none;" aria-label="Permalink to entry-float-elastic-add-float-elastic-multiply">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/float/Elastic.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/float/Elastic.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/float/src/ElasticTop.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Elastic floating-point operators:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val add0 =
  Module(new float.ElasticAdd(fp, combinational = false))
val mul0 =
  Module(new float.ElasticMultiply(fp, combinational = false))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Elastic wrappers around floating-point add/multiply datapaths. They expose elastic input/output interfaces; graph participation comes through surrounding elastic wiring and transforms.</td>
    </tr>
  </tbody>
</table>
