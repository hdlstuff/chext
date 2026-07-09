package chext.util

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
