#include <DemuxMm_Tbtop.hpp>

#include <verilated_vcd_sc.h>

#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>

#include <array>
#include <cstdint>
#include <systemc>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;

struct DemuxMmTestbench : TestBenchBase {
    SC_HAS_PROCESS(DemuxMmTestbench);

    DemuxMmTestbench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);
    }

    DemuxMm_Tbtop dut;

private:
    static constexpr std::array<uint64_t, 4> leafBases { 0x000, 0x200, 0x800, 0xc00 };

    sc_clock clock;
    sc_signal<bool> reset;

    void entry() override {
        resetDUT();

        for (uint32_t leaf = 0; leaf < leafBases.size(); ++leaf) {
            auto const writeData = UINT32_C(0x13570000) + leaf;
            auto const expectedStorage = UINT32_C(0x00570000) + leaf;
            auto const ignored = UINT32_C(0xdead0000) + leaf;
            auto const status = UINT32_C(0x100) + leaf;

            writeBurst(leafBases[leaf], writeData, ignored);
            readAndCheckBurst(leafBases[leaf], expectedStorage, status);
        }

        fmt::println("DemuxMm tree simulation completed at {}", sc_time_stamp().to_string());
        finish();
    }

    void resetDUT() {
        reset.write(true);
        wait(clock.negedge_event());
        wait(clock.negedge_event());
        reset.write(false);
        wait(clock.negedge_event());
    }

    void writeBurst(uint64_t address, uint32_t storage, uint32_t ignored) {
        auto aw = dut.s_axi.makeAW();
        aw.addr = address;
        aw.len = 1;
        aw.size = 3; // 64-bit, naturally aligned transfers
        aw.burst = 1; // INCR

        auto w0 = dut.s_axi.makeW();
        w0.data = (UINT64_C(0xa5a5a5a5) << 32) | storage;
        w0.strb = 0xf5; // update bytes 0 and 2; upper strobes target the read-only status
        w0.last = false;

        auto w1 = dut.s_axi.makeW();
        w1.data = (UINT64_C(0x5a5a5a5a) << 32) | ignored;
        w1.strb = 0xff;
        w1.last = true;

        sc_join join;
        SC_SPAWN_TO(join) { dut.s_axi.sendAW(aw); };
        SC_SPAWN_TO(join) {
            dut.s_axi.sendW(w0);
            dut.s_axi.sendW(w1);
        };
        join.wait();

        auto const b = dut.s_axi.receiveB();
        EXPECT_EQ(b.resp, 0);
    }

    void readAndCheckBurst(uint64_t address, uint32_t storage, uint32_t status) {
        auto ar = dut.s_axi.makeAR();
        ar.addr = address;
        ar.len = 1;
        ar.size = 3; // 64-bit, naturally aligned transfers
        ar.burst = 1; // INCR

        dut.s_axi.sendAR(ar);

        auto const r0 = dut.s_axi.receiveR();
        auto const expected0 = (uint64_t(status) << 32) | storage;
        EXPECT_EQ(r0.data.to_uint64(), expected0);
        EXPECT_EQ(r0.resp, 0);
        EXPECT_(!r0.last);

        auto const r1 = dut.s_axi.receiveR();
        EXPECT_EQ(r1.data.to_uint64(), UINT64_MAX);
        EXPECT_EQ(r1.resp, 0);
        EXPECT_(r1.last);
    }
};

int sc_main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
#if defined(VERILATED_TRACE_ENABLED)
    Verilated::traceEverOn(true);
#endif

    DemuxMmTestbench testBench;

    sc_start(SC_ZERO_TIME);

#if defined(VERILATED_TRACE_ENABLED)
    auto traceFile = std::make_unique<VerilatedVcdSc>();
    testBench.dut.traceVerilated(traceFile.get(), 99);
    traceFile->open("DemuxMm.vcd");
#endif

    testBench.start();

#if defined(VERILATED_TRACE_ENABLED)
    traceFile->close();
#endif

    return 0;
}
