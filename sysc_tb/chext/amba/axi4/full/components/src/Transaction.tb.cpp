#include <fmt/core.h>
#include <systemc>

#include <Transaction.hpp>

using namespace chext_test;
using namespace chext_test::amba;

struct TestCase {
    uint16_t wData;

    uint64_t addr;
    uint8_t len;
    uint8_t size;
    uint8_t burst;

    void run() const {
        fmt::print("{}\n", *this);

        axi4::full::Transaction transaction { wData };
        transaction.reset(addr, len, size, burst);

        axi4::full::Beat b;
        while (transaction.nextBeat(b)) {
            fmt::print("{}\n", b);
        }
    }

    // clang-format off
    JQR_DECL(
        TestCase,
        JQR_MEMBER(wData, jqr::opts::dump_fmt { "{:#06d}" }),
        JQR_MEMBER(addr, jqr::opts::dump_fmt { "{:#018x}" }),
        JQR_MEMBER(len, jqr::opts::dump_fmt { "{:#04x}" }),
        JQR_MEMBER(size, jqr::opts::dump_fmt { "{:#03d}" }),
        JQR_MEMBER(burst, jqr::opts::dump_fmt { "{:#04b}" })
    )
    // clang-format on
};

std::vector<TestCase> testCases {
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 0, 0 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 1, 0 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 2, 0 },

    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 0, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 1, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 2, 1 },

    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 0, 2 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 1, 2 },
    { .wData = 32 /* 4B */, .addr = 0x0000, 3, 2, 2 },

    { .wData = 32 /* 4B */, .addr = 0x0001, 3, 0, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0001, 3, 1, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0001, 3, 2, 1 },

    { .wData = 32 /* 4B */, .addr = 0x0002, 3, 0, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0002, 3, 1, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0002, 3, 2, 1 },

    { .wData = 32 /* 4B */, .addr = 0x0003, 3, 0, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0003, 3, 1, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0003, 3, 2, 1 },

    { .wData = 32 /* 4B */, .addr = 0x0005, 3, 0, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0005, 3, 1, 1 },
    { .wData = 32 /* 4B */, .addr = 0x0005, 3, 2, 1 },

    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 0, 0 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 1, 0 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 2, 0 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 3, 0 },

    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 0, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 1, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 2, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 3, 1 },

    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 0, 2 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 1, 2 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 2, 2 },
    { .wData = 64 /* 8B */, .addr = 0x0000, 3, 3, 2 },

    { .wData = 64 /* 8B */, .addr = 0x0001, 3, 0, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0001, 3, 1, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0001, 3, 2, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0001, 3, 3, 1 },

    { .wData = 64 /* 8B */, .addr = 0x0002, 3, 0, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0002, 3, 1, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0002, 3, 2, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0002, 3, 3, 1 },

    { .wData = 64 /* 8B */, .addr = 0x0003, 3, 0, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0003, 3, 1, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0003, 3, 2, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0003, 3, 3, 1 },

    { .wData = 64 /* 8B */, .addr = 0x0005, 3, 0, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0005, 3, 1, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0005, 3, 2, 1 },
    { .wData = 64 /* 8B */, .addr = 0x0005, 3, 3, 1 }
};

int sc_main(int argc, char** argv) {
    for (auto const& testCase : testCases) {
        testCase.run();
    }
    return 0;
}
