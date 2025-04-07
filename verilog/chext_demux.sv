module chext_demux_control #(
    parameter COUNT = 16,
    parameter DATA_WIDTH = 32,
    parameter SEL_WIDTH = 4
) (
    input [DATA_WIDTH-1:0] source_data,
    input source_last,
    input source_valid,
    output source_ready,

    output [COUNT * DATA_WIDTH-1:0] sink_data_n,
    output [COUNT-1:0] sink_valid_n,
    input [COUNT-1:0] sink_ready_n,

    input [SEL_WIDTH-1:0] select_bits,
    input select_valid,
    output select_ready
);

  wire valid = source_valid & select_valid;
  wire fire = valid & sink_ready_n[select_bits];

  assign source_ready = fire;
  assign select_ready = fire & source_last;
  
  generate
    for (genvar i = 0; i < COUNT; ++i) begin : valid_logic
        assign sink_valid_n[i] = valid & (i == select_bits);
    end

    for (genvar i = 0; i < COUNT; ++i) begin : data_logic
        assign sink_data_n[(i + 1) * DATA_WIDTH - 1:i * DATA_WIDTH] = source_data;
    end
  endgenerate
endmodule
