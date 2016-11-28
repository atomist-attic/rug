#!/bin/bash

die(){
  echo $1
  exit 1
}

NODE_VERSION="v6.9.1"

if [ -z $1 ]; then
  die "First parameter must be the version number of the module to publish"
fi



if  [[ $TRAVIS == true ]]; then
   echo "Running on travis. Downloading node..."
   wget "https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz"
   tar -xJf node-${NODE_VERSION}-linux-x64.tar.xz || die "Unable to extract archive"
   PATH="${PATH}:./node-${NODE_VERSION}-linux-x64/bin"
fi

command -v npm >/dev/null 2>&1 || die "npm not found on path!"

TARGET="target/nodejs"
mkdir -p "${TARGET}"

cp -a src/test/resources/user-model "${TARGET}" || die "Error copying user-model to target dir"
jq --arg version "$1" '.version = $version' src/test/resources/user-model/package.json > "${TARGET}"/user-model/package.json

cd "${TARGET}"

npm install typescript || die "Error installing typescript module"
./node_modules/typescript/bin/tsc -p user-model/ || die "Error building typescript project"


cd user-model

if [[ -z ${NPMJS_API_KEY} ]]; then
   echo "Assuming your ~/.npmrc is setup correctly for this project"
else
   echo "Creating local .npmrc using API key from environment"
   echo "registry.npmjs.org/:_authToken=${NPMJS_API_KEY}" > .npmrc
fi

# npm honors this

rm -f .gitignore
npm publish --access=public || die "Error publishing node module"

