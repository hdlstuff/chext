from chext_test import ElasticProtocol
import hdlinfo


def registerAddrLenSizeBurstBundle() -> None:
    def signalName(interface: hdlinfo.Interface) -> str:
        wAddr = interface.args["wAddr"]
        return f"protocols::AddrLenSizeBurstSignals<{wAddr}>"

    portsToSignals = [
        ("bits_addr", "bits.addr"),
        ("bits_len", "bits.len"),
        ("bits_size", "bits.size"),
        ("bits_burst", "bits.burst"),
        ("ready", "ready"),
        ("valid", "valid")
    ]

    ElasticProtocol(
        "chext.amba.axi4.full.components.addrgen.AddrLenSizeBurstBundle",
        includeStr='"Protocols.hpp"',
        bitsSignalType=signalName,
        portsToSignals=portsToSignals
    )


registerAddrLenSizeBurstBundle()


def registerAddrSizeLastBundle() -> None:
    def signalName(interface: hdlinfo.Interface) -> str:
        wAddr = interface.args["wAddr"]
        return f"protocols::AddrSizeLastSignals<{wAddr}>"

    portsToSignals = [
        ("bits_addr", "bits.addr"),
        ("bits_size", "bits.size"),
        ("bits_last", "bits.last"),
        ("ready", "ready"),
        ("valid", "valid")
    ]

    ElasticProtocol(
        "chext.amba.axi4.full.components.addrgen.AddrSizeLastBundle",
        includeStr='"Protocols.hpp"',
        bitsSignalType=signalName,
        portsToSignals=portsToSignals
    )


registerAddrSizeLastBundle()
