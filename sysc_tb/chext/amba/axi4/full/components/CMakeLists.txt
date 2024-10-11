cmake_minimum_required(VERSION 3.28)
project(tb)

add_compile_definitions(SC_DISABLE_API_VERSION_CHECK)

find_package(SystemCLanguage REQUIRED)
find_package(verilator REQUIRED)
find_package(fmt REQUIRED)
find_package(chext_test REQUIRED)
find_package(hdlscw REQUIRED)

add_executable(AddressGenerator.tb)
target_sources(AddressGenerator.tb PUBLIC src/AddressGenerator.tb.cpp)
target_include_directories(AddressGenerator.tb PUBLIC include/)
target_link_libraries(AddressGenerator.tb PUBLIC SystemC::systemc fmt::fmt chext_test::chext_test)

add_executable(AddressStrobeGenerator.tb)
target_sources(AddressStrobeGenerator.tb PUBLIC src/AddressStrobeGenerator.tb.cpp)
target_include_directories(AddressStrobeGenerator.tb PUBLIC include/)
target_link_libraries(AddressStrobeGenerator.tb PUBLIC SystemC::systemc fmt::fmt chext_test::chext_test)

verilate(
    AddressGenerator.tb
    SYSTEMC
    SOURCES hdl/AddressGenerator.sv
    VERILATOR_ARGS --trace
)

hdlscw(
    TARGET AddressGenerator.tb
    HDLINFO hdl/AddressGenerator.hdlinfo.json
    ARGS -r protocols.py
)

verilate(
    AddressStrobeGenerator.tb
    SYSTEMC
    SOURCES hdl/AddressStrobeGenerator.sv
    VERILATOR_ARGS --trace
)

hdlscw(
    TARGET AddressStrobeGenerator.tb
    HDLINFO hdl/AddressStrobeGenerator.hdlinfo.json
    ARGS -r protocols.py
)
