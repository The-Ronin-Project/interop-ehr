#!/bin/bash

TEMP_DIR=temp-profile-ig
SRC_PATH=${TEMP_DIR}/docs/package.tgz
DST_PATH=src/main/resources/package.tgz

function cleanup {
  if [ -d "${TEMP_DIR}" ]; then
    echo "Removing cloned dp-ronin-fhir-profile-ig"
    rm -rf temp-profile-ig
  fi
}

trap cleanup EXIT

echo "Cloning dp-ronin-fhir-profile-ig"
git clone git@github.com:projectronin/dp-ronin-fhir-profile-ig.git ${TEMP_DIR}

diff ${SRC_PATH} ${DST_PATH}

if [ "$?" -eq "0" ]; then
  echo "package.tgz is up-to-date"
else
  echo "package.tgz is outdated. Updating it."
  cp ${SRC_PATH} ${DST_PATH}
fi
