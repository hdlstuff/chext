package chext.amba.axi4.full.components

/** SystemC testbench top for the three-DemuxMm/four-RegisterBlock tree. */
class DemuxMm_Tbtop extends DemuxMmTreeTestTop

object DemuxMm_Tb extends chext.TestBench {
  emit(new DemuxMm_Tbtop)
}
