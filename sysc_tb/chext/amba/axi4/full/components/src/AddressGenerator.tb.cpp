#include <AddressGenerator.hpp>
#include <chext_test/util/Spawn.hpp>
#include <systemc>

using namespace sc_core;
using namespace sc_dt;

class AddressGeneratorTestbench : public sc_module {
public:
    SC_HAS_PROCESS(AddressGeneratorTestbench);

    AddressGeneratorTestbench()
        : sc_module(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);

        SC_THREAD(thread);
    }

    bool isDone() const noexcept {
        return done_;
    }

private:
    AddressGenerator dut;

    sc_clock clock;
    sc_signal<bool> reset;

    bool done_ { false };

    void thread() {
        using namespace protocols;

        wait(clock.negedge_event());
        reset.write(true);

        wait(clock.negedge_event());
        wait(clock.negedge_event());

        reset.write(false);

        wait(clock.negedge_event());

        sc_join j;

        SC_SPAWN_TO(j) {
            dut.source.send(AddrLenSizeBurst { .addr = "0x000", .len = 0, .size = 3, .burst = 1 });
        };

        SC_SPAWN_TO(j) {
            auto pkt = dut.sink.receiveAs<AddrSizeLast>();
            fmt::print("received = {}\n", pkt.size);
        };

        j.wait();

        done_ = true;
    }
};

int sc_main(int argc, char** argv) {
    AddressGeneratorTestbench tb;

    while (!tb.isDone()) {
        sc_start(50, SC_NS);
    }

    return 0;
}
