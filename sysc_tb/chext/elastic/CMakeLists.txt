# generate_tb(
#     TARGET_NAME Queue
#     HDL_MODULES QueueTestTop1
# )

# generate_tb(
#     TARGET_NAME Reduce
#     HDL_MODULES ReduceTestTop1
# )

# generate_tb(
#     TARGET_NAME Switch
#     HDL_MODULES Switch_Tbtop1
# )

# generate_tb(
#     TARGET_NAME ReduceTest1
#     HDL_MODULES ReduceTest1_Tbtop
# )

generate_tb(
    TARGET_NAME Fold
    HDL_MODULES Fold_Tbtop
)

generate_tb(
    TARGET_NAME Transducer
    HDL_MODULES Transducer_Tbtop
)

generate_tb(
    TARGET_NAME Arbiter
    HDL_MODULES Arbiter_Tbtop
)
