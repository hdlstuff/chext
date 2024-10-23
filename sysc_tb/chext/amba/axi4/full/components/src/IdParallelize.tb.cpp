#include <IdParallelizeTestTop2_1.hpp>

#include <verilated_vcd_sc.h>

#include <systemc>

#include <Util.hpp>
#include <chext_test/chext_test.hpp>

#include <chrono>
#include <random>

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
        , numThreads { 1u << dut.M_AXI.config().wId }
        , arFifos { numThreads }
        , rFifo {} {

        dut.clock(clock);
        dut.reset(reset);
    }

    IdParallelizeTestTop2_1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    unsigned numThreads;
    std::vector<sc_fifo<axi4::full::Packets::ReadAddress>> arFifos;
    sc_fifo<axi4::full::Packets::ReadData> rFifo;

    unsigned numTransactions = 4096;
    unsigned addrOffset = 16;

    std::mt19937 mt { 566542 };
    std::uniform_int_distribution<> distBeats { 0, 32 };
    std::uniform_int_distribution<> distWait { 20, 50 };

    void entry() override {
        resetDUTs();

        for (unsigned id = 0; id < numThreads; ++id) {
            sc_spawn([this, id] { idThread(id); });
        }

        sc_spawn([this] { m_axi_ar(); });
        sc_spawn([this] { m_axi_r(); });

        sc_join j;

        j.add_process(sc_spawn([this] { s_axi_ar(); }));
        j.add_process(sc_spawn([this] { s_axi_r(); }));

        j.wait();

        finish();
    }

    void s_axi_ar() {
        for (uint64_t i = 0; i < numTransactions; ++i) {
            axi4::full::Packets::ReadAddress ar {
                .id = bv_from(i & (numThreads - 1)),
                .addr = bv_from(i << addrOffset),
                .len = (uint8_t) distBeats(mt)
            };
            dut.S_AXI.sendAR(ar);
            fmt::print("[{:^20}] [{:^20}] dut.S_AXI.sendAR({})\n", sc_time_stamp().to_string(), "s_axi_ar", ar);
        }
    }

    void s_axi_r() {
        for (unsigned i = 0; i < numTransactions; ++i) {
            for (unsigned j = 0;; ++j) {
                auto r = dut.S_AXI.receiveR();
                fmt::print("[{:^20}] [{:^20}] dut.S_AXI.receiveR() = {}\n", sc_time_stamp().to_string(), "s_axi_r", r);

                auto received = r.data.to_uint64();
                auto expected = (((uint64_t)i) << addrOffset) + j;

                if (r.last)
                    break;
            }
        }
    }

    void m_axi_ar() {
        while (true) {
            auto ar = dut.M_AXI.receiveAR();
            fmt::print("[{:^20}] [{:^20}] dut.M_AXI.receiveAR() = {}\n", sc_time_stamp().to_string(), "m_axi_ar", ar);
            arFifos.at(ar.id.to_uint64()).write(ar);
        }
    }

    void m_axi_r() {
        while (true) {
            auto r = rFifo.read();
            dut.M_AXI.sendR(r);
            fmt::print("[{:^20}] [{:^20}] dut.M_AXI.sendR({})\n", sc_time_stamp().to_string(), "m_axi_r", r);
        }
    }

    void idThread(uint32_t id) {
        while (true) {
            auto ar = arFifos[id].read();
            fmt::print("[{:^20}] [{:^20}] arFifos[id].read() = {}\n", sc_time_stamp().to_string(), fmt::format("idThread({:^4d})", id), ar);

            wait(distWait(mt), SC_NS);

            for (unsigned j = 0; j <= ar.len; ++j) {
                axi4::full::Packets::ReadData r {
                    .id = bv_from(id),
                    .data = bv_from(ar.addr.to_uint64() + j),
                    .resp = 0,
                    .last = (j == ar.len)
                };
                rFifo.write(r);

                wait(distWait(mt), SC_NS);
            }
        }
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
