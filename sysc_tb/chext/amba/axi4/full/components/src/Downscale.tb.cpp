#include <DownscaleTestTop1_1.hpp>

#include <verilated_vcd_sc.h>

#include <chext_test/chext_test.hpp>
#include <systemc>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::amba;

#include <climits>
#include <type_traits>

template<typename T, typename Enable = void>
struct bv_from_impl;

template<typename T>
struct bv_from_impl<T, std::enable_if_t<std::is_integral_v<T>>> {
    static auto bv_from(T t, int width = (sizeof(T) * CHAR_BIT)) {
        sc_bv_base result(width > 0 ? width : 32);
        result = t;
        return result;
    }
};

template<typename T>
inline auto bv_from(T t) {
    return bv_from_impl<T>::bv_from(t);
}

template<typename T>
inline auto bv_from(T t, int width) {
    return bv_from_impl<T>::bv_from(t, width);
}

// maybe do this in a more concrete way?

#define LOG2_IMPL(type, return_type, clz)              \
    inline constexpr return_type log2(type x) {        \
        return (sizeof(type) * CHAR_BIT) - 1 - clz(x); \
    }

LOG2_IMPL(unsigned char, uint8_t, __builtin_clz);
LOG2_IMPL(unsigned short, uint8_t, __builtin_clz);
LOG2_IMPL(unsigned int, uint8_t, __builtin_clz);
LOG2_IMPL(unsigned long, uint8_t, __builtin_clzl);
LOG2_IMPL(unsigned long long, uint8_t, __builtin_clzll);

#define MIN(x, y) (((x) < (y)) ? (x) : (y))
#define MAX(x, y) (((x) > (y)) ? (x) : (y))

using addr_t = uint64_t;

struct Beat {
    uint32_t index;
    uint64_t addr;
    bool last;

    uint32_t lowerByteIndex;
    uint32_t upperByteIndex;
    uint32_t size;
    uint64_t strb;

    JQR_DECL(
        Beat,
        JQR_MEMBER(index),
        JQR_MEMBER(addr, jqr::opts::dump_fmt { "{:#018x}" }),
        JQR_MEMBER(last, jqr::opts::dump_fmt { "{:#03b}" }),
        JQR_MEMBER(lowerByteIndex),
        JQR_MEMBER(upperByteIndex),
        JQR_MEMBER(size, jqr::opts::dump_fmt { "{:#04x}" }),
        JQR_MEMBER(strb, jqr::opts::dump_fmt { "{:#010x}" })
    )

    JQR_TO_STRING
};

void prepareData(sc_bv_base& out, uint8_t const* in, uint8_t lowerByteIndex, uint8_t upperByteIndex) {
    uint8_t buffer[128] = { 0 };

    for (unsigned i = lowerByteIndex; i <= upperByteIndex; ++i)
        buffer[i] = *(in++);

    for (unsigned wordIndex = 0; wordIndex < out.size(); ++wordIndex) {
        static_assert(sizeof(sc_digit) == 4);

        // clang-format off
        out.set_word(
            wordIndex,
            (((sc_digit)buffer[wordIndex * 4]) << 0) |
            (((sc_digit)buffer[wordIndex * 4 + 1]) << 8) |
            (((sc_digit)buffer[wordIndex * 4 + 2]) << 16) |
            (((sc_digit)buffer[wordIndex * 4 + 3]) << 24)
        );
        // clang-format on
    }
}

struct Transaction {
    Transaction(axi4::full::Config const& cfg)
        : cfg_ { cfg } {
        assert(cfg.wAddr <= 64);
        assert(cfg.wData <= 256);
    }

    void reset(uint64_t addr, uint8_t len, uint8_t size, uint8_t burst) {
        assert(burst == 0 || burst == 1 || burst == 2);

        if (cfg_.axi3Compat) {
            assert(len < 16 && "in axi3 mode, len < 16!");
        }

        if (burst == 2 /* WRAP */) {
            assert(len == 0 || len == 1 || len == 3 || len == 7 || len == 15);
            uint8_t log2numBeats = log2(len + 1);

            // the total size of the transfer should be aligned
            uint64_t transferSize = ((uint64_t)1) << size;
            assert((addr & (transferSize - 1)) == 0 && "for wrap burst, it must be aligned to the transfer size!");
        }

        firstAddr_ = addr;
        addr_ = addr >> size;
        len_ = len;
        size_ = size;
        burst_ = burst;

        beatIndex_ = 0;
    }

    bool nextBeat(Beat& beat) {
        if (beatIndex_ > len_)
            return false;

        beat.~Beat();

        uint64_t busMask = (cfg_.wData / 8) - 1;

        if (beatIndex_ == 0 || burst_ == 0) {
            uint64_t addr = firstAddr_;

            uint32_t lowerByteIndex = addr & busMask;
            uint32_t upperByteIndex = (((1 + (addr >> size_)) << size_) - 1) & busMask;
            uint32_t size = (upperByteIndex - lowerByteIndex) + 1;
            uint64_t strb = ((((uint64_t)1) << size) - 1) << lowerByteIndex;

            new (&beat) Beat {
                .index = beatIndex_,
                .addr = addr,
                .last = (beatIndex_ == len_),
                .lowerByteIndex = lowerByteIndex,
                .upperByteIndex = upperByteIndex,
                .size = size,
                .strb = strb
            };
        } else {
            uint64_t addr = addr_ << size_;

            uint32_t lowerByteIndex = addr & busMask;
            uint32_t upperByteIndex = (((1 + (addr >> size_)) << size_) - 1) & busMask;
            uint32_t size = (upperByteIndex - lowerByteIndex) + 1;
            uint64_t strb = ((((uint64_t)1) << size) - 1) << lowerByteIndex;

            new (&beat) Beat {
                .index = beatIndex_,
                .addr = addr,
                .last = (beatIndex_ == len_),
                .lowerByteIndex = lowerByteIndex,
                .upperByteIndex = upperByteIndex,
                .size = size,
                .strb = strb
            };
        }

        if (burst_ == 0 /* FIXED */) {
            // do nothing
        } else if (burst_ == 1 /* INCR */) {
            addr_++;
        } else /* WRAP */ {
            uint64_t mask = len_;
            addr_ = ((addr_ + 1) & mask) | ((addr_) & ~mask);
        }

        beatIndex_++;

        return true;
    }

private:
    axi4::full::Config const& cfg_;

    unsigned beatIndex_;

    uint64_t firstAddr_;
    uint64_t addr_;

    uint8_t len_;
    uint8_t size_;
    uint8_t burst_;
};

void simpleWrite(axi4::full::SlaveBase& target, uint64_t& addr, uint64_t& numBytes, unsigned char const* data, int size = -1) {
    auto const& cfg = target.config();
    unsigned maxSize = log2(cfg.wData / 8u);
    size = (size >= 0 && size <= maxSize) ? size : maxSize;

    uint64_t mask = (((uint64_t)1) << size) - 1;
    uint64_t alignedAddr = addr & ~mask;
    uint64_t alignedNumBytes = numBytes + addr - alignedAddr;

    uint64_t numBeats = alignedNumBytes >> size;
    if (alignedNumBytes & mask)
        numBeats++;

    uint8_t len = numBeats - 1;

    // fmt::print("addr = {:08x}, numBytes = {}, size = {}, alignedAddr = {:08x}, numBeats = {}\n", addr, numBytes, size, alignedAddr, numBeats);

    Transaction transaction(cfg);
    transaction.reset(addr, len, size, 1);

    sc_join j;

    SC_SPAWN_TO(j) {
        axi4::full::Packets::Address aw {
            .id = bv_from(0, cfg.wId),
            .addr = bv_from(addr, cfg.wAddr),
            .size = (uint8_t)size,
            .burst = 1,
            .len = (uint8_t)len
        };

        target.sendAW(aw);
        fmt::print("[t = {}] sent: {}\n", sc_time_stamp().to_string(), aw);
    };

    SC_SPAWN_TO(j) {
        sc_bv_base bvData((int)cfg.wData);
        sc_bv_base bvStrb((int)cfg.wStrb);

        Beat b;

        while (transaction.nextBeat(b)) {
            prepareData(bvData, data, b.lowerByteIndex, b.upperByteIndex);

            axi4::full::Packets::WriteData w {
                .data = bvData,
                .strb = bvStrb,
                .last = b.last
            };

            target.sendW(w);
            fmt::print("[t = {}] sent: {}\n", sc_time_stamp().to_string(), w);

            addr += b.size;
            data += b.size;
            numBytes -= b.size;
        }
    };

    SC_SPAWN_TO(j) {
        auto b = target.receiveB();
        fmt::print("[t = {}] received: {}\n", sc_time_stamp().to_string(), b);
    };

    j.wait();
}
/*
void fillMemory(
    axi4::full::SlaveBase& target,
    uint64_t base,
    uint64_t size,
    unsigned char const* data
) {
    auto const& cfg = target.config();
    sc_bv_base bits((int)cfg.wData);

    unsigned busBytes = cfg.wData / 8u;
    unsigned maxBeats = (1u << cfg.wLen);

    unsigned totalBeats = 1 + ((size - 1) / busBytes);
    unsigned totalTransactions = 1 + ((totalBeats - 1) / maxBeats);

    Transaction transfer(cfg);

    transfer.reset(base, 15, log2(busBytes), 1);

    sc_join j;

    SC_SPAWN_TO(j) {
        axi4::full::Packets::Address aw {
            .id = bv_from(0, cfg.wId > 0 ? cfg.wId : 4),
            .addr = bv_from(base, cfg.wAddr),
            .size = log2(busBytes),
            .burst = 1,
            .len = 15
        };

        target.sendAW(aw);
        fmt::print("sent = {}\n", aw);
    };

    SC_SPAWN_TO(j) {
        Beat beat;

        while (transfer.nextBeat(beat)) {
            fmt::print("beat = {}\n", beat);

            axi4::full::Packets::WriteData w {
                .data = bv_from(0xCAFE'BABE, cfg.wData),
                .strb = bv_from(0xF, cfg.wStrb),
                .last = beat.last
            };

            target.sendW(w);
            fmt::print("sent = {}\n", w);
        }
    };

    SC_SPAWN_TO(j) {
        fmt::print("received = {}\n", target.receiveB());
    };

    j.wait();
}
*/

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

        uint8_t data[256];

        for (unsigned i = 0; i < 256; ++i)
            data[i] = i;

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x00;
        numBytes = 32;
        simpleWrite(normal_, addr, numBytes, data);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x01;
        numBytes = 32;
        simpleWrite(normal_, addr, numBytes, data);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 32;
        simpleWrite(normal_, addr, numBytes, data);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x03;
        numBytes = 32;
        simpleWrite(normal_, addr, numBytes, data);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x04;
        numBytes = 32;
        simpleWrite(normal_, addr, numBytes, data);

        wait(5, SC_NS);

        fmt::print("{} {}:{}\n", __PRETTY_FUNCTION__, __FILE__, __LINE__);
        addr = 0x02;
        numBytes = 4;
        simpleWrite(normal_, addr, numBytes, data);

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
