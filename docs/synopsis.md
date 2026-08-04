<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->
# Chext Synopsis

This is a working synopsis of the main Chext constructions, their intended use, and how they appear in the tracking/module graph. For project-wide naming guidance, see [naming.md](naming.md). For the underlying component model, see [tracking.md](tracking.md). For guidance on choosing <code>prefix(&hellip;)</code> and <code>uniquePrefix(&hellip;)</code>, see [prefixes.md](prefixes.md). For elastic-specific tracking and AXI4 DataView recovery, see [tracking-elastic.md](tracking-elastic.md). For detailed AXI4 component guides, see [axi4-full.md](axi4-full.md) and [axi4-lite.md](axi4-lite.md).

## Contents

- [Elastic Imports](#elastic-imports)
- [Elastic Interfaces and Operators](#elastic-interfaces-and-operators)
- [Elastic Components](#elastic-components)
- [Elastic Composite Components](#elastic-composite-components)
- [AXI4 Imports](#axi4-imports)
- [AXI4 Interfaces and Connects](#axi4-interfaces-and-connects)
- [AXI4 Buffers](#axi4-buffers)
- [AXI4 Full Components](#axi4-full-components)
- [AXI4 Lite Components](#axi4-lite-components)
- [Memory](#memory)
- [Stream](#stream)
- [Load/Store](#loadstore)
- [Float](#float)
- [HDLInfo Port Declarations](#hdlinfo-port-declarations)

<span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span>
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
    <tr id="entry-elastic-interfaces-declare-elastic-interface">
      <td style="vertical-align:top;"><code>declareElasticInterface</code> <a href="#entry-elastic-interfaces-declare-elastic-interface" style="text-decoration:none;" aria-label="Permalink to entry-elastic-interfaces-declare-elastic-interface">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/AnnotatedModule.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Declare Elastic IO on an <code>AnnotatedModule</code>:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">declareElasticInterface(source, &quot;Task&quot;)
declareElasticInterface(sink, &quot;Result&quot;)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Adds hdlinfo metadata. If a module has Elastic interfaces but no Elastic components, manually initialize tracking in its module body with <code>chext.elastic.tracking.register()</code> so parent graph components can reference its child IO.</td>
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
    <tr id="entry-elastic-components-e-stall-sink">
      <td style="vertical-align:top;"><code>e.StallSink</code> <a href="#entry-elastic-components-e-stall-sink" style="text-decoration:none;" aria-label="Permalink to entry-elastic-components-e-stall-sink">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/StallSink.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Permanently backpressure a source:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stallSink0 =
  new e.StallSink(source)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: StallSink</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: stallSink</span> Port: <code>source</code>.</td>
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

## Elastic Composite Components

Composite components may expose hierarchy-only Elastic ports with <code>addSource(&hellip;, boundary = true)</code> and <code>addSink(&hellip;, boundary = true)</code>. Boundary ports appear in the module graph without marking the interface or participating in deadlock monitors; operational ports remain on leaf descendants.

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
    <tr id="entry-elastic-composite-components-e-repeat">
      <td style="vertical-align:top;"><code>e.Repeat</code> <a href="#entry-elastic-composite-components-e-repeat" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-repeat">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Repeat.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Count.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Repeat one input token multiple times:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in =&gt; in.length }
  out { (in, index, first, last) =&gt; in }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Repeat</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: repeat</span>. Boundary interfaces: source <code>source</code>, sink <code>sink</code>. Hierarchy: creates a child <code>Count</code> under <code>withComponent(this)</code>, with observed path shape <code>/repeat0</code> -> <code>/repeat0_count</code>.</td>
    </tr>
    <tr id="entry-elastic-composite-components-e-fold">
      <td style="vertical-align:top;"><code>e.Fold</code> <a href="#entry-elastic-composite-components-e-fold" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-fold">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Fold.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Fold.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/elastic/src/Fold.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Reduce a stream using user-provided fold logic:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in =&gt; in.data }
  last { in =&gt; in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Fold</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: fold</span>. Boundary sources: <code>source</code>, <code>sourceInit</code>, <code>sourceResult</code>; boundary sinks: <code>sink</code>, <code>sinkA</code>, <code>sinkB</code>. The latter three interfaces are user extension points for fold logic. Hierarchy: stage wiring under <code>stage0</code> and <code>stage1</code>; children include <code>Transducer</code> or <code>Transform</code>, <code>Join</code>, <code>Connect</code>, and buffers attached to the <code>Fold</code> component.</td>
    </tr>
    <tr id="entry-elastic-composite-components-e-loop">
      <td style="vertical-align:top;"><code>e.Loop</code> <a href="#entry-elastic-composite-components-e-loop" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-loop">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Loop.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Iterative elastic loop with user-visible body endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state =&gt; state.done }
  sinkCurrent :=&gt; loopBody.source
  loopBody.sink :=&gt; sourceNext
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Loop</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: loop</span>. Boundary sources: <code>sourceInit</code>, <code>sourceNext</code>; boundary sinks: <code>sinkExit</code>, <code>sinkCurrent</code>. <code>sinkCurrent</code> and <code>sourceNext</code> are the user-visible loop-body extension points. Hierarchy: creates <code>Stall</code>, <code>Connect</code>, <code>Fork</code>, <code>Demux</code>, <code>Merger</code>, and buffers attached to the <code>Loop</code> component.</td>
    </tr>
    <tr id="entry-elastic-composite-components-e-scope">
      <td style="vertical-align:top;"><code>e.Scope</code> <a href="#entry-elastic-composite-components-e-scope" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-scope">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Scope.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Begin/end scoped elastic region:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in =&gt; started := true.B }
  exit { out =&gt; finished := true.B }
  sinkBegin :=&gt; body.source
  body.sink :=&gt; sourceEnd
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Scope</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: scope</span>. Boundary sources: <code>sourceInit</code>, <code>sourceEnd</code>; boundary sinks: <code>sinkExit</code>, <code>sinkBegin</code>. <code>sinkBegin</code> and <code>sourceEnd</code> are the user-visible scope-body extension points. Hierarchy: creates a <code>Stall</code>, a <code>Connect</code>, and sink buffers attached to the <code>Scope</code> component.</td>
    </tr>
    <tr id="entry-elastic-composite-components-e-random-stall">
      <td style="vertical-align:top;"><code>e.RandomStall</code> <a href="#entry-elastic-composite-components-e-random-stall" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-random-stall">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/RandomStall.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Test/diagnostic random backpressure:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val randomStall0 =
  new e.RandomStall(source, sink)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: RandomStall</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: randomStall</span>. Boundary interfaces: source <code>source</code>, sink <code>sink</code>. Hierarchy: creates <code>Stall</code> plus <code>SinkBuffer</code>; observed child paths include <code>/randomStall0_stall</code> and <code>/randomStall0_stall_sinkBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-elastic-composite-components-e-switch">
      <td style="vertical-align:top;"><code>e.Switch</code> <a href="#entry-elastic-composite-components-e-switch" style="text-decoration:none;" aria-label="Permalink to entry-elastic-composite-components-e-switch">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/elastic/Switch.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/elastic/Switch.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Route tokens through user-defined branches:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val switch0 = new e.Switch(source, sink) {
  branch { in =&gt; in.select } { (branchSource, branchSink) =&gt;
    branchSource :=&gt; branchSink
  }
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Switch</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: switch</span>. Boundary interfaces include external source <code>source</code> and sink <code>sink</code>, plus <code>sink_&lt;branch&gt;</code> / <code>source_&lt;branch&gt;</code> extension pairs passed to each branch callback. Hierarchy: creates <code>Fork</code>, <code>Demux</code>, <code>Mux</code>, a selection queue, and the components instantiated by each branch.</td>
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
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for raw, full, and lite AXI interfaces. <code>lite = true</code> selects AXI4-Lite behavior; full AXI keeps <code>lite = false</code>. <code>BurstType.Encoding</code> and <code>ResponseFlag.Encoding</code> provide the protocol's integer encodings, while the outer objects provide the corresponding Chisel literals.</td>
    </tr>
    <tr id="entry-axi-connects-tracking-properties">
      <td style="vertical-align:top;">AXI4 aggregate tracking properties <a href="#entry-axi-connects-tracking-properties" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-tracking-properties">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/tracking/values/CheckResult.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/values/BurstShape.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/values/ThreadMode.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/values/TrafficProfile.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/values/package.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/properties/Defs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/properties/Keys.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/properties/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/Defs.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/Resolver.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/amba/axi4/tracking/Checker.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/tracking/Properties.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/tracking/CompositionDiagnostics.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Declare master properties, i.e. issued traffic, and slave properties, i.e. accepted traffic:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}
import chext.amba.axi4.tracking.{properties =&gt; p, values =&gt; v}

s_axi.properties(p.MasterReadBurstShape) = v.BurstShape(
  maxBeats = 16,
  types = Seq(FIXED, INCR, WRAP),
  sizes = v.BurstShape.supportedSizesFor(s_axi.cfg),
  aligned = true // Every transaction is naturally aligned to its AxSIZE.
)
s_axi.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleThread
m_axi.properties(p.SlaveMemoryMap) = v.MemoryMap(size = 0x1000)

s_axi.properties(p.MasterWriteTrafficProfile) =
  v.TrafficProfile(outstandingTransactions = 4)

val reads: Seq[p.Cell[_]] = s_axi.properties.select(p.Read)

request match {
  case ResolveRequest(
        _,
        cell @ p.Key(p.Master, p.Read, p.BurstShape | p.ThreadMode)
      ) =&gt;
    // `cell` is the original property cell.
}</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span> Repeated <code>.asFull</code> or <code>.asLite</code> conversions of the same raw AXI4 interface emit call-site warnings but do not fail elaboration; every resulting DataView wrapper remains independently tracked.<br><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span> Property keys are flat values in <code>tracking.properties</code>: for example, <code>p.MasterReadBurstShape</code> and <code>p.SlaveMemoryMap</code>. <code>p.KnownKeys</code> contains the standard catalog. Every <code>p.Key[T]</code> carries a <code>Role</code>, <code>Access</code>, and typed <code>ValueType[T]</code>; their package aliases (<code>p.Master</code>, <code>p.Read</code>, <code>p.BurstShape</code>, and so on) are both selectors and stable pattern values. <code>Manager.select</code> accepts one selector and returns matching <code>Cell</code> instances. <code>ResolveRequest[T]</code> is the transparent case class <code>(tracked, cell)</code>, so resolvers pattern-match it directly with <code>p.Key(role, access, valueType)</code> and may bind the original cell. Scala 2 does not refine <code>T</code> from a stable value-type pattern, so <code>calculate</code> checks the inferred value against the key's runtime type while <code>mapFrom</code> receives the selected value type explicitly. <code>BurstShape</code> and placeholder <code>TrafficProfile</code> are immutable value case classes under <code>tracking.values</code>; actual values conventionally use the <code>v</code> alias. <code>BurstShape</code> uses maximum beat count <code>maxBeats</code>, normalized <code>types</code> and <code>sizes</code> sequences, and Boolean <code>aligned</code>, which means every transaction satisfies <code>AxADDR % (1 &lt;&lt; AxSIZE) == 0</code>. <code>BurstShape</code> and <code>ThreadMode</code> both provide <code>normalize</code>, configuration-dependent <code>all</code>, <code>checkConfig</code>, and <code>checkCompatible</code>; checks return either <code>CheckResult.Success</code> or <code>CheckResult.Error(errors: Seq[String])</code>. <code>ThreadMode.values</code> lists every mode and <code>supportedModesFor</code> selects those valid for a configuration. Values are enforced as complete replacements; equal re-enforcement is idempotent, while conflicting re-enforcement fails. At root completion, <code>Checker</code> resolves paired Full burst shapes and Full/Lite thread modes but validates and compares a pair only when at least one local property is <code>Enforced</code>; calculated-vs-calculated pairs are omitted as redundant inferred boundaries. Each interface's memory map is still validated independently. AXI4-Lite burst shapes are <code>Undefined</code>; <code>TrafficProfile</code> is not checked. AXI4 property-checker diagnostics use the <code>axi4.tracking</code> label and are printed without aborting elaboration; Elastic endpoint and graph diagnostics use <code>elastic.tracking</code>. Each AXI diagnostic renders the problem, <code>Interface</code>, <code>Property</code>, details, and resolution traces as separate labeled lines. Enforcement retains its Chisel <code>SourceInfo</code>, originating interface, and the public resolver <code>Owner</code>; ordinary assignment syntax consumes the resolver's exact module/component owner implicitly and otherwise falls back to Chisel's current module. This origin propagates with calculated values. Failed compatibility diagnostics render separate master/slave enforcement blocks with the origin interface and property location. Module owners show module path, definition, and instantiation only; component owners first show component path and instantiation, then the containing module. The interface declaration is printed last. Immediately before checking, AXI module states cache module instantiation <code>SourceInfo</code> in one recursive hierarchy pass that scans each tracked parent's emitted <code>DefInstance</code> commands at most once.</td>
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
  simCheckAddrWidth = chext.util.SimulationCheck.None
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>s_axi.connect(m_axi, cfg)</code>: controls tie-offs, warnings, and simulation checks for full AXI connections. <code>warnSideband</code> compares the effective widths of <code>LOCK</code>, <code>CACHE</code>, <code>PROT</code>, <code>QOS</code>, and <code>REGION</code> on the address channels, plus <code>ARUSER</code>, <code>RUSER</code>, <code>AWUSER</code>, <code>WUSER</code>, and <code>BUSER</code>. It does not add simulation-time sideband checks. Address-width simulation checks are disabled by default.</td>
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
  simCheckAddrWidth = chext.util.SimulationCheck.None
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>s_axil.connect(m_axil, cfg)</code>: controls tie-offs, warnings, and address-width simulation checks for AXI4-Lite connections. Address-width simulation checks are disabled by default; select <code>Default</code>, <code>Printf</code>, or <code>Assert</code> to enable them.</td>
    </tr>
    <tr id="entry-axi-connects-full-slave-master">
      <td style="vertical-align:top;"><code>axi4f.Slave</code> / <code>axi4f.Master</code> <a href="#entry-axi-connects-full-slave-master" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-slave-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Full AXI IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi = IO(axi4f.Slave(axiCfg))
val m_axi = IO(axi4f.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>. Declared Full interfaces are registered by object identity for the root-level AXI compatibility pass.</td>
    </tr>
    <tr id="entry-axi-connects-lite-slave-master">
      <td style="vertical-align:top;"><code>axi4l.Slave</code> / <code>axi4l.Master</code> <a href="#entry-axi-connects-lite-slave-master" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-slave-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Interface.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite IO:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil = IO(axi4l.Slave(axiCfg))
val m_axil = IO(axi4l.Master(axiCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Channel-level elastic interfaces: <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, <code>b</code>. Declared Lite interfaces are registered by object identity for the root-level AXI compatibility pass.</td>
    </tr>
    <tr id="entry-axi-connects-full-single-connect">
      <td style="vertical-align:top;">Full single connect <a href="#entry-axi-connects-full-single-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-full-single-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/Connect.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one full AXI interface pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axi :=&gt; m_axi</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4f.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4fConnect</span>; graph <code>tpe</code> is <code>Axi4f_Connect</code>. Exposes each available master/slave AXI channel as a boundary interface; channel-level child <code>Connect</code> components retain operational ownership. A private <code>Connect_Resolver(owner: Connect)(implicit sourceInfo: SourceInfo)</code> transparently forwards master <code>BurstShape</code> / <code>ThreadMode</code> along the request path and slave <code>BurstShape</code> / <code>ThreadMode</code> / <code>MemoryMap</code> along the response path; direct <code>TrafficProfile</code> requests terminate as <code>Incomplete</code>. This transport ensures component-owned wire interfaces carry facts to a registered compatibility-check boundary.</td>
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
      <td style="vertical-align:top;">Lite single connect <a href="#entry-axi-connects-lite-single-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-single-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Connect one AXI4-Lite pair:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">s_axil :=&gt; m_axil</code></pre></td>
      <td style="vertical-align:top;">Creates <code>axi4l.Connect</code>; <span style="background:#f1f3f4;color:#3c4043;padding:2px 6px;border-radius:4px;">UniquePrefix: axi4lConnect</span>; graph <code>tpe</code> is <code>Axi4l_Connect</code>. Exposes each available master/slave AXI channel as a boundary interface; channel-level child <code>Connect</code> components retain operational ownership. Binding leaves all Lite <code>BurstShape</code> properties <code>Undefined</code>; a private <code>Connect_Resolver(owner: Connect)(implicit sourceInfo: SourceInfo)</code> transparently forwards master <code>ThreadMode</code> along the request path and slave <code>ThreadMode</code> / <code>MemoryMap</code> along the response path, while direct <code>TrafficProfile</code> requests terminate as <code>Incomplete</code>. This transport ensures component-owned wire interfaces carry facts to a registered compatibility-check boundary.</td>
    </tr>
    <tr id="entry-axi-connects-lite-configured-connect">
      <td style="vertical-align:top;">Lite configured connect <a href="#entry-axi-connects-lite-configured-connect" style="text-decoration:none;" aria-label="Permalink to entry-axi-connects-lite-configured-connect">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Connect.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/lite/Connect.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
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

AXI buffer naming follows the elastic convention: <code>SlaveBuffer</code>, <code>MasterBuffer</code>, <code>LeftBuffer</code>, and <code>RightBuffer</code> wrap their internal implementation in <code>uniquePrefix(name)</code> and are usually used anonymously inside a connection expression. <code>SlaveBuffered</code> and <code>MasterBuffered</code> return an interface directly and do not add an outer <code>uniquePrefix</code>, including for sequence overloads; bind them to a <code>val</code> when the buffered AXI side is reused. Every helper creates an inner tracked <code>Buffer</code> component (<code>axi4fBuffer</code> or <code>axi4lBuffer</code>) with a private <code>Buffer_Resolver(owner: Buffer)(implicit sourceInfo: SourceInfo)</code>, transparent aggregate-property forwarding, and channel-level elastic children.

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
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates a tracked <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Axi4f_Buffer</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: axi4fBuffer</span> that owns the AXI property resolver and channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code> children. Read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-full-slave-master-buffered">
      <td style="vertical-align:top;"><code>axi4f.SlaveBuffered</code> / <code>axi4f.MasterBuffered</code> <a href="#entry-axi-buffers-full-slave-master-buffered" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-full-slave-master-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered full AXI side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same tracked <code>Axi4f_Buffer</code> component and channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface and sequence forms do not add an outer <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add per-element index prefixes around the inner component, for example <code>slaveBuffered_0_axi4fBuffer0_arBuffer0_queue0</code>.</td>
    </tr>
    <tr id="entry-axi-buffers-lite-slave-master-buffer">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffer</code> / <code>axi4l.MasterBuffer</code> <a href="#entry-axi-buffers-lite-slave-master-buffer" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-lite-slave-master-buffer">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Buffer an AXI4-Lite side inline in a connection:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">axi4l.SlaveBuffer(s_axil, bufferCfg) :=&gt; m_axil
s_axil :=&gt; axi4l.MasterBuffer(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Uses <code>uniquePrefix</code>; internally creates a tracked <span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> <span style="background:#fff0e6;color:#a14200;padding:2px 6px;border-radius:4px;">tpe: Axi4l_Buffer</span> <span style="background:#eef0ff;color:#3730a3;padding:2px 6px;border-radius:4px;">namePrefix: axi4lBuffer</span> that owns the AXI property resolver and channel-level <code>e.SourceBuffer</code> / <code>e.SinkBuffer</code> children. AXI4-Lite read channels use <code>arBuffer</code> / <code>rBuffer</code>; write channels use <code>awBuffer</code> / <code>wBuffer</code> / <code>bBuffer</code>; underlying graph nodes are elastic <code>Queue</code> and <code>Connect</code> components.</td>
    </tr>
    <tr id="entry-axi-buffers-lite-slave-master-buffered">
      <td style="vertical-align:top;"><code>axi4l.SlaveBuffered</code> / <code>axi4l.MasterBuffered</code> <a href="#entry-axi-buffers-lite-slave-master-buffered" style="text-decoration:none;" aria-label="Permalink to entry-axi-buffers-lite-slave-master-buffered">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/Buffer.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Create a buffered AXI4-Lite side for reuse as a named value:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#ffe8ef;color:#a50e38;padding:2px 6px;border-radius:4px;">Function</span> Same tracked <code>Axi4l_Buffer</code> component and channel-level buffering as <code>SlaveBuffer</code> / <code>MasterBuffer</code>, but the single-interface and sequence forms do not add an outer <code>uniquePrefix</code>; the Scala <code>val</code> name is the important handle. Sequence overloads add per-element index prefixes around the inner component, for example <code>slaveBuffered_0_axi4lBuffer0_arBuffer0_queue0</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Full Components

AXI4 Full components are Chisel modules whose graph content comes from internal elastic components. A private neighboring resolver registers each tracked interface and implements its propagation or transformation policy. Standard values are <code>BurstShape</code>, <code>ThreadMode</code>, placeholder <code>TrafficProfile</code>, and slave <code>MemoryMap</code>. Binding marks disabled read/write accesses <code>Undefined</code>; the root checker validates Full burst shapes, Full/Lite thread modes, and memory maps. One-to-one resolvers forward or transform applicable facts, reserve <code>DontCare</code> for synthetic multi-endpoint aggregates without a represented value, and use <code>Incomplete</code> for applicable facts that are not yet modeled; <code>TrafficProfile</code> is not checked. See [axi4-full.md](axi4-full.md) for component APIs and examples.

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
  numMasters = 4, // m_axi interfaces
  decodeFn = addr =&gt; addr(13, 12),
  numIdsTrackedRead = 4,
  numIdsTrackedWrite = 4,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Demux</code>; <code>decodeFn</code> maps each address to an <code>m_axi</code> interface.</td>
    </tr>
    <tr id="entry-axi4-full-components-demux">
      <td style="vertical-align:top;"><code>axi4f.components.Demux</code> <a href="#entry-axi4-full-components-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Route transactions from one full AXI <code>s_axi</code> interface to multiple <code>m_axi</code> interfaces:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4f.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-out by address/routing selection. Its private <code>Demux_Resolver</code> forwards each master <code>BurstShape</code> and <code>ThreadMode</code> from <code>s_axi</code> unchanged to every <code>m_axi</code> interface. Slave burst/thread properties remain separate at the <code>m_axi</code> interfaces, so their synthetic aggregate at <code>s_axi</code> is <code>DontCare</code> and compatibility is checked at each <code>m_axi</code> interface. An arbitrary <code>decodeFn</code> cannot describe a combined address map, so <code>p.SlaveMemoryMap</code> remains <code>Incomplete</code> and strict map checking rejects the unresolved topology. <code>TrafficProfile</code> remains <code>Incomplete</code>. Internally uses channel-level elastic wiring around <code>ar</code>, <code>r</code>, <code>aw</code>, <code>w</code>, and <code>b</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-demux-mm-config">
      <td style="vertical-align:top;"><code>axi4f.components.DemuxMmConfig</code> <a href="#entry-axi4-full-components-demux-mm-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-demux-mm-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/DemuxMm.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Memory-map-driven fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.DemuxMmConfig(
  axiSlaveCfg = fullCfg,
  numMasters = 4,
  numIdsTrackedRead = 4,
  numIdsTrackedWrite = 4,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>DemuxMm</code>; address selection is supplied by generated elastic decoders rather than a Chisel decode function. AXI boundary buffering is left to the caller.</td>
    </tr>
    <tr id="entry-axi4-full-components-demux-mm">
      <td style="vertical-align:top;"><code>axi4f.components.DemuxMm</code> <a href="#entry-axi4-full-components-demux-mm" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-demux-mm">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/DemuxMm.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/DemuxMm.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/DemuxMm.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Aggregate maps resolved at the <code>m_axi</code> interfaces and generate address decoders:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">import chext.amba.axi4.tracking.{properties =&gt; p, values =&gt; v}

val demux = Module(new axi4f.components.DemuxMm(cfg))
demux.m_axi :=&gt; m_axi

// Slave properties propagate through AXI Connect components.
m_axi.zip(childMaps).foreach { case (master, childMap) =&gt;
  require(childMap.path.nonEmpty)
  master.properties(p.SlaveMemoryMap) = childMap
}
// Place m_axi(2), then m_axi(0); reserve m_axi(1) for decode errors:
val memoryMap = demux.genDecoder(
  permutation = Some(Seq(2, 0)),
  errorSlave = Some(1),
  allocationScheme = v.MemoryMap.AllocationScheme.AlignedPacked
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Resolves each <code>m_axi</code> interface's <code>p.SlaveMemoryMap</code> through connected components and buffers, then aggregates the non-error maps with <code>AlignedPacked</code>, <code>AlignedLargest</code>, or <code>Tight</code> allocation. <code>order</code> may supply a complete <code>m_axi</code> permutation. Each decoder subtracts the selected child offset from forwarded addresses, so nested demuxes receive local addresses. <code>memoryMapPath</code> names the aggregate; maps and segments retain absolute origins, and <code>captureResolutionTrace</code> stores typed forwarding steps. <code>errorSlave = Some(index)</code> reserves one <code>m_axi</code> interface for misses and permits its map to be <code>Undefined</code>; without it, misses select the first address-ordered <code>m_axi</code> interface. Validation rejects invalid bounds, overlaps, and layouts wider than <code>wAddr</code>. The resolver publishes the aggregate after <code>genDecoder()</code>, forwards master burst/thread facts to every <code>m_axi</code> interface, keeps slave burst/thread properties separate with <code>DontCare</code>, and leaves <code>TrafficProfile</code> <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.MuxConfig</code> <a href="#entry-axi4-full-components-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.MuxConfig(
  axiSlaveCfg = fullCfg,
  numSlaves = 4, // s_axi interfaces
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Mux</code>; IDs at <code>m_axi</code> are widened to include the selected <code>s_axi</code> interface.</td>
    </tr>
    <tr id="entry-axi4-full-components-mux">
      <td style="vertical-align:top;"><code>axi4f.components.Mux</code> <a href="#entry-axi4-full-components-mux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Arbitrate transactions from multiple full AXI <code>s_axi</code> interfaces onto one <code>m_axi</code> interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4f.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI fan-in. Its private <code>Mux_Resolver</code> forwards the common slave <code>BurstShape</code>, <code>ThreadMode</code>, and <code>MemoryMap</code> from <code>m_axi</code> to every <code>s_axi</code> interface. Master <code>BurstShape</code> at <code>m_axi</code> is <code>DontCare</code>; with one <code>s_axi</code> interface its master <code>ThreadMode</code> is forwarded, while multiple <code>s_axi</code> interfaces conservatively produce <code>Unconstrained</code>. <code>TrafficProfile</code> remains <code>Incomplete</code>. Internally creates arbitration and channel-level elastic wiring for read and write paths.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-demux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemuxConfig</code> <a href="#entry-axi4-full-components-id-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdDemuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // routes by 2 ID bits
  capacityPortQueueW = 8,
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdDemux</code>; <code>wIdSel</code> determines the number of <code>m_axi</code> interfaces as <code>1 &lt;&lt; wIdSel</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-demux">
      <td style="vertical-align:top;"><code>axi4f.components.IdDemux</code> <a href="#entry-axi4-full-components-id-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route full AXI traffic by transaction ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idDemux0 = Module(new axi4f.components.IdDemux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-based full AXI fan-out. Its private <code>IdDemux_Resolver</code> forwards master <code>BurstShape</code> from <code>s_axi</code> to each ID-selected <code>m_axi</code> interface and normally forwards <code>ThreadMode</code>. When selection removes every ID bit, each <code>m_axi</code> interface instead preserves <code>SingleTransaction</code> or conservatively calculates <code>SingleThread</code>. Slave burst properties remain separate at the <code>m_axi</code> interfaces with <code>DontCare</code> at <code>s_axi</code>, while slave thread properties are intersected and mapped through the inverse ID-removal rule. <code>p.SlaveMemoryMap</code> and all <code>TrafficProfile</code> transformations remain <code>Incomplete</code>. Internally expands to elastic channel components and rewrites ID bits for routing.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-mux-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdMuxConfig</code> <a href="#entry-axi4-full-components-id-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdMuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // s_axi interface bits in ID
  arbiterPolicy = e.Chooser.rr
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdMux</code>; response routing uses ID bits to recover the originating <code>s_axi</code> interface.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-mux">
      <td style="vertical-align:top;"><code>axi4f.components.IdMux</code> <a href="#entry-axi4-full-components-id-mux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-mux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdMux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Merge full AXI traffic while preserving ID-based response routing:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idMux0 = Module(new axi4f.components.IdMux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-aware full AXI fan-in. Its private <code>IdMux_Resolver</code> forwards slave <code>BurstShape</code>, <code>ThreadMode</code>, and <code>MemoryMap</code> from <code>m_axi</code> to every <code>s_axi</code> interface. Master <code>BurstShape</code> at <code>m_axi</code> is <code>DontCare</code>; master <code>ThreadMode</code> is forwarded for one <code>s_axi</code> interface and is <code>Unconstrained</code> for multiple prefixed-ID interfaces. <code>TrafficProfile</code> is <code>Incomplete</code>. Internally uses ID-based response routing and channel-level elastic components.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-serialize-config">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerializeConfig</code> <a href="#entry-axi4-full-components-id-serialize-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-serialize-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">ID serialization configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.IdSerializeConfig(
  axiSlaveCfg = fullCfg,
  numOutstandingRead = 4,
  numOutstandingWrite = 4,
  wIdSelect = 0
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdSerialize</code>; the ID width at <code>m_axi</code> becomes zero.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-serialize">
      <td style="vertical-align:top;"><code>axi4f.components.IdSerialize</code> <a href="#entry-axi4-full-components-id-serialize" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-serialize">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdSerialize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdSerialize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Serialize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idSerialize0 =
  Module(new axi4f.components.IdSerialize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID serialization. Its private <code>IdSerialize_Resolver</code> forwards the slave memory map and burst shape from <code>m_axi</code> to <code>s_axi</code>, and forwards the master burst shape from <code>s_axi</code> to <code>m_axi</code>. Forwarding the slave burst shape preserves the restrictions at <code>m_axi</code> so compatibility checks at <code>s_axi</code> still catch a missing <code>Unburst</code>. Master thread mode at <code>m_axi</code> preserves <code>SingleTransaction</code> and otherwise becomes <code>SingleThread</code>; the slave thread property at <code>s_axi</code> is the inverse of that transform, permitting <code>Unconstrained</code> only when the slave property at <code>m_axi</code> accepts <code>SingleThread</code>. Traffic profiles remain <code>Incomplete</code>. Internally limits outstanding work and routes response channels through elastic control logic.</td>
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
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>IdParallelize</code>; creates a wider-ID <code>m_axi</code> interface from a zero-ID <code>s_axi</code> interface.</td>
    </tr>
    <tr id="entry-axi4-full-components-id-parallelize">
      <td style="vertical-align:top;"><code>axi4f.components.IdParallelize</code> <a href="#entry-axi4-full-components-id-parallelize" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-id-parallelize">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/IdParallelize.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/IdParallelize.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Parallelize transactions by ID:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val idParallelize0 =
  Module(new axi4f.components.IdParallelize(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI ID parallelization. Its private <code>IdParallelize_Resolver</code> forwards the slave memory map from <code>m_axi</code> to <code>s_axi</code> and the master burst shape from <code>s_axi</code> to <code>m_axi</code>. The slave property at <code>s_axi</code> requires <code>SingleThread</code>; its accepted read-burst length is limited by the response-buffer capacity while all supported burst types and sizes are accepted with <code>aligned = false</code>. The unchanged slave write-burst property flows from <code>m_axi</code>, the master at <code>m_axi</code> issues <code>UniqueThreads</code>, and traffic profiles remain <code>Incomplete</code>. Internally distributes requests and rejoins responses with elastic channel logic.</td>
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
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width upscaler. Its private <code>Upscale_Resolver</code> requires <code>SingleThread</code> at <code>s_axi</code>, forwards <code>p.SlaveMemoryMap</code> from <code>m_axi</code>, and derives the slave burst property at <code>s_axi</code> from the property at <code>m_axi</code> while filtering types and sizes to the narrower interface. Master burst and thread properties flow unchanged from <code>s_axi</code> to <code>m_axi</code>, preserving stronger facts such as <code>SingleTransaction</code>; traffic profiles remain <code>Incomplete</code> because latency is not modeled. Internally adapts read/write data channels without changing transaction start addresses.</td>
    </tr>
    <tr id="entry-axi4-full-components-downscale-config">
      <td style="vertical-align:top;"><code>axi4f.components.DownscaleConfig</code> <a href="#entry-axi4-full-components-downscale-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-downscale-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Width downscale configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 32,
  numOutstandingWrite = 32,
  simCheckBurst = chext.util.SimulationCheck.Default
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Downscale</code>; the <code>m_axi</code> data width must be narrower than the <code>s_axi</code> data width. Transactions accepted at <code>s_axi</code> must have no IDs (<code>wId == 0</code>) and must be single-beat (<code>ARLEN == 0</code>, <code>AWLEN == 0</code>). <code>simCheckBurst</code> controls optional simulation checks for the single-beat precondition.</td>
    </tr>
    <tr id="entry-axi4-full-components-downscale">
      <td style="vertical-align:top;"><code>axi4f.components.Downscale</code> <a href="#entry-axi4-full-components-downscale" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-downscale">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Downscale.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Adapt a wider full AXI data bus to a narrower one:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val downscale0 = Module(new axi4f.components.Downscale(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI width downscaler for ID-free, single-beat transactions at <code>s_axi</code>. Its private <code>Downscale_Resolver</code> publishes a maximally permissive one-beat slave shape at <code>s_axi</code> using the interface-supported types and sizes with <code>aligned = false</code>, requires <code>SingleThread</code>, and forwards <code>p.SlaveMemoryMap</code>. The master property at <code>m_axi</code> uses INCR, conservatively advertises the protocol maximum length, maps <code>s_axi</code> sizes to the narrower bus, preserves the natural-alignment guarantee, and forwards the master thread mode from <code>s_axi</code>. Slave and master <code>TrafficProfile</code> transformations remain <code>Incomplete</code>. Use <code>Unburst</code> before it when transactions at <code>s_axi</code> may contain bursts and after it when the slave connected to <code>m_axi</code> cannot accept the advertised maximum burst.</td>
    </tr>
    <tr id="entry-axi4-full-components-unburst-config">
      <td style="vertical-align:top;"><code>axi4f.components.UnburstConfig</code> <a href="#entry-axi4-full-components-unburst-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-unburst-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Burst decomposition configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.UnburstConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>Unburst</code>; the <code>s_axi</code> and <code>m_axi</code> configurations are the same, but bursts are decomposed internally.</td>
    </tr>
    <tr id="entry-axi4-full-components-unburst">
      <td style="vertical-align:top;"><code>axi4f.components.Unburst</code> <a href="#entry-axi4-full-components-unburst" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-unburst">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Unburst.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/Unburst.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/amba/axi4/full/components/src/Unburst.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">Convert burst transactions into single-beat transactions:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val unburst0 = Module(new axi4f.components.Unburst(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI burst decomposition. Its private <code>Unburst_Resolver</code> requires <code>SingleThread</code>, calculates a one-beat INCR master shape at <code>m_axi</code>, preserves the <code>s_axi</code> natural-alignment guarantee, and forwards <code>p.SlaveMemoryMap</code>. It derives the slave burst property at <code>s_axi</code> from one-beat INCR acceptance at <code>m_axi</code>, filtering sizes and propagating the alignment requirement while retaining every burst type supported at <code>s_axi</code>. The master property at <code>m_axi</code> remains <code>SingleThread</code> because one burst issued at <code>s_axi</code> becomes multiple same-ID transactions; traffic profiles remain <code>Incomplete</code>. Internally generates per-beat addresses and coordinates response/data channels.</td>
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
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI data widening helper. Its private <code>Widen_Resolver</code> publishes a maximally permissive local <code>s_axi</code> shape except for unsupported FIXED bursts, forwards slave and master thread modes unchanged, and forwards <code>p.SlaveMemoryMap</code>. It removes FIXED from the burst types at <code>m_axi</code>, forces full-width <code>m_axi</code> transfers, sets <code>m_axi.aligned = false</code>, and calculates the <code>m_axi</code> beat count for the worst-case starting offset. Traffic profiles remain <code>Incomplete</code>. Internally coordinates address, strobe, data, and response handling through channel logic.</td>
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
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Full AXI credit-style response buffering around read/write channels. <code>CreditBuffer_Resolver</code> forwards master <code>BurstShape</code> and <code>ThreadMode</code> from <code>s_axi</code> to <code>m_axi</code>, slave <code>ThreadMode</code> and <code>p.SlaveMemoryMap</code> from <code>m_axi</code> to <code>s_axi</code>, and an unbuffered read/write path's slave <code>BurstShape</code> from <code>m_axi</code> to <code>s_axi</code>. When read-response or write-payload buffering is enabled, the corresponding slave burst shape at <code>s_axi</code> is capped by the buffer capacity and protocol maximum; all supported burst types and sizes are accepted with <code>aligned = false</code>. The write cap guarantees that the complete W burst is buffered locally before the AW transfer is allowed to proceed, without depending on W progress at <code>m_axi</code>. Traffic profiles remain <code>Incomplete</code> until concurrency and latency semantics are implemented.</td>
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
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Staged Full AXI protocol conversion using ID demux/serialization, width conversion, unbursting, and ID mux stages. External slave properties resolve from <code>m_axi</code> to <code>s_axi</code> through the composed internal stages; an internal <code>IdDemux</code> leaves the external slave burst aggregate <code>DontCare</code> while preserving checks at its <code>m_axi</code> interfaces, but still aggregates and inversely maps slave thread mode. <code>p.SlaveMemoryMap</code> flows directly from external <code>m_axi</code> to <code>s_axi</code>, and the resolver exposes the final composed master burst/thread values at external <code>m_axi</code>. A passthrough configuration also forwards the master traffic profile from <code>s_axi</code> to <code>m_axi</code> and the slave traffic profile from <code>m_axi</code> to <code>s_axi</code>, while transforming configurations leave them <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-lite-converter-config">
      <td style="vertical-align:top;"><code>axi4f.components.LiteConverterConfig</code> <a href="#entry-axi4-full-components-lite-converter-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-lite-converter-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/LiteConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Configure conversion from AXI4-Full to AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4f.components.LiteConverterConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 2,
  numOutstandingWrite = 2,
  simCheckNarrow = chext.util.SimulationCheck.Default,
  simCheckAligned = chext.util.SimulationCheck.Default
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configures inferred burst decomposition and width conversion. The Full interface must have <code>wId == 0</code>, and all user widths must be zero; <code>LiteConverter</code> does not instantiate <code>IdSerialize</code>. Outstanding capacities default to 2. <code>simCheckNarrow</code> verifies full-width <code>AxSIZE</code>, while <code>simCheckAligned</code> verifies naturally aligned <code>AxADDR</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-lite-converter">
      <td style="vertical-align:top;"><code>axi4f.components.LiteConverter</code> <a href="#entry-axi4-full-components-lite-converter" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-lite-converter">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/LiteConverter.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/amba/axi4/full/components/LiteConverter.test.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div></div></td>
      <td style="vertical-align:top;">Convert an AXI4-Full slave to an AXI4-Lite master:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val liteConverter0 =
  Module(new axi4f.components.LiteConverter(cfg))
s_axi :=&gt; liteConverter0.s_axi
liteConverter0.m_axil :=&gt; memController.s_axil</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> ID-free Full-to-Lite conversion with an authoritative naturally aligned, full-width slave burst shape at <code>s_axi</code> and <code>SingleThread</code> mode on both sides; Lite burst properties remain <code>Undefined</code>. Its bridge resolver publishes the protocol-implied one-beat shape only on the internal Full interface and forwards slave thread, traffic, and memory-map properties from <code>m_axil</code>; the external slave and master <code>TrafficProfile</code> transformations remain <code>Incomplete</code>. Unbursting at <code>s_axi</code> always precedes inferred width conversion. Upscaling steers data and write strobes without adding beats; downscaling is followed by a second unburst stage.</td>
    </tr>
    <tr id="entry-axi4-full-components-constant-slaves">
      <td style="vertical-align:top;"><code>axi4f.components.ConstantSlave</code> / <code>ZeroSlave</code> / <code>ErrorSlave</code> <a href="#entry-axi4-full-components-constant-slaves" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-constant-slaves">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Termination.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Terminate a full AXI master with constant responses:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val constantSlave = Module(new axi4f.components.ConstantSlave(
  axiCfg = fullCfg,
  readData = &quot;h1234&quot;.U,
  response = axi4.ResponseFlag.OKAY
))
val zeroSlave = Module(new axi4f.components.ZeroSlave(fullCfg))
val errorSlave0 = Module(new axi4f.components.ErrorSlave(fullCfg))
val errorSlave1 = Module(new axi4f.components.ErrorSlave(
  fullCfg,
  axi4.ResponseFlag.SLVERR
))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> <code>ConstantSlave</code> applies <code>SlaveBuffered</code> to its complete interface, preserves IDs and read burst length, discards writes, and returns constant read data and responses. Its resolver publishes the protocol-maximum slave <code>BurstShape</code>, <code>SingleTransaction</code>, and a full-address-space map only for successful responses; <code>ErrorSlave</code> uses an <code>Undefined</code> map. <code>ZeroSlave</code> derives from it with zero/<code>OKAY</code>; <code>ErrorSlave</code> derives from it with zero and a selectable <code>SLVERR</code> or <code>DECERR</code> response (default <code>DECERR</code>). <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-full-components-stall-slave-idle-master">
      <td style="vertical-align:top;"><code>axi4f.components.StallSlave</code> / <code>IdleMaster</code> <a href="#entry-axi4-full-components-stall-slave-idle-master" style="text-decoration:none;" aria-label="Permalink to entry-axi4-full-components-stall-slave-idle-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/full/components/Termination.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Attach explicit inactive full AXI endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stallSlave = Module(new axi4f.components.StallSlave(fullCfg))
val idleMaster = Module(new axi4f.components.IdleMaster(fullCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> <code>StallSlave</code> permanently backpressures requests and publishes an empty slave <code>BurstShape</code>, <code>Unconstrained</code>, and an <code>Undefined</code> map. <code>IdleMaster</code> issues no requests and publishes an empty master <code>BurstShape</code> with <code>SingleTransaction</code>. Their <code>TrafficProfile</code> properties remain <code>Incomplete</code>.</td>
    </tr>
  </tbody>
</table>

## AXI4 Lite Components

AXI4-Lite uses <code>ThreadMode</code>, placeholder <code>TrafficProfile</code>, and slave <code>MemoryMap</code>; its <code>BurstShape</code> properties are <code>Undefined</code>. Only <code>SingleTransaction</code> and <code>SingleThread</code> are valid Lite thread modes. The root checker validates thread modes and memory maps; <code>TrafficProfile</code> is not checked and may be forwarded or remain <code>Incomplete</code> according to the component policy. See [axi4-lite.md](axi4-lite.md) for component APIs and examples.

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
    <tr id="entry-axi4-lite-components-constant-slaves">
      <td style="vertical-align:top;"><code>axi4l.components.ConstantSlave</code> / <code>ZeroSlave</code> / <code>ErrorSlave</code> <a href="#entry-axi4-lite-components-constant-slaves" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-constant-slaves">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Termination.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Terminate an AXI4-Lite master with constant responses:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val constantSlave = Module(new axi4l.components.ConstantSlave(
  axiCfg = liteCfg,
  readData = &quot;h1234&quot;.U,
  response = axi4.ResponseFlag.OKAY
))
val zeroSlave = Module(new axi4l.components.ZeroSlave(liteCfg))
val errorSlave0 = Module(new axi4l.components.ErrorSlave(liteCfg))
val errorSlave1 = Module(new axi4l.components.ErrorSlave(
  liteCfg,
  axi4.ResponseFlag.SLVERR
))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> <code>ConstantSlave</code> applies <code>SlaveBuffered</code> to its complete interface, accepts AW and W in either order, discards writes, and returns constant read data and responses. Its resolver publishes <code>SingleTransaction</code> and a full-address-space map only for successful responses; <code>ErrorSlave</code> uses an <code>Undefined</code> map, and Lite burst shapes remain <code>Undefined</code>. <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-lite-components-stall-slave-idle-master">
      <td style="vertical-align:top;"><code>axi4l.components.StallSlave</code> / <code>IdleMaster</code> <a href="#entry-axi4-lite-components-stall-slave-idle-master" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-stall-slave-idle-master">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Termination.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Attach explicit inactive AXI4-Lite endpoints:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val stallSlave = Module(new axi4l.components.StallSlave(liteCfg))
val idleMaster = Module(new axi4l.components.IdleMaster(liteCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> <code>StallSlave</code> permanently backpressures requests and publishes <code>SingleThread</code> plus an <code>Undefined</code> map. <code>IdleMaster</code> issues no requests and publishes <code>SingleTransaction</code>; their Lite burst shapes remain <code>Undefined</code>, and their <code>TrafficProfile</code> properties remain <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-lite-components-demux-config">
      <td style="vertical-align:top;"><code>axi4l.components.DemuxConfig</code> <a href="#entry-axi4-lite-components-demux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-demux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite fan-out configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.DemuxConfig(
  axiSlaveCfg = liteCfg,
  numMasters = 4, // m_axil interfaces
  decodeFn = addr =&gt; addr(13, 12),
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for AXI4-Lite <code>Demux</code>; <code>decodeFn</code> maps each address to an <code>m_axil</code> interface.</td>
    </tr>
    <tr id="entry-axi4-lite-components-demux">
      <td style="vertical-align:top;"><code>axi4l.components.Demux</code> <a href="#entry-axi4-lite-components-demux" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-demux">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Demux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Route transactions from one AXI4-Lite <code>s_axil</code> interface to multiple <code>m_axil</code> interfaces:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val demux0 = Module(new axi4l.components.Demux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-out for address/data/control channels. Its private <code>Demux_Resolver</code> forwards master <code>ThreadMode</code> from <code>s_axil</code> to every <code>m_axil</code> interface, keeps slave thread properties separate at the <code>m_axil</code> interfaces with <code>DontCare</code> at <code>s_axil</code>, and leaves the arbitrary-decode <code>MemoryMap</code> plus every <code>TrafficProfile</code> request <code>Incomplete</code>. Lite burst shapes remain <code>Undefined</code>.</td>
    </tr>
    <tr id="entry-axi4-lite-components-mux-config">
      <td style="vertical-align:top;"><code>axi4l.components.MuxConfig</code> <a href="#entry-axi4-lite-components-mux-config" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-mux-config">#</a><div style="margin-top:6px;font-size:0.92em;"><div><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span></div></div><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/Mux.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">AXI4-Lite fan-in configuration:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val cfg = axi4l.components.MuxConfig(
  axiSlaveCfg = liteCfg,
  numSlaves = 4, // s_axil interfaces
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
      <td style="vertical-align:top;">Arbitrate transactions from multiple AXI4-Lite <code>s_axil</code> interfaces onto one <code>m_axil</code> interface:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val mux0 = Module(new axi4l.components.Mux(cfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite fan-in with channel-level elastic arbitration and routing. Its private <code>Mux_Resolver</code> forwards slave <code>ThreadMode</code> and <code>MemoryMap</code> from <code>m_axil</code> to each <code>s_axil</code> interface. Master <code>ThreadMode</code> at <code>m_axil</code> is <code>SingleThread</code>, every Lite burst shape is <code>Undefined</code>, and <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
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
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite response buffering with credit-style control around read/write channels. Its resolver forwards master <code>ThreadMode</code> from <code>s_axil</code> to <code>m_axil</code> and slave <code>ThreadMode</code> plus <code>p.SlaveMemoryMap</code> from <code>m_axil</code> to <code>s_axil</code>. Lite burst shapes remain <code>Undefined</code>, and slave/master <code>TrafficProfile</code> remain <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-lite-components-mem-controller">
      <td style="vertical-align:top;"><code>axi4l.components.MemController</code> <a href="#entry-axi4-lite-components-mem-controller" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-mem-controller">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/MemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a memory-like backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val memController0 =
  Module(new axi4l.components.MemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite memory controller. <code>MemController_Resolver</code> publishes the memory map and <code>SingleThread</code> slave mode; Lite burst shapes remain <code>Undefined</code>, and <code>TrafficProfile</code> remains <code>Incomplete</code>. Internally bridges AXI4-Lite channels to memory-style request and response paths.</td>
    </tr>
    <tr id="entry-axi4-lite-components-sync-read-mem-controller">
      <td style="vertical-align:top;"><code>axi4l.components.SyncReadMemController</code> <a href="#entry-axi4-lite-components-sync-read-mem-controller" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-sync-read-mem-controller">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/SyncReadMemController.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Expose a synchronous-read memory backend as AXI4-Lite:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val syncReadMemController0 =
  Module(new axi4l.components.SyncReadMemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI4-Lite controller variant for synchronous-read memories. <code>SyncReadMemController_Resolver</code> publishes the memory map and <code>SingleThread</code> slave mode; Lite burst shapes remain <code>Undefined</code>, and <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
    </tr>
    <tr id="entry-axi4-lite-components-register-block">
      <td style="vertical-align:top;"><code>axi4l.components.RegisterBlock</code> <a href="#entry-axi4-lite-components-register-block" style="text-decoration:none;" aria-label="Permalink to entry-axi4-lite-components-register-block">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/amba/axi4/lite/components/RegisterBlock.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Build an AXI4-Lite register block:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val regs = new axi4l.components.RegisterBlock(
  wAddr = 32, // AXI4-Lite address width
  wData = 32, // AXI4-Lite data width
  wMask = 12  // register address-space mask
)
val s_axil = IO(axi4l.Slave(regs.cfgAxi))
s_axil :=&gt; regs.s_axil

val control = RegInit(0.U(32.W))
val status = WireDefault(0.U(32.W))
regs.reg(control, desc = &quot;control&quot;)
regs.reg(status, write = false, desc = &quot;status&quot;)
val memoryMap = regs.complete()
assert(regs.memoryMapOption.contains(memoryMap))
assert(regs.memoryMap == memoryMap)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#e8f1ff;color:#174ea6;padding:2px 6px;border-radius:4px;">Component</span> AXI4-Lite register block with generated Elastic read/write behavior. Its request and response buffers permit multiple outstanding transactions, so its resolver publishes <code>SingleThread</code> slave mode while Lite burst shapes remain <code>Undefined</code>. <code>memoryMapOption</code> is <code>None</code> until <code>complete()</code> is called exactly once; <code>memoryMap</code> requires the completed value. <code>complete()</code> eagerly publishes <code>p.SlaveMemoryMap</code>, while a resolution request made before completion fails and <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
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
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>stream.Chunk</code>; controls address width, length width, data width, and maximum emitted burst length. <code>maxBurstLength</code> is a positive beat count.</td>
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
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> <code>ReadConfig</code> requires a read-only Full AXI configuration and controls read result mode and outstanding task buffering. <code>WriteConfig</code> requires a write-only Full AXI configuration and controls write result mode and outstanding task buffering. <code>maxBurstLength</code> is a positive beat count limited to 16 for AXI3-compatible configurations and 256 otherwise.</td>
    </tr>
    <tr id="entry-stream-read-stream-write">
      <td style="vertical-align:top;"><code>stream.Read</code>, <code>stream.Write</code> <a href="#entry-stream-read-stream-write" style="text-decoration:none;" aria-label="Permalink to entry-stream-read-stream-write">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/stream/Read.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/stream/Write.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/test/scala/chext/stream/Read.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../src/test/scala/chext/stream/Write.tb.scala" style="text-decoration:none;"><span style="background:#edf2ff;color:#364fc7;padding:2px 6px;border-radius:4px;">Scala TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Read.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div><div><a href="../sysc_tb/chext/stream/src/Write.tb.cpp" style="text-decoration:none;"><span style="background:#e6fcf5;color:#087f5b;padding:2px 6px;border-radius:4px;">SysC TB</span></a></div></div></td>
      <td style="vertical-align:top;">AXI stream read/write frontends:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val read0 = Module(new stream.Read(readCfg))
val write0 = Module(new stream.Write(writeCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> AXI stream read/write engines. They create task/data/result elastic channels and internally use <code>Drop</code>, <code>Fork</code>, <code>Transform</code>, <code>Repeat</code>, <code>Join</code>, <code>Mux</code>, and buffers. A private neighboring resolver binds each <code>m_axi</code> as a master. <code>Read</code> publishes its read <code>BurstShape</code> and <code>Write</code> publishes its write <code>BurstShape</code> with <code>maxBeats = cfg.maxBurstLength</code>, INCR bursts, full-width transfer size, and <code>aligned = true</code>; both publish <code>SingleThread</code> because every request uses ID zero while multiple transactions may be outstanding. The unsupported read/write access is <code>Undefined</code>, and master <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
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
      <td style="vertical-align:top;"><span style="background:#e6f4ea;color:#0d652d;padding:2px 6px;border-radius:4px;">Config</span> Configuration for <code>ldstr.Load</code> and <code>ldstr.Store</code>; <code>LoadConfig</code> requires a read-only Full AXI configuration, <code>StoreConfig</code> requires a write-only Full AXI configuration, and <code>numOutstandingTasks</code> sizes the buffering around AXI read/write response paths.</td>
    </tr>
    <tr id="entry-load-store-ldstr-load-ldstr-store">
      <td style="vertical-align:top;"><code>ldstr.Load</code>, <code>ldstr.Store</code> <a href="#entry-load-store-ldstr-load-ldstr-store" style="text-decoration:none;" aria-label="Permalink to entry-load-store-ldstr-load-ldstr-store">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/ldstr/Load.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div><div><a href="../src/main/scala/chext/ldstr/Store.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Load/store AXI frontends:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val load0 = Module(new ldstr.Load(loadCfg))
val store0 = Module(new ldstr.Store(storeCfg))</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#fff4d6;color:#8a5a00;padding:2px 6px;border-radius:4px;">Module</span> Load/store frontends for AXI memory access. They internally use elastic <code>Fork</code>, <code>Transform</code>, <code>SinkBuffer</code>, and <code>Join</code> components around AXI <code>ar/r</code> or <code>aw/w/b</code> paths. A private neighboring resolver binds each <code>m_axi</code> as a master and publishes its supported read/write access as a one-beat INCR <code>BurstShape</code> with full-width transfer size and <code>aligned = false</code>, plus <code>SingleThread</code> because every request uses ID zero while multiple transactions may be outstanding. The unsupported access is <code>Undefined</code>, and master <code>TrafficProfile</code> remains <code>Incomplete</code>.</td>
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

## HDLInfo Port Declarations

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
    <tr id="entry-hdlinfo-port-declarations-declare-data">
      <td style="vertical-align:top;"><code>declareData</code> <a href="#entry-hdlinfo-port-declarations-declare-data" style="text-decoration:none;" aria-label="Permalink to entry-hdlinfo-port-declarations-declare-data">#</a><div style="margin-top:6px;font-size:0.92em;"><div><a href="../src/main/scala/chext/AnnotatedModule.scala" style="text-decoration:none;"><span style="background:#e7f0ff;color:#0b4f9c;padding:2px 6px;border-radius:4px;">Scala</span></a></div></div></td>
      <td style="vertical-align:top;">Declare raw scalar and integer IO on an <code>AnnotatedModule</code>:<br><pre style="white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;padding:8px;background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;line-height:1.45;"><code class="language-scala">val request = IO(Input(UInt(12.W)))
val result = IO(Output(SInt(9.W)))

declareData(request)
declareData(result)</code></pre></td>
      <td style="vertical-align:top;"><span style="background:#f3e8ff;color:#6b21a8;padding:2px 6px;border-radius:4px;">Interface</span> Adds an hdlinfo data port and infers its name, direction, width, and bus range. Supports <code>Bool</code>, <code>UInt</code>, and <code>SInt</code>; signed integers are represented as raw bit vectors in hdlinfo.</td>
    </tr>
  </tbody>
</table>
