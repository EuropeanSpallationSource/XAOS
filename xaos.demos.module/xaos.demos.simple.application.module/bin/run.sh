#!/bin/bash
#
# ******************************************************************************
#     Runs the deployable product.
#
#     Must be executed from inside the product folder (the one containing the
#     "mods" and "libs" sub-folders), where fixup.sh copies this script.
# ******************************************************************************

set -euo pipefail

LIBS_PATH=./libs
MODS_PATH=./mods
MAIN_MODULE=xaos.demos.simple.application
MAIN_CLASS=eu.ess.xaos.demos.simple.SimpleApplication

# Service providers (icons, log handlers, ...) are bound automatically by the
# module system, so no explicit --add-modules is needed here.
java \
  -cp "${LIBS_PATH}" \
  -p "${MODS_PATH}" \
  --enable-native-access=javafx.graphics \
  --module ${MAIN_MODULE}/${MAIN_CLASS} \
  "$@"
