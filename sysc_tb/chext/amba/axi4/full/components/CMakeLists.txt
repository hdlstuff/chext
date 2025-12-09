generate_tb(
    TARGET_NAME AddressGenerator
    HDL_MODULES AddressGenerator_Tbtop_1
)

generate_tb(
    TARGET_NAME AddressStrobeGenerator
    HDL_MODULES AddressStrobeGenerator_Tbtop_1
)

generate_tb(
    TARGET_NAME Downscale
    HDL_MODULES Downscale_Tbtop_1 Downscale_Tbtop_2
)

generate_tb(
    TARGET_NAME Upscale
    HDL_MODULES
        UpscaleTestTop1_1
        UpscaleTestTop1_2
)

generate_tb(
    TARGET_NAME IdSerialize
    HDL_MODULES IdSerializeTestTop1_1
)

generate_tb(
    TARGET_NAME IdParallelize
    HDL_MODULES
        IdParallelizeTestTop2_1
        IdParallelizeTestTop2_2
        IdParallelizeTestTop2_3
        IdParallelizeTestTop2_4
        IdParallelizeTestTop2_5
        IdParallelizeTestTop2_6
)

generate_tb(
    TARGET_NAME Unburst
    HDL_MODULES UnburstTestTop1_1
)

generate_tb(
    TARGET_NAME Interconnect
    HDL_MODULES Interconnect_Tbtop_1
)

generate_tb(
    TARGET_NAME Widen
    HDL_MODULES Widen_Tbtop
)
