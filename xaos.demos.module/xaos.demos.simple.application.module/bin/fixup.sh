#!/bin/bash
#
# ******************************************************************************
#     Completes the preparation of the deployable product
# ******************************************************************************


BIN_PATH=./bin
TARGET_PATH=./target
PRODUCT_PATH="${TARGET_PATH}/product"
MODS_PATH="${PRODUCT_PATH}/mods"
LIBS_PATH="${PRODUCT_PATH}/libs"
MAIN_JAR="${TARGET_PATH}/$1"
DOC_PATH=./doc


# Test dependencies are already kept out of "mods" by the copy-dependencies
# "includeScope" setting in the parent POM.


# Remove the empty, platform-less OpenJFX marker JARs. They carry no classes,
# so the module system reads them as automatic modules whose names clash with
# the real platform-specific ones (e.g. javafx-graphics-<v>-mac-aarch64.jar).
for jar in "${MODS_PATH}"/javafx-*.jar "${MODS_PATH}"/jdk-jsobject-*.jar; do
  [ -e "${jar}" ] || continue
  if [ "$(unzip -l "${jar}" | grep -c '\.class$')" -eq 0 ]; then
    rm -v "${jar}"
  fi
done


# Move non-module JARs into the class path...
if [ ! -d "${LIBS_PATH}" ]; then
  mkdir -pv "${LIBS_PATH}"
fi

#mv -v "${MODS_PATH}/xxx.jat" "${LIBS_PATH}/"


# Copy product main JAR in module path...
cp -v "${MAIN_JAR}" "${MODS_PATH}/"


# Copy run script in the product folder.
cp -v "${BIN_PATH}/run.sh" "${PRODUCT_PATH}/"


# Super call to main fixup.sh
../../bin/fixup.sh $1 $2

