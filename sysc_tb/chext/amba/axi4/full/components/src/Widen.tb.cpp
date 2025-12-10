#include <Widen_Tbtop.hpp>

#include <verilated_vcd_sc.h>

#include <chext_test/amba/axi4/full/ReadWriteTester.hpp>
#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>

#include <systemc>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::amba;

struct WidenTestbench : virtual TestBenchBase, axi4::full::ReadWriteTester {
    SC_HAS_PROCESS(WidenTestbench);

    WidenTestbench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);
    }

    Widen_Tbtop dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    void entry() override {
#if 0
        SC_SPAWN {
            while (true) {
                fmt::print("\r{:=^100}", fmt::format("  t = {}  ", sc_time_stamp().to_string()));
                std::cout.flush();
                wait(100, SC_US);
            }
        };
#endif

        resetDUTs();

        readWriteTest(dut.S_AXI_TEST, dut.S_AXI_TEST, 0x00, 1024);
        readWriteTest(dut.S_AXI_NORMAL, dut.S_AXI_TEST, 0x00, 128);
        readWriteTest(dut.S_AXI_TEST, dut.S_AXI_NORMAL, 0x00, 128);

        // uint64_t addr = 0x008;
        // uint64_t numBytes = 1024;
        // std::vector<uint8_t> wrBuffer(numBytes);
        // buffer_utils::linearInit(wrBuffer);
        // axi4::full::write(dut.S_AXI_NORMAL, addr, numBytes, wrBuffer.data(), 2, false);

        // sc_join j;

        // int count = 9;

        // SC_SPAWN_TO(j) {
        //     dut.S_AXI_TEST.ar.send({ .addr = sc_bv<32>(addr), .len = count - 1, .size = 0, .burst = 1 });
        // };

        // SC_SPAWN_TO(j) {
        //     for (int i = 0; i < count; ++i) {
        //         fmt::println("Received: {}", dut.S_AXI_TEST.r.receive());
        //     }
        // };

        // j.wait();

        fmt::print("\r{:~^100}\n", fmt::format("  simulation time: {}  ", sc_time_stamp().to_string()));

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

    WidenTestbench testBench;

    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    testBench.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open("WidenTestbench.vcd");

    testBench.start();

    trace_file->close();

    return 0;
}
