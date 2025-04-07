module chext_demux_control #(
    parameter COUNT = 16,
    parameter SEL_WIDTH = 4
) (
    input source_last,
    input source_valid,
    output source_ready,

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

    for (int i = 0; i < COUNT; ++i) begin : valid_logic
        assign sink_valid_n[i] = valid & (i == select_bits);
    end

  endgenerate

endmodule
