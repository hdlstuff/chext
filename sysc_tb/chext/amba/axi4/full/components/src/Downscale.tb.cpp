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
        sc_bv_base result(width);
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

sc_bv_base createStrobe() {
}

using addr_t = uint64_t;

struct Beat {
    uint8_t index;

    uint64_t addr;
    uint8_t size;
    bool last;

    uint64_t strb;

    uint8_t lowerByteIndex;
    uint8_t upperByteIndex;

    JQR_DECL(
        Beat,
        JQR_MEMBER(index),
        JQR_MEMBER(addr, jqr::opts::dump_fmt { "{:#018x}" }),
        JQR_MEMBER(size, jqr::opts::dump_fmt { "{:#04x}" }),
        JQR_MEMBER(last, jqr::opts::dump_fmt { "{:#03b}" }),
        JQR_MEMBER(strb, jqr::opts::dump_fmt { "{:#010b}" }),
        JQR_MEMBER(lowerByteIndex),
        JQR_MEMBER(upperByteIndex)
    )

    JQR_TO_STRING
};

struct Transfer {
    Transfer(axi4::full::Config const& cfg)
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
            uint64_t transactionSize = ((uint64_t)1) << (log2numBeats + size);
            assert((addr & (transferSize - 1)) == 0 && "for wrap burst, it must be aligned!");
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

        if (beatIndex_ == 0) {
            new (&beat) Beat {
                .index = (uint8_t)beatIndex_,
                .addr = firstAddr_,
                .size = size_,
                .last = (beatIndex_ == len_),
                .strb = 0
            };
        } else {
            new (&beat) Beat {
                .index = (uint8_t)beatIndex_,
                .addr = addr_ << size_,
                .size = size_,
                .last = (beatIndex_ == len_),
                .strb = 0
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
    axi4::full::Config cfg_;

    unsigned beatIndex_;

    uint64_t firstAddr_;
    uint64_t addr_;

    uint8_t len_;
    uint8_t size_;
    uint8_t burst_;
};

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
    unsigned totalTransfers = 1 + ((totalBeats - 1) / maxBeats);

    Transfer transfer(cfg);

    transfer.reset(base, 255, log2(busBytes), 1);

    sc_join j;

    SC_SPAWN_TO(j) {
        axi4::full::Packets::Address aw {
            .id = bv_from(0, cfg.wId > 0 ? cfg.wId : 4),
            .addr = bv_from(base, cfg.wAddr),
            .size = log2(busBytes),
            .burst = 1,
            .len = 255
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

struct TwoInterfaceTest {
    TwoInterfaceTest(std::string const& name, axi4::full::SlaveBase& normal, axi4::full::SlaveBase& test)
        : name_ { name }
        , normal_ { normal }
        , test_ { test } {
    }

    void runTest(uint64_t addr, uint64_t length) {
        // step 1: fill in the memory range with data, in the form:
        fillMemory(normal_, addr, length, nullptr);
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
