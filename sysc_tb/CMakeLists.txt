cmake_minimum_required(VERSION 3.28)
project(sysc_tb)

add_compile_definitions(SC_DISABLE_API_VERSION_CHECK)
add_compile_definitions(SC_ALLOW_DEPRECATED_IEEE_API)

find_package(SystemCLanguage REQUIRED)
find_package(verilator REQUIRED)
find_package(fmt REQUIRED)
find_package(hdlstuff_hal REQUIRED)
find_package(chext_test REQUIRED)
find_package(boost_endian REQUIRED)
find_package(hdlscw REQUIRED)

set(CHEXT_HDL_DIR "${CMAKE_SOURCE_DIR}/_common/verilog")

if(NOT EXISTS "${CHEXT_TEST_HDL_DIR}/chext_deadlock_monitor.sv")
    message(
        FATAL_ERROR
        "chext_test did not provide chext_deadlock_monitor.sv via CHEXT_TEST_HDL_DIR"
    )
endif()

option(SYSC_TB_ENABLE_VERILATOR_TRACE "Enable Verilator VCD tracing for SystemC testbenches" OFF)

include(CMakeParseArguments)

file(
    GLOB SYSC_TB_COMMON_SOURCES
    "${CMAKE_SOURCE_DIR}/_common/src/*.c"
    "${CMAKE_SOURCE_DIR}/_common/src/*.cc"
    "${CMAKE_SOURCE_DIR}/_common/src/*.cpp"
)

function(get_current_ns OUTPUT_VAR)
    file(RELATIVE_PATH REL_PATH "${CMAKE_SOURCE_DIR}" "${CMAKE_CURRENT_SOURCE_DIR}")
    string(REPLACE "/" "." NAMESPACE "${REL_PATH}")
    set(${OUTPUT_VAR} "${NAMESPACE}" PARENT_SCOPE)
endfunction()

function(generate_tb)
    set(options "")
    set(oneValueArgs TARGET_NAME)
    set(multiValueArgs CPP_SOURCES HDL_MODULES)
    cmake_parse_arguments(GEN_TB "${options}" "${oneValueArgs}" "${multiValueArgs}" ${ARGN})

    if(NOT GEN_TB_CPP_SOURCES)
        set(GEN_TB_CPP_SOURCES ${GEN_TB_TARGET_NAME}.tb.cpp)
    endif()

    get_current_ns(GEN_TB_NS)
    set(GEN_TB_OUTPUT_NAME ${GEN_TB_TARGET_NAME}.tb)
    set(GEN_TB_TARGET_NAME ${GEN_TB_NS}.${GEN_TB_TARGET_NAME}.tb)

    add_executable(${GEN_TB_TARGET_NAME})
    set_target_properties(${GEN_TB_TARGET_NAME} PROPERTIES OUTPUT_NAME ${GEN_TB_OUTPUT_NAME})

    if(SYSC_TB_ENABLE_VERILATOR_TRACE)
        target_compile_definitions(${GEN_TB_TARGET_NAME} PUBLIC VERILATED_TRACE_ENABLED)
        set(GEN_TB_VERILATOR_TRACE_ARGS --trace)
    else()
        set(GEN_TB_VERILATOR_TRACE_ARGS "")
    endif()

    foreach(SOURCE_FILE IN LISTS GEN_TB_CPP_SOURCES)
        target_sources(
            ${GEN_TB_TARGET_NAME}
            PUBLIC
            ${CMAKE_CURRENT_SOURCE_DIR}/src/${SOURCE_FILE}
        )
    endforeach()

    target_include_directories(
        ${GEN_TB_TARGET_NAME}
        PUBLIC
        include/
        ${CMAKE_SOURCE_DIR}/_common/include
    )
    target_sources(${GEN_TB_TARGET_NAME} PUBLIC ${SYSC_TB_COMMON_SOURCES})

    target_link_libraries(
        ${GEN_TB_TARGET_NAME}
        PUBLIC
        SystemC::systemc
        fmt::fmt
        chext::chext_test
        Boost::endian
    )

    foreach(MODULE IN LISTS GEN_TB_HDL_MODULES)
        verilate(
            ${GEN_TB_TARGET_NAME}
            SYSTEMC
            SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/hdl/${MODULE}.sv
            VERILATOR_ARGS
                ${GEN_TB_VERILATOR_TRACE_ARGS}
                -I${CHEXT_HDL_DIR}
                -I${CHEXT_TEST_HDL_DIR}
                -Wno-fatal
        )

        hdlscw(
            TARGET ${GEN_TB_TARGET_NAME}
            HDLINFO ${CMAKE_CURRENT_SOURCE_DIR}/hdl/${MODULE}.hdlinfo.json
            ARGS -r ${CMAKE_CURRENT_SOURCE_DIR}/protocols.py
        )
    endforeach()
endfunction()

add_subdirectory(chext/elastic/)
add_subdirectory(chext/amba/axi4/full/components/)
add_subdirectory(chext/amba/axi4/lite/components/)
# add_subdirectory(chext/memory/)
add_subdirectory(chext/stream/)
# add_subdirectory(chext/ldstr/)
