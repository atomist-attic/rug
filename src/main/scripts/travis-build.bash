#!/bin/bash
# build, test, and publish maven projects on Travis CI

set -o pipefail

declare Pkg=travis-build-mvn
declare Version=0.1.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

function main() {
    msg "branch is ${TRAVIS_BRANCH}"

    local mvn="mvn --settings .settings.xml -B -V -P npm-release"
    local project_version
    if [[ $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        if ! $mvn build-helper:parse-version versions:set -DnewVersion="$TRAVIS_TAG" versions:commit; then
            err "failed to set project version"
            return 1
        fi
        project_version="$TRAVIS_TAG"
    else
        if ! $mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-\${timestamp} versions:commit
        then
            err "failed to set timestamped project version"
            return 1
        fi
        project_version=$(mvn help:evaluate -Dexpression=project.version | grep -v "^\[")
        if [[ $? != 0 || ! $project_version ]]; then
            err "failed to parse project version"
            return 1
        fi
    fi

    if ! $mvn scoverage:report -Dmaven.javadoc.skip=true; then
        err "tests failed"
        return 1
    fi

    if ! $mvn install -DskipTests -Dmaven.javadoc.skip=true; then
        err "maven install failed"
        return 1
    fi

    if [[ $TRAVIS_PULL_REQUEST != false ]]; then
        msg "not publishing or tagging pull request"
        return 0
    fi

    if [[ $TRAVIS_BRANCH == master || $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        msg "version is $project_version"
        local mvn_deploy_args
        if [[ $TRAVIS_BRANCH == master ]]; then
            mvn_deploy_args=-DaltDeploymentRepository=public-atomist-dev::default::https://atomist.jfrog.io/atomist/libs-dev-local
        fi

        if ! $mvn deploy site -DskipTests $mvn_deploy_args; then
            err "maven deploy failed"
            return 1
        fi

        if ! git config --global user.email "travis-ci@atomist.com"; then
            err "failed to set git user email"
            return 1
        fi
        if ! git config --global user.name "Travis CI"; then
            err "failed to set git user name"
            return 1
        fi
        local git_tag=$project_version+travis$TRAVIS_BUILD_NUMBER
        if ! git tag "$git_tag" -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"; then
            err "failed to create git tag: $git_tag"
            return 1
        fi
        if ! git push --quiet --tags "https://$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG" > /dev/null 2>&1; then
            err "failed to push git tags"
            return 1
        fi
    fi
}

main "$@" || exit 1
exit 0
