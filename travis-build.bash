#!/bin/bash

set -eu
set -o pipefail
ulimit -c unlimited

#export MAVEN_OPTS="-Xmx1g"
#export JAVA_OPTS="-Xmx1g"
#export RUG_DISABLE_V8="true"

mvn="mvn --settings .settings.xml -B -V"
if [[ $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    $mvn build-helper:parse-version versions:set -DnewVersion="$TRAVIS_TAG" versions:commit
    project_version="$TRAVIS_TAG"
else
    $mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-\${timestamp} versions:commit
    project_version=$(mvn help:evaluate -Dexpression=project.version | grep -v "^\[")
fi
$mvn install -Dmaven.javadoc.skip=true

echo "Branch is ${TRAVIS_BRANCH}"

[[ $TRAVIS_PULL_REQUEST == false ]] || exit 0
[[ $project_version ]] || exit 0
if [[ $TRAVIS_BRANCH == master || $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Version is $project_version"
    $mvn deploy -DskipTests
    if [[ $TRAVIS_TAG ]]; then
       ./npm_publish.sh "${TRAVIS_TAG}"
    fi

    git config --global user.email "travis-ci@atomist.com"
    git config --global user.name "Travis CI"
    git_tag="$project_version+travis$TRAVIS_BUILD_NUMBER"
    git tag "$git_tag" -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"
    git push origin "$git_tag"
fi

exit 0
