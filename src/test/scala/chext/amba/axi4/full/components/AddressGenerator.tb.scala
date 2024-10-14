package chext.amba.axi4.full.components

object AddressGenerator_TB extends App with chext.TestBench {
  emit(new AddressGenerator(32, Some("AddressGenerator_1")))
}
