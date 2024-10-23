#include <IdParallelizeTestTop2_1.hpp>

#include <verilated_vcd_sc.h>

#include <systemc>

#include <Util.hpp>
#include <chext_test/chext_test.hpp>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::util;

using namespace chext_test::amba;

struct MyTestBench : virtual TestBenchBase {
    SC_HAS_PROCESS(MyTestBench);

    MyTestBench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" }
        , arFifo { "arFifo" } {

        dut.clock(clock);
        dut.reset(reset);
    }

    IdParallelizeTestTop2_1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    // other channels
    sc_fifo<axi4::full::Packets::ReadAddress> arFifo;

    void entry() override {
        auto const& axiSlaveCfg = dut.S_AXI.config();
        auto const& axiMasterCfg = dut.S_AXI.config();

        unsigned numAddress = 1024 * 16;
        unsigned numBeat = 1;
        unsigned idMask = (1 << axiMasterCfg.wId) - 1;
        unsigned addrOffset = 16;

        // needed for chisel modules
        resetDUTs();

        sc_join j;

        SC_SPAWN_TO(j) {
            for (uint16_t i = 0; i < numAddress; ++i) {
                axi4::full::Packets::ReadAddress ar {
                    .id = bv_from(i & idMask),
                    .addr = bv_from(i << addrOffset),
                    .len = (uint8_t)(numBeat - 1)
                };
                dut.S_AXI.sendAR(ar);
                fmt::print("t = {}, dut.S_AXI.sendAR({})\n", sc_time_stamp().to_string(), ar);
            }
        };

        SC_SPAWN_TO(j) {
            for (uint16_t i = 0; i < numAddress; ++i) {
                for (uint16_t j = 0; j < numBeat; ++j) {
                    auto r = dut.S_AXI.receiveR();
                    fmt::print("t = {}, dut.S_AXI.receiveR() = {}\n", sc_time_stamp().to_string(), r);

                    auto received = r.data.to_uint64();
                    auto expected = (i << addrOffset) + j;

                    fmt::print("t = {}, received = {:#010x}, expected = {:#010x}, {}\n", sc_time_stamp().to_string(), received, expected, received == expected ? "" : "*");

                    ASSERT_EQ(received, expected);
                }
            }
        };

        SC_SPAWN_TO(j) {
            for (uint16_t i = 0; i < numAddress; ++i) {
                auto ar = dut.M_AXI.receiveAR();
                fmt::print("t = {}, dut.M_AXI.receiveAR() = {}\n", sc_time_stamp().to_string(), ar);
                arFifo.write(ar);
            }
        };

        SC_SPAWN_TO(j) {
            for (uint16_t i = 0; i < numAddress; ++i) {
                auto ar = arFifo.read();

                for (uint16_t j = 0; j < numBeat; ++j) {
                    axi4::full::Packets::ReadData r {
                        .id = ar.id,
                        .data = bv_from(ar.addr.to_uint64() + j),
                        .resp = 0,
                        .last = (j == numBeat - 1)
                    };
                    dut.M_AXI.sendR(r);
                    fmt::print("t = {}, dut.M_AXI.sendR({})\n", sc_time_stamp().to_string(), r);
                }
            }
        };

        j.wait();

        finish();
    }

    void resetDUTs() {
        wait(clock.negedge_event());
        reset.write(true);

        wait(clock.negedge_event());
        wait(clock.negedge_event());

        reset.write(false);

        wait(clock.negedge_event());
    }
};

int sc_main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true);

    MyTestBench testBench;

    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    testBench.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open("MyTestBench.vcd");

    testBench.start();

    trace_file->close();

    return 0;
}
