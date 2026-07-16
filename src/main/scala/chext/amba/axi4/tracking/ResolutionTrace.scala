package chext.amba.axi4.tracking

final case class ResolutionStep(
    interfaceFrom: String,
    interfaceTo: String,
    kind: String,
    resolver: String,
    resolverPath: String
)

final case class ResolutionTrace(steps: Seq[ResolutionStep])
