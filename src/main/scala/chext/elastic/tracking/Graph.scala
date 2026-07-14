package chext.elastic.tracking

import hdlinfo.TypedObject

// NOTE: Keeping sourceInfo is mostly useless! Do not attempt doing it later.

object Graph {
  private val require_ = chext.util.Require.inferred()

  case class Interface(
      val path: String,
      val tpe: String,
      val args: Map[String, TypedObject] = Map.empty
  ) {
    require_(path.length > 0 && path.head == '/', "path should start with '/'!")

    private[Graph] def pathPrepended(x: String) = copy(path = f"$x$path")
  }

  case class InterfaceRef(
      val path: String,
      val desc: String = "",
      val boundary: Boolean = false
  ) {
    require_(path.length > 0 && path.head == '/', "path should start with '/'!")

    private[Graph] def pathPrepended(x: String) = copy(path = f"$x$path")
  }

  case class Component(
      val path: String,
      val tpe: String,
      val sources: Seq[(String, InterfaceRef)],
      val sinks: Seq[(String, InterfaceRef)],
      val children: Seq[String],
      val parent: String,
      val args: Map[String, TypedObject] = Map.empty
  ) {
    require_(path.length > 0 && path.head == '/', "path should start with '/'!")

    private[Graph] def pathPrepended(x: String) = copy(
      path = f"$x$path",
      sources = sources.map { case (name, interfaceRef) => (name, interfaceRef.pathPrepended(x)) },
      sinks = sinks.map { case (name, interfaceRef) => (name, interfaceRef.pathPrepended(x)) },
      children = children.map { path => f"$x$path" },
      parent = {
        if (parent.nonEmpty) f"$x$parent"
        else ""
      }
    )
  }

  case class Module(
      val name: String,
      val path: String,
      val sources: Seq[Interface],
      val sinks: Seq[Interface],
      val wires: Seq[Interface],
      val components: Seq[Component],
      val children: Seq[Module],
      val args: Map[String, TypedObject] = Map.empty
  ) {
    require_(path.length > 0 && path.head == '/', "path should start with '/'!")

    private[Graph] def pathPrepended(x: String): Module = copy(
      path = f"$x$path",
      sources = sources.map { _.pathPrepended(x) },
      sinks = sinks.map { _.pathPrepended(x) },
      wires = wires.map { _.pathPrepended(x) },
      components = components.map { _.pathPrepended(x) },
      children = children.map { child => child.copy(path = f"$x${child.path}") }
    )

    def flatten: Module = {
      val childrenFlattened = children.map { _.flatten }
      copy(children = childrenFlattened).flattenOnce
    }

    def flattenOnce: Module = {
      val childrenPrepended = children.map { child => child.pathPrepended(child.path) }

      Module(
        name = name,
        path = path,
        sources = sources,
        sinks = sinks,
        wires = {
          Seq(
            wires,
            childrenPrepended.map { _.sources }.flatten,
            childrenPrepended.map { _.sinks }.flatten,
            childrenPrepended.map { _.wires }.flatten
          ).flatten
        },
        components = {
          Seq(
            components,
            childrenPrepended.map { _.components }.flatten
          ).flatten
        },
        children = {
          childrenPrepended.map { _.children }.flatten
        },
        args = args
      )
    }
  }
}
