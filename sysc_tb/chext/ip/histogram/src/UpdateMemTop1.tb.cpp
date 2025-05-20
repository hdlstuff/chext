#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>
#include <systemc>
#include <verilated_vcd_sc.h>

#include <UpdateMemTop1.hpp>

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

    UpdateMemTop1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    /*
        val zero = Bool()
        val last = Bool()

        val bucket = UInt(64.W)
        val value = UInt(64.W)
    */
    void sendTask(bool last, bool zero, uint16_t bucket, uint16_t value) {
        UpdateMemTop1::Item_value item { zero, last, bucket, value };
        fmt::print("sending task: {}\n", item);
        dut.sourceItem.send(item);
    }

    void retrieveResult() {
        dut.sinkResult.receive();
    }

    void entry() override {
        resetDut();

        sendTask(false, false, 0, 5);
        sendTask(false, false, 0, 5);
        sendTask(false, false, 0, 5);
        sendTask(true, false, 0, 5);
        retrieveResult();

        sendTask(true, true, 0, 0);
        retrieveResult();

        sendTask(true, false, 0, 5);
        retrieveResult();

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
