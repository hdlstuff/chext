#include <DownscaleTestTop1_1.hpp>
#include <DownscaleTestTop1_2.hpp>

#include <verilated_vcd_sc.h>

#include <Transaction.hpp>
#include <chext_test/chext_test.hpp>

#include <systemc>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::amba;

#include <chrono>
#include <random>

#include <iostream>

#define LOG_ENABLED false

namespace buffer_utils {

namespace detail {

static std::mt19937 mt(879821);
static std::uniform_int_distribution<> dist(0, 1024);

} // namespace detail

template<typename BufferT>
void reset(BufferT& buffer) {
    using value_type = std::remove_reference_t<decltype(*std::begin(buffer))>;

    for (auto it = std::begin(buffer); it != std::end(buffer); ++it)
        *it = ((value_type)0);
}

template<typename BufferT>
void linearInit(BufferT& buffer) {
    using value_type = std::remove_reference_t<decltype(*std::begin(buffer))>;

    value_type idx = (value_type)0;
    for (auto it = std::begin(buffer); it != std::end(buffer); ++it)
        *it = (idx++);
}

template<typename BufferT>
void randomInit(BufferT& buffer) {
    using value_type = std::remove_reference_t<decltype(*std::begin(buffer))>;

    for (auto it = std::begin(buffer); it != std::end(buffer); ++it)
        *it = ((value_type)detail::dist(detail::mt));
}

} // namespace buffer_utils

class DownscaleTestbench : public TestBenchBase {
public:
    SC_HAS_PROCESS(DownscaleTestbench);

    DownscaleTestbench()
        : TestBenchBase(sc_module_name("tb"))
        , dut1 { "dut1" }
        , dut2 { "dut2" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut1.clock(clock);
        dut1.reset(reset);

        dut2.clock(clock);
        dut2.reset(reset);
    }

    DownscaleTestTop1_1 dut1;
    DownscaleTestTop1_2 dut2;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    void entry() override {
        resetDUTs();

        SC_SPAWN {
            while (true) {
                fmt::print("\r{:=^100}", fmt::format("  t = {}  ", sc_time_stamp().to_string()));
                std::cout.flush();
                wait(100, SC_US);
            }
        };

        readWriteTestCase(dut1.S_AXI_TEST, dut1.S_AXI_TEST, 0x00, 1024);
        readWriteTestCase(dut1.S_AXI_NORMAL, dut1.S_AXI_TEST, 0x00, 1024);
        readWriteTestCase(dut1.S_AXI_TEST, dut1.S_AXI_NORMAL, 0x00, 1024);

        readWriteTestCase(dut2.S_AXI_TEST, dut2.S_AXI_TEST, 0x00, 1024);
        readWriteTestCase(dut2.S_AXI_NORMAL, dut2.S_AXI_TEST, 0x00, 1024);
        readWriteTestCase(dut2.S_AXI_TEST, dut2.S_AXI_NORMAL, 0x00, 1024);

        fmt::print("\r{:~^100}\n", fmt::format("  simulation time: {}  ", sc_time_stamp().to_string()));

        finish();
    }

    void readWriteTestCase(
        axi4::full::SlaveBase& writeSlave,
        axi4::full::SlaveBase& readSlave,
        uint64_t addr,
        uint64_t numBytes
    ) {
        using axi4::full::read;
        using axi4::full::write;

        std::vector<uint8_t> rdBuffer(numBytes), wrBuffer(numBytes);

        for (uint64_t offset = 0; offset < 128; ++offset) {
            for (int size = -1; size < 2; ++size) {
                buffer_utils::linearInit(wrBuffer);
                write(writeSlave, addr + offset, numBytes, wrBuffer.data(), size, false);
                read(readSlave, addr + offset, numBytes, rdBuffer.data(), size, false);
                ASSERT_(rdBuffer == wrBuffer);
            }
        }

        for (uint64_t offset = 0; offset < 128; ++offset) {
            for (int size = -1; size < 2; ++size) {
                buffer_utils::randomInit(wrBuffer);
                write(writeSlave, addr + offset, numBytes, wrBuffer.data(), size, false);
                read(readSlave, addr + offset, numBytes, rdBuffer.data(), size, false);
                ASSERT_(rdBuffer == wrBuffer);
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

    DownscaleTestbench testBench;

    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    testBench.dut1.traceVerilated(trace_file.get(), 99);
    testBench.dut2.traceVerilated(trace_file.get(), 99);
    trace_file->open("DownscaleTestbench.vcd");

    testBench.start();

    trace_file->close();

    return 0;
}
