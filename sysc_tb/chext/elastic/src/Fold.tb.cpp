#include <Fold_Tbtop.hpp>

#include <systemc>
#include <verilated_vcd_sc.h>

#include <chext_test/util/Spawn.hpp>

using namespace sc_core;
using namespace sc_dt;

struct Testbench : public sc_module {
    Testbench(sc_module_name const& name = "testbench")
        : sc_module { name }
        , SC_NAMED(dut)
        , SC_NAMED(clock, sc_time(2, SC_NS))
        , SC_NAMED(reset) {

        dut.clock(clock);
        dut.reset(reset);

        SC_THREAD(thread0);
    }

    Fold_Tbtop dut;

    sc_clock clock;
    sc_signal<bool> reset;

    void thread0() {
        reset.write(1);
        wait(10, SC_NS);
        reset.write(0);

        sc_join j;

        SC_SPAWN_TO(j) {
            dut.source.send({ .zero = false, .last = false, .data = 32 });
            dut.source.send({ .zero = false, .last = true, .data = 32 });

            dut.source.send({ .zero = true, .last = false, .data = 32 });
            dut.source.send({ .zero = false, .last = true, .data = 32 });

            for (int i = 0; i < 32; ++i) {
                dut.source.send({ .zero = false, .last = i == 31, .data = -i });
            }

            dut.source.send({ .zero = false, .last = true, .data = 32 });
            dut.source.send({ .zero = true, .last = true, .data = 32 });

            dut.source.send({ .zero = true, .last = true, .data = 32 });
            dut.source.send({ .zero = true, .last = true, .data = 32 });
            dut.source.send({ .zero = true, .last = true, .data = 32 });
            dut.source.send({ .zero = true, .last = true, .data = 32 });
            dut.source.send({ .zero = true, .last = true, .data = 32 });
        };

        SC_SPAWN_TO(j) {
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());

            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());

            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());

            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());

            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
            fmt::println("result: {}", dut.sink.receive());
            fmt::println("time: {}", sc_time_stamp().to_string());
        };

        j.wait();
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
    trace_file->open("Fold.tb.vcd");
#endif

    sc_start(sc_time(1000, SC_NS));
#if defined(VERILATED_TRACE_ENABLED)
    trace_file->close();
#endif

    return 0;
}
