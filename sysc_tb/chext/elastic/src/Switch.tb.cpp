#include <Switch_Tbtop1.hpp>

#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>

#include <systemc>
#include <verilated_vcd_sc.h>

using namespace chext_test;
using namespace sc_core;
using namespace sc_dt;

struct Testbench : public chext_test::TestBenchBase {
    Testbench(sc_module_name const& name = "testbench")
        : TestBenchBase { name }
        , SC_NAMED(dut)
        , SC_NAMED(clock, sc_time(2, SC_NS))
        , SC_NAMED(reset) {

        dut.clock(clock);
        dut.reset(reset);
    }

    Switch_Tbtop1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    void sendTask(
        std::uint8_t target,
        std::uint32_t operand,
        std::uint8_t delay
    ) {
        Switch_Tbtop1::Task_value task { target, operand, delay };
        dut.sourceTask.send(task);
        fmt::println("Sent: {} at {}", task, sc_time_stamp().to_string());
    }

    std::uint32_t expectResult(std::uint32_t x) {
        auto result = dut.sinkResult.receive();
        fmt::println("Received: {} at {} {}", result, sc_time_stamp().to_string(), x == result ? "SUCCESS" : "FAIL");
        return result;
    }

    void entry() override {
        resetDut();

        sc_join j;

        SC_SPAWN_TO(j) {
            sendTask(0, 32, 0);
            sendTask(0, 64, 0);
            sendTask(0, 96, 0);

            sendTask(1, 5, 0);
            sendTask(1, 10, 0);
            sendTask(1, 15, 0);

            sendTask(2, 6, 30);
            sendTask(2, 12, 30);
            sendTask(2, 18, 30);
        };

        SC_SPAWN_TO(j) {
            expectResult(32);
            expectResult(64);
            expectResult(96);

            expectResult(10);
            expectResult(20);
            expectResult(30);

            expectResult(12);
            expectResult(24);
            expectResult(36);
        };

        j.wait();

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
#if defined(VERILATED_TRACE_ENABLED)
    Verilated::traceEverOn(true);
#endif

    Testbench tb;
    sc_start(SC_ZERO_TIME);

#if defined(VERILATED_TRACE_ENABLED)
    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    tb.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open("MyTestBench.vcd");
#endif

    sc_start(sc_time(1000, SC_NS));
#if defined(VERILATED_TRACE_ENABLED)
    trace_file->close();
#endif

    return 0;
}
