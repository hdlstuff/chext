#include <QueueTestTop1.hpp>

#include <systemc>
#include <verilated_vcd_sc.h>

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

    QueueTestTop1 dut;

    sc_clock clock;
    sc_signal<bool> reset;

    void thread0() {
        reset.write(1);
        wait(10, SC_NS);
        reset.write(0);

        dut.source.send(sc_bv<32>(187));
        dut.source.send(sc_bv<32>(874));
        dut.source.send(sc_bv<32>(991));

        std::cout << dut.sink.receive().to_int() << std::endl;
        std::cout << dut.sink.receive().to_int() << std::endl;
        std::cout << dut.sink.receive().to_int() << std::endl;
    }
};

int sc_main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true);

    Testbench tb;
    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    tb.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open("MyTestBench.vcd");

    sc_start(sc_time(100, SC_NS));
    trace_file->close();

    return 0;
}
