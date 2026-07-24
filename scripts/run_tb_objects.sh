#!/usr/bin/env bash
set -euo pipefail

# Runs SystemC HDL generators and repository test-scope applications.
# Usage:
#   ./scripts/run_tb_objects.sh
#   ./scripts/run_tb_objects.sh --dry-run
#   ./scripts/run_tb_objects.sh --log-file /tmp/tb.log

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DRY_RUN=0
LOG_FILE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --log-file)
      LOG_FILE="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

TEST_TARGETS=(
  "chext.elastic.Count_Tb"
  "chext.elastic.Fold_Tb"
  "chext.elastic.Transducer_Tb"
  "chext.elastic.Arbiter_Tb"
  "chext.elastic.BufferedNaming_Tb"
  "chext.elastic.BufferedNamingSeq_Tb"
  "chext.elastic.Null_Tb"
  "chext.elastic.Switch_Tb"
  "chext.stream.Read_Tb"
  "chext.stream.Write_Tb"
  "chext.float.Elastic_Tb"
  "chext.memory.Buffer_Tb"
  "chext.memory.BufferOp_Tb"
  "chext.memory.ConnectSeq_Tb"
  "chext.amba.axi4.full.components.AddressGenerator_Tb"
  "chext.amba.axi4.full.components.AddressStrobeGenerator_Tb"
  "chext.amba.axi4.full.components.Downscale_Tb"
  "chext.amba.axi4.full.components.DemuxMm_Tb"
  "chext.amba.axi4.full.components.Upscale_Tb"
  "chext.amba.axi4.full.components.Interconnect_Tb"
  "chext.amba.axi4.full.components.Unburst_Tb"
  "chext.amba.axi4.full.components.Widen_Tb"
  "chext.amba.axi4.full.components.IdParallelize_Tb"
  "chext.amba.axi4.full.components.IdSerialize_Tb"
)

COMPILE_TARGETS=()

APP_TARGETS=(
  "chext.amba.axi4.tracking.Properties_Test"
  "chext.amba.TrackingDiagnostics_Test"
  "chext.amba.axi4.ConstantSlave_Test"
  "chext.amba.axi4.full.Connect_Test"
  "chext.amba.axi4.lite.Connect_Test"
  "chext.amba.axi4.full.components.DemuxMm_Test"
  "chext.amba.axi4.full.components.LiteConverter_Test"
  "chext.elastic.ChildIoTracking_Test"
  "chext.elastic.GraphDeclarationOrder_Test"
)

sbt_args=()
for t in "${TEST_TARGETS[@]}"; do
  sbt_args+=("Test / runMain ${t}")
done
for t in "${COMPILE_TARGETS[@]}"; do
  sbt_args+=("Compile / runMain ${t}")
done
for t in "${APP_TARGETS[@]}"; do
  sbt_args+=("Test / runMain ${t}")
done

echo "Repository: $ROOT_DIR"
echo "Will run ${#TEST_TARGETS[@]} test-scope generators, ${#COMPILE_TARGETS[@]} compile-scope generators, and ${#APP_TARGETS[@]} test-scope apps."

if [[ $DRY_RUN -eq 1 ]]; then
  printf 'sbt'
  for arg in "${sbt_args[@]}"; do
    printf ' "%s"' "$arg"
  done
  printf '\n'
  exit 0
fi

if [[ -n "$LOG_FILE" ]]; then
  mkdir -p "$(dirname "$LOG_FILE")"
  sbt "${sbt_args[@]}" | tee "$LOG_FILE"
else
  sbt "${sbt_args[@]}"
fi

echo "Done. Generated SV artifacts are typically under sysc_tb/**/hdl/."
