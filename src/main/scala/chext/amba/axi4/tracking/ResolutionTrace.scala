package chext.amba.axi4.tracking

/** One boundary crossed while deriving a tracking property.
  *
  * Steps are ordered from the interface on which resolution was requested toward the interface
  * that supplied the value. Paths are absolute slash-separated tracking paths, while `kind` and
  * `resolver` are stable machine-readable identifiers suitable for diagnostics and serialized
  * memory-map arguments.
  *
  * @param interfaceFrom
  *   interface whose property was being calculated at this hop
  * @param interfaceTo
  *   dependency interface from which the property was obtained
  * @param kind
  *   category of boundary crossed, such as `connect` or `buffer`
  * @param resolver
  *   resolver implementation identifier
  * @param resolverPath
  *   absolute path of the component or module that owns the resolver
  */
final case class ResolutionStep(
    interfaceFrom: String,
    interfaceTo: String,
    kind: String,
    resolver: String,
    resolverPath: String
)

/** Ordered provenance of a calculated tracking property.
  *
  * The first element is nearest the original request and the last is nearest the interface that
  * supplied the value.
  */
final case class ResolutionTrace(steps: Seq[ResolutionStep])
