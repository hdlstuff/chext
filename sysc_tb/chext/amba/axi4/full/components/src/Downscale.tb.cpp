#include <DownscaleTestTop1_1.hpp>

#include <verilated_vcd_sc.h>

#include <chext_test/chext_test.hpp>
#include <systemc>
#include <Transaction.hpp>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::amba;

using chext_test::amba::axi4::full::write;
using chext_test::amba::axi4::full::read;

#include <climits>
#include <type_traits>

struct TwoInterfaceTest {
    TwoInterfaceTest(std::string const& name, axi4::full::SlaveBase& normal, axi4::full::SlaveBase& test)
        : name_ { name }
        , normal_ { normal }
        , test_ { test } {
    }

    void runTest(uint64_t addr_, uint64_t length) {
        // step 1: fill in the memory range with data, in the form:
        // fillMemory(normal_, addr + 0x2, length, nullptr);

        uint64_t addr, numBytes;
        uint8_t const* dataPtr;
        uint8_t* dataPtr2;

        uint8_t data[256], data2[256] = { 0 };

        for (unsigned i = 0; i < 256; ++i)
            data[i] = i;

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x00;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        addr = 0x00;
        numBytes = 64;
        dataPtr2 = data2;
        read(normal_, addr, numBytes, dataPtr2, 2);

        for (unsigned idx = 0; idx < 64; ++idx)
            fmt::print("data2[{}] = {}\n", idx, data2[idx]);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x01;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x03;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x04;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 4;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 2);

        wait(5, SC_NS);

        fmt::print(" ---- \n");

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x00;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x01;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x03;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x04;
        numBytes = 32;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 4;
        dataPtr = data;
        write(normal_, addr, numBytes, dataPtr, 1);

        wait(5, SC_NS);
    }

private:
    std::string name_;
    axi4::full::SlaveBase& normal_;
    axi4::full::SlaveBase& test_;
};

class DownscaleTestbench : public TestBenchBase {
public:
    SC_HAS_PROCESS(DownscaleTestbench);

    DownscaleTestbench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);
    }

    DownscaleTestTop1_1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    void entry() override {
        TwoInterfaceTest test("", dut.S_AXI_NORMAL, dut.S_AXI_TEST);
        test.runTest(0x0000, 128);
        finish();
    }
};

int sc_main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true);

    DownscaleTestbench testBench;

    sc_start(SC_ZERO_TIME);

    std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
    testBench.dut.traceVerilated(trace_file.get(), 99);
    trace_file->open("DownscaleTestbench.vcd");

    testBench.start();

    trace_file->close();

    return 0;
}
