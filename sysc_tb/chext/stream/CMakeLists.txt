generate_tb(
    TARGET_NAME Read
    HDL_MODULES
        Read_Tbtop_DropEmpty
        Read_Tbtop_LastAlwaysInvalid
        Read_Tbtop_LastSometimesInvalid
)

generate_tb(
    TARGET_NAME Write
    HDL_MODULES
        Write_Tbtop_DropEmpty
        Write_Tbtop_KeepAll
)
