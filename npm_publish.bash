#!/bin/bash

function die() {
    echo "$1"
    echo "npm_publish: $*"
    exit 1
}

if [[ ! $1 ]]; then
  die "First parameter must be the version number of the module to publish"
fi

npm="target/node/node_modules/npm/bin/npm"

TARGET="target/.atomist/node_modules/@atomist/rug"

PACKAGE_FILE="${TARGET}/package.json"

cp "${PACKAGE_FILE}" "${PACKAGE_FILE}.old"
jq --arg version "$1" '.version = $version' "${PACKAGE_FILE}.old" > "${PACKAGE_FILE}"

cd "${TARGET}" ||

if [[ -z ${NPM_TOKEN} ]]; then
    echo "Assuming your .npmrc is setup correctly for this project"
else
    echo "Creating local .npmrc using API key from environment"
    ( touch .npmrc && chmod 600 .npmrc ) || die "failed to create secure .npmrc"
    echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > .npmrc
fi

if ! $npm publish --access=public; then
    cat npm-debug.log
    die "Error publishing node module"
fi
