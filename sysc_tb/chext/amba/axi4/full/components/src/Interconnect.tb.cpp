#include <Interconnect_Tbtop_1.hpp>

#include <verilated_vcd_sc.h>

#include <chext_test/chext_test.hpp>
#include <chext_test/util/Spawn.hpp>

#include <systemc>

using namespace sc_core;
using namespace sc_dt;

using namespace chext_test;
using namespace chext_test::amba;

// Some statistics:
// With RR arbiter, took 15559433 ns
// With Priority arbiter, took 9170045 ns

static constexpr struct {
    bool log = false;
    bool trace = false;
} settings;

struct InterconnectTestbench : TestBenchBase {
    SC_HAS_PROCESS(InterconnectTestbench);

    InterconnectTestbench()
        : TestBenchBase(sc_module_name("tb"))
        , dut { "dut" }
        , clock { "clock", 2.0, SC_NS }
        , reset { "reset" } {

        dut.clock(clock);
        dut.reset(reset);

        for (uint32_t i = 0; i < M; ++i)
            slaves[i] = (&dut.S_AXI_00) + i;

        for (uint32_t i = 0; i < N; ++i)
            masters[i] = (&dut.M_AXI_00) + i;
    }

    Interconnect_Tbtop_1 dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    static constexpr unsigned M = 16;
    static constexpr unsigned N = 16;

    using SlaveT = decltype(Interconnect_Tbtop_1::S_AXI_00);
    using MasterT = decltype(Interconnect_Tbtop_1::M_AXI_00);
    using Packets = amba::axi4::full::Packets;

    std::array<SlaveT*, M> slaves;
    std::array<MasterT*, N> masters;

    static constexpr unsigned threadShift = 2;
    static constexpr unsigned threadMask = (1 << threadShift) - 1;
    static constexpr unsigned threadsPerSlave = 1 << threadShift;
    static constexpr unsigned numThreads = N * threadsPerSlave;

    struct ThreadInfo {
        std::deque<Packets::ReadAddress> arTasks, arExpected;
        std::deque<Packets::ReadData> rTasks, rExpected;
        std::deque<Packets::WriteAddress> awTasks, awExpected;
        std::deque<std::vector<Packets::WriteData>> wTasks, wExpected;
        std::deque<Packets::WriteResponse> bTasks, bExpected;
    };

    struct MasterInfo {
        uint32_t ar = 0, aw = 0, w = 0;
    };
    struct SlaveInfo {
        uint32_t r = 0, b = 0;
    };

    std::array<ThreadInfo, numThreads> threadInfos;
    std::array<MasterInfo, M> masterInfos;
    std::array<SlaveInfo, N> slaveInfos;

    void entry() {
        resetDUTs();
        createTasks();

        if (!settings.log) {
            SC_SPAWN {
                while (true) {
                    fmt::print("\r{:=^100}", fmt::format("  t = {}  ", sc_time_stamp().to_string()));
                    std::cout.flush();
                    wait(100, SC_US);
                }
            };
        }

        sc_join j;

        for (int i = 0; i < M; ++i) {
            auto handle = sc_spawn([this, i] { handleSlave(i); });
            j.add_process(handle);
        }
        for (int i = 0; i < N; ++i) {
            auto handle = sc_spawn([this, i] { handleMaster(i); });
            j.add_process(handle);
        }

        j.wait();
        fmt::print("\nSimulation done at t = {}\n", sc_time_stamp().to_string());

        finish();
    }

    void resetDUTs() {
        reset.write(true);
        wait(clock.negedge_event());
        wait(clock.negedge_event());
        reset.write(false);
        wait(clock.negedge_event());
    }

    void createTasks() {
        fmt::print("Creating tasks...");
        for (uint32_t slaveIdx = 0; slaveIdx < M; ++slaveIdx) {
            for (uint32_t masterIdx = 0; masterIdx < N; ++masterIdx) {
                for (uint8_t id = 0; id < 4; ++id) {
                    for (uint8_t idx = 0; idx < 25; ++idx) {
                        readTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );

                        writeTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );
                    }
                }
            }
        }

        for (uint32_t slaveIdx = 0; slaveIdx < M; ++slaveIdx) {
            for (uint8_t id = 0; id < 4; ++id) {
                for (uint8_t idx = 0; idx < 25; ++idx) {
                    for (uint32_t masterIdx = 0; masterIdx < N; ++masterIdx) {
                        readTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );

                        writeTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );
                    }
                }
            }
        }

        for (uint8_t id = 0; id < 4; ++id) {
            for (uint8_t idx = 0; idx < 25; ++idx) {
                for (uint32_t masterIdx = 0; masterIdx < N; ++masterIdx) {
                    for (uint32_t slaveIdx = 0; slaveIdx < M; ++slaveIdx) {
                        readTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );

                        writeTask(
                            slaveIdx,
                            masterIdx,
                            (masterIdx << 12) + (rand() & 0xFFF),
                            rand() & 0xFF,
                            id
                        );
                    }
                }
            }
        }

        fmt::print("done.\n");
    }

    void waitRandom(int maxCycles) {
        int delay = rand() % (maxCycles + 1);

        for (int i = 0; i < delay; ++i) {
            wait(clock.negedge_event());
        }
    }

    template<typename T>
    void logMaster(uint16_t idx, const std::string& msg, const T& packet) {
        if (!settings.log)
            return;

        std::cout << fmt::format("[{:^6}] [Master {:02}] {}: {}\n", sc_time_stamp().to_string(), idx, msg, packet);
    }

    inline void logMaster(uint16_t idx, const std::string& msg) {
        if (!settings.log)
            return;

        std::cout << fmt::format("[{:^6}] [Master {:02}] {}\n", sc_time_stamp().to_string(), idx, msg);
    }

    template<typename T>
    void logSlave(uint16_t idx, const std::string& msg, const T& packet) {
        if (!settings.log)
            return;

        std::cout << fmt::format("[{:^6}] [Slave  {:02}] {}: {}\n", sc_time_stamp().to_string(), idx, msg, packet);
    }

    inline void logSlave(uint16_t idx, const std::string& msg) {
        if (!settings.log)
            return;

        std::cout << fmt::format("[{:^6}] [Slave  {:02}] {}\n", sc_time_stamp().to_string(), idx, msg);
    }

    void readTask(uint32_t slaveIdx, uint32_t masterIdx, uint32_t addr, uint8_t len, uint16_t id) {
        auto& slaveInfo = slaveInfos[slaveIdx];
        auto& masterInfo = masterInfos[masterIdx];

        int threadIdx = id + (slaveIdx << threadShift);
        auto& threadInfo = threadInfos[threadIdx];

        threadInfo.arTasks.push_back({ sc_bv<2>(id), sc_bv<32>(addr), len });
        for (int i = 0; i <= len; ++i)
            threadInfo.rTasks.push_back({ sc_bv<6>(threadIdx), sc_bv<32>(rand() & 0xffffffff), 0, i == len });

        slaveInfo.r += (len + 1);
        masterInfo.ar += 1;
    }

    void writeTask(uint32_t slaveIdx, uint32_t masterIdx, uint32_t addr, uint8_t len, uint16_t id) {
        auto& slaveInfo = slaveInfos[slaveIdx];
        auto& masterInfo = masterInfos[masterIdx];

        int threadIdx = id + (slaveIdx << threadShift);
        auto& threadInfo = threadInfos[threadIdx];

        threadInfo.awTasks.push_back({ sc_bv<2>(id), sc_bv<32>(addr), len });

        std::vector<Packets::WriteData> burst;
        for (int i = 0; i <= len; ++i)
            burst.push_back({ sc_bv<32>(rand() & 0xffffffff), sc_bv<4>(0xf), i == len });

        threadInfo.wTasks.push_back(burst);
        threadInfo.bTasks.push_back({ sc_bv<6>(threadIdx), 0 });

        slaveInfo.b += 1;
        masterInfo.aw += 1;
        masterInfo.w += 1;
    }

    void handleMaster(uint32_t masterIdx) {
        auto& master = *masters[masterIdx];
        auto& masterInfo = masterInfos[masterIdx];

        sc_join j;

        auto readProcess = sc_spawn([&] {
            while (masterInfo.ar > 0) {
                logMaster(masterIdx, fmt::format("Remaining AR packets = {}", masterInfo.ar));
                logMaster(masterIdx, "waiting for AR");
                auto ar = master.receiveAR();
                logMaster(masterIdx, "received AR", ar);

                uint32_t threadIdx = ar.id.to_uint();
                auto& threadInfo = threadInfos[threadIdx];

                EXPECT_(!threadInfo.arExpected.empty());
                EXPECT_EQ(threadInfo.arExpected.front(), ar);

                threadInfo.arExpected.pop_front();

                EXPECT_(!threadInfo.rTasks.empty());

                for (uint32_t i = 0; i <= ar.len; ++i) {
                    auto r = threadInfo.rTasks.front();
                    threadInfo.rTasks.pop_front();

                    EXPECT_EQ(r.id.to_uint64(), ar.id.to_uint64());
                    auto rExpected = r;
                    new (&rExpected.id) sc_bv<2>(ar.id.to_uint() & threadMask);
                    threadInfo.rExpected.push_back(rExpected);

                    waitRandom(4);
                    logMaster(masterIdx, "send R", r);
                    master.sendR(r);
                }

                masterInfo.ar -= 1;
            }
        });
        j.add_process(readProcess);

        auto writeProcess = sc_spawn([&] {
            while (masterInfo.aw > 0 || masterInfo.w > 0) {
                logMaster(masterIdx, fmt::format("Remaining AW = {}, W = {}", masterInfo.aw, masterInfo.w));

                std::optional<Packets::WriteAddress> awOpt;
                std::optional<std::vector<Packets::WriteData>> wOpt;

                sc_join innerJoin;
                innerJoin.add_process(sc_spawn([&] {
                    if (masterInfo.aw > 0) {
                        logMaster(masterIdx, "waiting for AW");
                        awOpt = master.receiveAW();
                        logMaster(masterIdx, "received AW", *awOpt);
                        masterInfo.aw -= 1;
                    }
                }));
                innerJoin.add_process(sc_spawn([&] {
                    if (masterInfo.w > 0) {
                        logMaster(masterIdx, "waiting for W");

                        std::vector<Packets::WriteData> w;
                        do {
                            w.push_back(master.receiveW());
                        } while (!w.back().last);
                        wOpt = w;

                        fmt::memory_buffer buf;
                        fmt::format_to(std::back_inserter(buf), "[");
                        for (size_t i = 0; i < w.size(); ++i) {
                            if (i > 0)
                                fmt::format_to(std::back_inserter(buf), ", ");
                            fmt::format_to(std::back_inserter(buf), "{}", w[i]);
                        }
                        fmt::format_to(std::back_inserter(buf), "]");

                        logMaster(masterIdx, fmt::format("received W {}", fmt::to_string(buf)));
                        masterInfo.w -= 1;
                    }
                }));
                innerJoin.wait();

                auto& aw = *awOpt;
                auto& w = *wOpt;

                uint32_t threadIdx = aw.id.to_uint();
                auto& threadInfo = threadInfos[threadIdx];

                EXPECT_(!threadInfo.awExpected.empty());
                EXPECT_EQ(threadInfo.awExpected.front(), aw);
                threadInfo.awExpected.pop_front();

                EXPECT_(!threadInfo.wExpected.empty());
                EXPECT_(threadInfo.wExpected.front() == w); // format correctly, use _EQ
                threadInfo.wExpected.pop_front();

                EXPECT_(!threadInfo.bTasks.empty());
                auto b = threadInfo.bTasks.front();
                threadInfo.bTasks.pop_front();

                EXPECT_EQ(b.id.to_uint64(), aw.id.to_uint64());
                auto bExpected = b;
                new (&bExpected.id) sc_bv<2>(aw.id.to_uint() & threadMask);
                threadInfo.bExpected.push_back(bExpected);

                waitRandom(16);
                logMaster(masterIdx, "send B", b);
                master.sendB(b);
            }
        });
        j.add_process(writeProcess);

        j.wait();
        logMaster(masterIdx, "Complete.");
    }

    void handleSlave(uint32_t slaveIdx) {
        auto& slave = *slaves[slaveIdx];
        auto& slaveInfo = slaveInfos[slaveIdx];

        sc_join j;

        auto arThread = sc_spawn([&] {
            bool done = false;
            while (!done) {
                bool sentAny = false;
                for (uint32_t tid = 0; tid < threadsPerSlave; ++tid) {
                    uint32_t threadIdx = tid + (slaveIdx << threadShift);
                    auto& threadInfo = threadInfos[threadIdx];

                    uint32_t popNum = 1 + (rand() % 4);
                    for (uint32_t i = 0; i < popNum; ++i) {
                        if (!threadInfo.arTasks.empty()) {
                            auto ar = threadInfo.arTasks.front();
                            threadInfo.arTasks.pop_front();
                            new (&ar.id) sc_bv<6>(threadIdx);
                            threadInfo.arExpected.push_back(ar);
                            logSlave(slaveIdx, "send AR", ar);
                            slave.sendAR(ar);
                            waitRandom(4);
                            sentAny = true;
                        }
                    }
                }
                done = !sentAny;
                waitRandom(4);
            }
        });
        j.add_process(arThread);

        auto rThread = sc_spawn([&] {
            while (slaveInfo.r > 0) {
                logSlave(slaveIdx, fmt::format("Remaining R = {}", slaveInfo.r));
                logSlave(slaveIdx, "waiting for R");
                auto r = slave.receiveR();
                logSlave(slaveIdx, "received R", r);

                uint32_t threadIdx = r.id.to_uint() + (slaveIdx << threadShift);
                auto& threadInfo = threadInfos[threadIdx];

                EXPECT_(!threadInfo.rExpected.empty());
                EXPECT_EQ(threadInfo.rExpected.front(), r);
                threadInfo.rExpected.pop_front();

                waitRandom(4);
                slaveInfo.r -= 1;
            }
        });
        j.add_process(rThread);

        auto awThread = sc_spawn([&] {
            bool done = false;
            while (!done) {
                bool sentAny = false;
                for (uint32_t tid = 0; tid < threadsPerSlave; ++tid) {
                    uint32_t threadIdx = tid + (slaveIdx << threadShift);
                    auto& threadInfo = threadInfos[threadIdx];

                    uint32_t popNum = 1 + (rand() % 4);
                    for (uint32_t i = 0; i < popNum; ++i) {
                        if (!threadInfo.awTasks.empty()) {
                            auto aw = threadInfo.awTasks.front();
                            threadInfo.awTasks.pop_front();

                            auto awExpected = aw;
                            new (&awExpected.id) sc_bv<6>(threadIdx);
                            threadInfo.awExpected.push_back(awExpected);

                            EXPECT_(!threadInfo.wTasks.empty());
                            auto w = threadInfo.wTasks.front();
                            threadInfo.wTasks.pop_front();
                            threadInfo.wExpected.push_back(w);

                            sc_join inner;
                            inner.add_process(sc_spawn([&] {
                                logSlave(slaveIdx, "send AW", aw);
                                slave.sendAW(aw);
                            }));
                            inner.add_process(sc_spawn([&] {
                                for (auto const& w : w) {
                                    logSlave(slaveIdx, "send W", w);
                                    slave.sendW(w);
                                }
                            }));
                            inner.wait();

                            waitRandom(4);
                            sentAny = true;
                        }
                    }
                }
                done = !sentAny;
                waitRandom(4);
            }
        });
        j.add_process(awThread);

        auto bThread = sc_spawn([&] {
            while (slaveInfo.b > 0) {
                logSlave(slaveIdx, fmt::format("Remaining B = {}", slaveInfo.b));
                logSlave(slaveIdx, "waiting for B");
                auto b = slave.receiveB();
                logSlave(slaveIdx, "received B", b);

                uint32_t threadIdx = b.id.to_uint() + (slaveIdx << threadShift);
                auto& threadInfo = threadInfos[threadIdx];

                EXPECT_(!threadInfo.bExpected.empty());
                EXPECT_EQ(threadInfo.bExpected.front(), b);
                threadInfo.bExpected.pop_front();

                waitRandom(4);
                slaveInfo.b -= 1;
            }
        });
        j.add_process(bThread);

        j.wait();
        logSlave(slaveIdx, "Complete.");
    }
};

int sc_main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);

#if defined(VERILATED_TRACE_ENABLED)
    if (settings.trace)
        Verilated::traceEverOn(true);
#endif

    InterconnectTestbench testBench;

    sc_start(SC_ZERO_TIME);

    if (settings.trace) {
#if defined(VERILATED_TRACE_ENABLED)
        std::unique_ptr<VerilatedVcdSc> trace_file = std::make_unique<VerilatedVcdSc>();
        testBench.dut.traceVerilated(trace_file.get(), 99);
        trace_file->open("Interconnect.vcd");
#endif

        testBench.start();

#if defined(VERILATED_TRACE_ENABLED)
        trace_file->close();
#endif
    } else {
        testBench.start();
    }

    return 0;
}
