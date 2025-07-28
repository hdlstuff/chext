package chext.util

import scala.collection.mutable.ArrayBuffer

class Node(val id: String = "<unnamed>") {
  private var _parent = Option.empty[Node]
  private var _data = Option.empty[Any]
  private var _children = ArrayBuffer.empty[Node]
  private var _level: Int = 0

  def setData[T](data: T): Unit = _data = Some(data)
  def hasData: Boolean = _data.isDefined
  def data[T]: T = _data.get.asInstanceOf[T]

  def level: Int = _level

  def hasParent: Boolean = _parent.isDefined
  def isRoot: Boolean = !hasParent
  def parent: Node = _parent.get

  def isLeaf: Boolean = _children.isEmpty

  def addChild(node: Node): Unit = {
    node._level = _level + 1
    node._parent = Some(this)
    _children += node
  }

  def children: Seq[Node] = _children.toSeq

  override def toString(): String = {
    f"Node(id = \"${id}\", data = ${_data.getOrElse(null)})"
  }

  def dump(indent: Int = 0): Unit = {
    println(" " * indent + this)
    _children.foreach { _.dump(indent + 4) }
  }

  def traverseDown(f: (Node) => Unit): Unit = {
    f(this)
    _children.foreach { _.traverseDown(f) }
  }

  def traverseUp(f: (Node) => Unit): Unit = {
    f(this)
    if (hasParent)
      parent.traverseUp(f)
  }
}

class Tree(
    val root: Node,
    val depth: Int,
    val count: Int
) {
  def dump(): Unit = root.dump()
  def traverse(f: (Node) => Unit): Unit = root.traverseDown(f)

  def internalNodes: Seq[Node] = Tree.getInternalNodes(root)
  def leafNodes: Seq[Node] = Tree.getLeafNodes(root)
}

object Tree {
  private def logCeil(x: Int, base: Int): Int = {
    // we want to find the smallest n such that base^n >= x
    // Math.log suffers from numerical precision issues

    var m = 1
    var n = 0

    while (m < x) {
      m *= base
      n += 1
    }

    n
  }

  private def getInternalNodes(root: Node): Seq[Node] = {
    val internalNodes = ArrayBuffer.empty[Node]
    val queue = new scala.collection.mutable.Queue[Node]
    queue.enqueue(root)

    while (queue.nonEmpty) {
      val node = queue.dequeue()

      if (node.children.nonEmpty)
        internalNodes += node

      node.children.foreach { queue.enqueue(_) }
    }

    internalNodes.toSeq
  }

  private def getLeafNodes(root: Node): Seq[Node] = {
    val leafNodes = ArrayBuffer.empty[Node]
    val queue = new scala.collection.mutable.Queue[Node]
    queue.enqueue(root)

    while (queue.nonEmpty) {
      val node = queue.dequeue()

      if (node.children.isEmpty)
        leafNodes += node
      else
        node.children.foreach { queue.enqueue(_) }
    }

    leafNodes.toSeq
  }

  def selfRoutingTree(log2base: Int, count: Int): Tree = {
    def safeDiv(dividend: Int, divisor: Int): Int = {
      if ((dividend % divisor) == 0)
        dividend / divisor
      else
        dividend / divisor + 1
    }

    val root = new Node(":")
    var numNodes = 1

    val numDigits = safeDiv(chisel3.util.log2Ceil(count), log2base)
    val mask = (1 << log2base) - 1

    def append(number: Int): Unit = {
      val path = {
        val path = ArrayBuffer.empty[Int]

        var n = number
        while (n > 0) {
          path += n & mask
          n = n >> log2base
        }

        path.toSeq.padTo(numDigits, 0).reverse
      }
      // println(numDigits, path)

      var node = root
      path.foreach { (index) =>
        {
          if (index >= node.children.length) {
            for (i <- node.children.length to index) {
              val child = new Node(node.id + "/" + i)
              node.addChild(child)
              numNodes += 1
            }
          }

          node = node.children(index)
        }
      }

      node.setData(number)
    }

    for (i <- 0 until count)
      append(i)

    new Tree(root, numDigits, numNodes)
  }
}

object TreeTest extends App {
  // Tree.selfRoutingTree(2, 1).dump()
  // Tree.selfRoutingTree(2, 2).dump()
  // Tree.selfRoutingTree(2, 3).dump()
  // Tree.selfRoutingTree(2, 4).dump()
  // Tree.selfRoutingTree(2, 5).dump()
  // Tree.selfRoutingTree(2, 6).dump()
  // Tree.selfRoutingTree(2, 7).dump()
  // Tree.selfRoutingTree(2, 8).dump()
  // Tree.selfRoutingTree(2, 9).dump()
  // Tree.selfRoutingTree(2, 10).dump()
  // Tree.selfRoutingTree(2, 11).dump()
  // Tree.selfRoutingTree(2, 12).dump()
  // Tree.selfRoutingTree(2, 13).dump()
  // Tree.selfRoutingTree(2, 14).dump()
  // Tree.selfRoutingTree(2, 15).dump()
  // Tree.selfRoutingTree(2, 16).dump()
  // Tree.selfRoutingTree(2, 17).dump()

  val tree = Tree.selfRoutingTree(2, 34)
  tree.dump()
  tree.leafNodes.foreach { (node) => println(f"Leaf node: $node") }
  tree.internalNodes.foreach { (node) => println(f"Internal node: $node, level: ${node.level}") }
  println(s"Tree depth: ${tree.depth}")
}
