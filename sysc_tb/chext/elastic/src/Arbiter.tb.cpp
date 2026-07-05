#include <Arbiter_Tbtop.hpp>

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

        for (uint32_t i = 0; i < N; ++i) {
            rr_sources[i] = (&dut.rr_sources_00) + i;
        }

        rr_sink = &dut.rr_sink;
        rr_select = &dut.rr_select;

        for (uint32_t i = 0; i < N; ++i) {
            priority_sources[i] = (&dut.priority_sources_00) + i;
        }

        priority_sink = &dut.priority_sink;
        priority_select = &dut.priority_select;

        SC_THREAD(thread0);
    }

    Arbiter_Tbtop dut;

private:
    sc_clock clock;
    sc_signal<bool> reset;

    using SourceT = decltype(Arbiter_Tbtop::rr_sources_00);
    using SinkT = decltype(Arbiter_Tbtop::rr_sink);
    using SelectT = decltype(Arbiter_Tbtop::rr_select);

    static constexpr uint32_t N = 16;
    static constexpr uint32_t numPackets = 128;

    std::array<SourceT*, N> rr_sources;
    SinkT* rr_sink;
    SelectT* rr_select;

    std::array<SourceT*, N> priority_sources;
    SinkT* priority_sink;
    SelectT* priority_select;

    void waitRandom() {
        static uint32_t delays[] = { 4, 32, 1024, 4096 };
        uint32_t delay = delays[rand() % 4];

        wait(delay * 2.0, SC_NS);
    }

    void testRr0() {
        sc_join j;
        std::vector<std::vector<int>> check(N, std::vector<int>(numPackets, 0));

        for (uint32_t i = 0; i < N; ++i) {
            auto handle = sc_spawn([this, i] {
                for (uint32_t j = 0; j < numPackets; ++j) {
                    rr_sources[i]->send((i << 16) + j);
                }
            });

            j.add_process(handle);
        }

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = rr_sink->receive();
                auto source = received >> 16;
                auto index = received & 0xffff;

                fmt::println("[{:^06}] received @[{:^10}], from = {}, index = {}", j, sc_time_stamp().to_string(), source, index);

                check[source][index]++;
            }
        };

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = rr_select->receive();

                fmt::println("[{:^06}] received @[{:^10}], select = {}", j, sc_time_stamp().to_string(), received);
            }
        };

        j.wait();

        bool checkFailed = false;
        for (uint32_t i = 0; i < N; ++i)
            for (uint32_t j = 0; j < numPackets; ++j) {
                if (check[i][j] != 1) {
                    fmt::println("check failed for i = {}, j = {}", i, j);
                    checkFailed = true;
                }
            }

        if (checkFailed)
            fmt::println("failed!!!");
        else
            fmt::println("success!!!");
    }

    void testRr1() {
        sc_join j;
        std::vector<std::vector<int>> check(N, std::vector<int>(numPackets, 0));

        for (uint32_t i = 0; i < N; ++i) {
            auto handle = sc_spawn([this, i] {
                for (uint32_t j = 0; j < numPackets; ++j) {
                    rr_sources[i]->send((i << 16) + j);
                    waitRandom();
                }
            });

            j.add_process(handle);
        }

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = rr_sink->receive();
                auto source = received >> 16;
                auto index = received & 0xffff;

                fmt::println("[{:^06}] received @[{:^10}], from = {}, index = {}", j, sc_time_stamp().to_string(), source, index);

                check[source][index]++;

                waitRandom();
            }
        };

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = rr_select->receive();

                fmt::println("[{:^06}] received @[{:^10}], select = {}", j, sc_time_stamp().to_string(), received);
            }
        };

        j.wait();

        bool checkFailed = false;
        for (uint32_t i = 0; i < N; ++i)
            for (uint32_t j = 0; j < numPackets; ++j) {
                if (check[i][j] != 1) {
                    fmt::println("check failed for i = {}, j = {}", i, j);
                    checkFailed = true;
                }
            }

        if (checkFailed)
            fmt::println("failed!!!");
        else
            fmt::println("success!!!");
    }

    void testPriority0() {
        sc_join j;
        std::vector<std::vector<int>> check(N, std::vector<int>(numPackets, 0));

        for (uint32_t i = 0; i < N; ++i) {
            auto handle = sc_spawn([this, i] {
                for (uint32_t j = 0; j < numPackets; ++j) {
                    priority_sources[i]->send((i << 16) + j);
                }
            });

            j.add_process(handle);
        }

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = priority_sink->receive();
                auto source = received >> 16;
                auto index = received & 0xffff;

                fmt::println("[{:^06}] received @[{:^10}], from = {}, index = {}", j, sc_time_stamp().to_string(), source, index);

                check[source][index]++;
            }
        };

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = priority_select->receive();

                fmt::println("[{:^06}] received @[{:^10}], select = {}", j, sc_time_stamp().to_string(), received);
            }
        };

        j.wait();

        bool checkFailed = false;
        for (uint32_t i = 0; i < N; ++i)
            for (uint32_t j = 0; j < numPackets; ++j) {
                if (check[i][j] != 1) {
                    fmt::println("check failed for i = {}, j = {}", i, j);
                    checkFailed = true;
                }
            }

        if (checkFailed)
            fmt::println("failed!!!");
        else
            fmt::println("success!!!");
    }

    void testPriority1() {
        sc_join j;
        std::vector<std::vector<int>> check(N, std::vector<int>(numPackets, 0));

        for (uint32_t i = 0; i < N; ++i) {
            auto handle = sc_spawn([this, i] {
                for (uint32_t j = 0; j < numPackets; ++j) {
                    priority_sources[i]->send((i << 16) + j);
                    waitRandom();
                }
            });

            j.add_process(handle);
        }

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = priority_sink->receive();
                auto source = received >> 16;
                auto index = received & 0xffff;

                fmt::println("[{:^06}] received @[{:^10}], from = {}, index = {}", j, sc_time_stamp().to_string(), source, index);

                check[source][index]++;

                waitRandom();
            }
        };

        SC_SPAWN_TO(j) {
            for (uint32_t j = 0; j < (N * numPackets); ++j) {
                auto received = priority_select->receive();

                fmt::println("[{:^06}] received @[{:^10}], select = {}", j, sc_time_stamp().to_string(), received);
            }
        };

        j.wait();

        bool checkFailed = false;
        for (uint32_t i = 0; i < N; ++i)
            for (uint32_t j = 0; j < numPackets; ++j) {
                if (check[i][j] != 1) {
                    fmt::println("check failed for i = {}, j = {}", i, j);
                    checkFailed = true;
                }
            }

        if (checkFailed)
            fmt::println("failed!!!");
        else
            fmt::println("success!!!");
    }

    void thread0() {
        reset.write(1);
        wait(10, SC_NS);
        reset.write(0);

        testRr0();
        testRr1();

        testPriority0();
        testPriority1();

        fmt::println("done.");
        sc_stop();
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
    trace_file->open("Arbiter.tb.vcd");
#endif

    sc_start(sc_time(120000000, SC_NS));
#if defined(VERILATED_TRACE_ENABLED)
    trace_file->close();
#endif

    return 0;
}
