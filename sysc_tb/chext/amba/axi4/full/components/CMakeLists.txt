generate_tb(
    TARGET_NAME AddressGenerator
    HDL_MODULES AddressGeneratorTestTop1_1
)

generate_tb(
    TARGET_NAME AddressStrobeGenerator
    HDL_MODULES AddressStrobeGeneratorTestTop1_1
)

generate_tb(
    TARGET_NAME Downscale
    HDL_MODULES DownscaleTestTop1_1 DownscaleTestTop1_2
)

generate_tb(
    TARGET_NAME Upscale
    HDL_MODULES UpscaleTestTop1_1 UpscaleTestTop1_2
)

generate_tb(
    TARGET_NAME IdSerialize
    HDL_MODULES IdSerializeTestTop1_1
)

generate_tb(
    TARGET_NAME Unburst
    HDL_MODULES UnburstTestTop1_1
)

generate_tb(
    TARGET_NAME Transaction
    HDL_MODULES
        UpscaleTestTop1_1
        DownscaleTestTop1_1
)
