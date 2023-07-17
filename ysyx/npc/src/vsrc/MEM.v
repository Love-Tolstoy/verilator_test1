module MEM(
  input              clock,
  input              reset,
  input              en,
  input              rw,
  input       [63:0] addr,
  input       [3:0]  len,
  input       [63:0] dataIn,
  output      [63:0] dataOut,
  output         reg valid
);
import "DPI-C" function void vmem_read(
  input            en,
  input            rw,
  input  bit[63:0] addr,
  input  bit[3:0]  len,
  input  bit[63:0] dataIn,
  output bit[63:0] dataOut,
  input  bit       reset
);
initial begin
  valid = 1'b1;
end
always @(posedge clock) begin
  if (reset)
    valid <= 1'b1;

  if (valid == 1'b0 && en) begin
    vmem_read(en, rw, addr, len, dataIn, dataOut, reset);
    valid <= 1'b1;
  end
  else begin
    valid <= 1'b0;
  end

  // vmem_read(en, rw, addr, len, dataIn, dataOut, reset);
  // valid <= 1'b1;
end

endmodule