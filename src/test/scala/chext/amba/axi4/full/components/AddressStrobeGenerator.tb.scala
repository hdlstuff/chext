package chext.amba.axi4.full.components

object AddressStrobeGenerator_TB extends App with chext.TestBench {
  emit(new AddressStrobeGenerator(32, 128, Some("AddressStrobeGenerator_1")))
}
