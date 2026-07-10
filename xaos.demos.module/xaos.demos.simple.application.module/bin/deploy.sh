#!/bin/bash
#
# ******************************************************************************
#     Builds a self-contained application image of the deployable product.
#
#     Must be executed from the module folder, after "mvn package" and fixup.sh
#     have populated target/product/mods.
#
#     Usage: bin/deploy.sh [app-version]
#
#     The application version must be one to three dot-separated integers, whose
#     first component is greater than zero: jpackage rejects both qualifiers
#     such as "-SNAPSHOT" and leading zeroes. It is therefore independent of the
#     Maven project version.
# ******************************************************************************

set -euo pipefail

PRODUCT_PATH=./target/product
MODS_PATH="${PRODUCT_PATH}/mods"
OUTPUT="${PRODUCT_PATH}/installer"
APPLICATION_NAME="Simple Application"
APPLICATION_VERSION="${1:-1.0}"
MAIN_MODULE=xaos.demos.simple.application
MAIN_CLASS=eu.ess.xaos.demos.simple.SimpleApplication

if [ ! -d "${MODS_PATH}" ]; then
  echo "ERROR: ${MODS_PATH} not found. Run 'mvn package' and bin/fixup.sh first." >&2
  exit 1
fi

# Supply a ready-made runtime so that jpackage does not invoke jlink: RxJava
# pulls in reactive-streams, which is an automatic module, and jlink refuses to
# link those.
RUNTIME_IMAGE="${JAVA_HOME:-$(/usr/libexec/java_home 2> /dev/null || true)}"

if [ -z "${RUNTIME_IMAGE}" ] || [ ! -d "${RUNTIME_IMAGE}" ]; then
  echo "ERROR: cannot locate a JDK. Set JAVA_HOME and retry." >&2
  exit 1
fi

rm -rf "${OUTPUT}"
mkdir -p "${OUTPUT}"

# JavaFX ships its native libraries inside the platform-specific JARs already
# present in "mods", so no separate javafx-jmods path is required.
#
# Use "--type app-image" for a runnable image; replace it with "pkg" (macOS),
# "deb"/"rpm" (Linux) or "msi" (Windows) to build an installer instead.
jpackage \
  --type app-image \
  --module-path "${MODS_PATH}" \
  --module ${MAIN_MODULE}/${MAIN_CLASS} \
  --runtime-image "${RUNTIME_IMAGE}" \
  --dest "${OUTPUT}" \
  --name "${APPLICATION_NAME}" \
  --app-version "${APPLICATION_VERSION}" \
  --java-options --enable-native-access=javafx.graphics
