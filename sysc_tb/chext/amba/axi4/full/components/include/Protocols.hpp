#ifndef PROTOCOLS_HPP_INCLUDED
#define PROTOCOLS_HPP_INCLUDED

#include <fmt/core.h>
#include <systemc>

namespace protocols {

namespace detail {

using namespace sc_core;
using namespace sc_dt;

struct AddrLenSizeBurst {
    sc_bv_base addr;
    uint8_t len;
    uint8_t size;
    uint8_t burst;
};

template<unsigned wAddr>
struct AddrLenSizeBurstSignals {
    using value_type = AddrLenSizeBurst;

    AddrLenSizeBurstSignals(const char* name)
        : addr(fmt::format("{}_addr", name).c_str())
        , len(fmt::format("{}_len", name).c_str())
        , size(fmt::format("{}_size", name).c_str())
        , burst(fmt::format("{}_burst", name).c_str()) {}

    sc_signal<sc_bv<wAddr>> addr;
    sc_signal<sc_bv<8>> len;
    sc_signal<sc_bv<3>> size;
    sc_signal<sc_bv<2>> burst;

    value_type read() {
        return {
            addr.read(),
            (uint8_t)len.read().to_uint(),
            (uint8_t)size.read().to_uint(),
            (uint8_t)burst.read().to_uint()
        };
    }

    void write(value_type const& x) {
        addr.write(x.addr);
        len.write(x.len);
        size.write(x.size);
        burst.write(x.burst);
    }
};

struct AddrSizeLast {
    sc_bv_base addr;
    uint8_t size;
    bool last;
};

template<unsigned wAddr>
struct AddrSizeLastSignals {
    using value_type = AddrSizeLast;

    AddrSizeLastSignals(const char* name)
        : addr(fmt::format("{}_addr", name).c_str())
        , size(fmt::format("{}_size", name).c_str())
        , last(fmt::format("{}_last", name).c_str()) {}

    sc_signal<sc_bv<wAddr>> addr;
    sc_signal<sc_bv<3>> size;
    sc_signal<bool> last;

    value_type read() {
        return {
            addr.read(),
            (uint8_t)size.read().to_uint(),
            last.read()
        };
    }

    void write(value_type const& x) {
        addr.write(x.addr);
        size.write(x.size);
        last.write(x.last);
    }
};

} // namespace detail

using detail::AddrLenSizeBurst;
using detail::AddrLenSizeBurstSignals;
using detail::AddrSizeLast;
using detail::AddrSizeLastSignals;

} // namespace protocols

#endif /* PROTOCOLS_HPP_INCLUDED */
