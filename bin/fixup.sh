#!/bin/bash
#
# ******************************************************************************
#     Completes the preparation of the deployable product
# ******************************************************************************


TARGET_PATH=./target
PRODUCT_PATH="${TARGET_PATH}/product"
MODS_PATH="${PRODUCT_PATH}/mods"
MAIN_JAR="${TARGET_PATH}/$1"
DOC_PATH=./doc
DOT_BIN="$2"


# Creating modules dependency overview documentation image...
#
# The existing .dot/.png are replaced only once jdeps has succeeded, so that a
# failure here cannot leave the checked-in documentation deleted.
#
# --multi-release is required: some dependencies (e.g. commons-text) ship as
# multi-release JARs, which jdeps refuses to read without it.
if [ -f "${MAIN_JAR}" ]; then

  SCRATCH="$(mktemp -d)"
  trap 'rm -rf "${SCRATCH}"' EXIT

  jdeps \
    --multi-release "$(jdeps --version | cut -d. -f1)" \
    --dot-output "${SCRATCH}" \
    --module-path "${MODS_PATH}" \
    -recursive -summary -filter:module \
    "${MAIN_JAR}"

  mv -v "${SCRATCH}/summary.dot" "${DOC_PATH}/module-dependencies.dot"

  if command -v "${DOT_BIN}" > /dev/null 2>&1; then
    "${DOT_BIN}" -T png -o "${DOC_PATH}/module-dependencies.png" "${DOC_PATH}/module-dependencies.dot"
  else
    echo "WARNING: '${DOT_BIN}' not found; skipping module-dependencies.png." >&2
  fi

fi

