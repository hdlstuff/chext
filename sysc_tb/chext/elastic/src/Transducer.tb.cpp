#include <Transducer_Tbtop.hpp>

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

    Transducer_Tbtop dut;

    sc_clock clock;
    sc_signal<bool> reset;

    void thread0() {
        reset.write(1);
        wait(10, SC_NS);
        reset.write(0);

        sc_join j;

        SC_SPAWN_TO(j) {
            dut.source.send({ .skip = 0, .data = 20, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 0, .data = 21, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 0, .data = 22, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 4, .data = 23, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 4, .data = 80, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 4, .data = 81, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 4, .data = 82, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 4, .data = 83, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 0, .data = 24, .bad = false, .stallForever = false });
            dut.source.send({ .skip = 0, .data = 25, .bad = true, .stallForever = false });
            wait(100, SC_NS);
            dut.source.send({ .skip = 0, .data = 25, .bad = true, .stallForever = true });
        };

        SC_SPAWN_TO(j) {
            while (true) {
                fmt::println("received: {}", dut.sink.receive());
            }
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
    trace_file->open("Transducer.tb.vcd");
#endif

    sc_start(sc_time(1000, SC_NS));
#if defined(VERILATED_TRACE_ENABLED)
    trace_file->close();
#endif

    return 0;
}
