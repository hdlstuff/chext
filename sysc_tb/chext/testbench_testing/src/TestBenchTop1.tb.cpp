#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>
#include <systemc>
#include <verilated_vcd_sc.h>

#include <TestBenchTop1.hpp>

using namespace chext_test;
using namespace sc_core;
using namespace sc_dt;

class TestBench : public chext_test::TestBenchBase {
public:
    SC_HAS_PROCESS(TestBench);

    TestBench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);
    }

    TestBenchTop1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    void entry() override {
        resetDut();

        TestBenchTop1::Task1_value task1;

        finish();
    }

    void resetDut() {
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

    TestBench tb;

    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    tb.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open(fmt::format("{}.vcd", "UpdateMemTop1").c_str());

    tb.start();

    trace_file->close();

    return 0;
}
