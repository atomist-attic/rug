#!/bin/bash

set -o pipefail

declare Pkg=npm-publish
declare Version=0.3.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

# publish node module, default MODULE=rug
# usage: publish MODULE_VERSION [MODULE]
function publish() {
    local module_version=$1
    if [[ ! $module_version ]]; then
        err "publish: missing required parameter: MODULE_VERSION"
        return 10
    fi
    shift
    local module_name=$1
    if [[ ! $module_name ]]; then
        module_name=rug
    else
        shift
    fi

    local target=target/.atomist/node_modules/@atomist/$module_name
    local package=$target/package.json
    local package_version
    package_version=$(jq --raw-output --exit-status .version "$package")
    if [[ $? -ne 0 || ! $package_version ]]; then
        err "failed to parse package version from $package: $package_version"
        return 1
    fi
    if [[ $package_version != $module_version ]]; then
        err "version in $package ($package_version) does not match provided module version ($module_version)"
        return 1
    fi

    local registry
    if [[ $module_version =~ ^[0-9]+\.[0-9]+\.[0-9]+-[0-9]{14}$ ]]; then
        msg "publishing snapshot version $module_version"
        if [[ $ATOMIST_REPO_TOKEN && $ATOMIST_REPO_USER ]]; then
            msg "creating local .npmrc using auth details pulled from Artifactory"
            if ! ( umask 077 && curl -s -u"$ATOMIST_REPO_USER:$ATOMIST_REPO_TOKEN" https://atomist.jfrog.io/atomist/api/npm/auth > "$HOME/.npmrc" 2>/dev/null )
            then
                err "failed to create $HOME/.npmrc"
                return 1
            fi
        else
            msg "assuming your .npmrc is setup correctly to publish snapshots"
        fi
        registry=--registry=https://atomist.jfrog.io/atomist/api/npm/npm-dev-local
    elif [[ $module_version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-(m|rc)\.[0-9]+)?$ ]]; then
        msg "publishing release version $module_version"
        if [[ $NPM_TOKEN ]]; then
            msg "creating local .npmrc using NPM token from environment"
            if ! ( umask 077 && echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > "$HOME/.npmrc" ); then
                err "failed to create $HOME/.npmrc"
                return 1
            fi
        else
            msg "assuming your .npmrc is setup correctly to publish to npmjs.org"
        fi
    else
        err "not publishing invalid version: $module_version"
        return 1
    fi

    # npm honors this
    rm -f "$target/.gitignore"

    if ! ( cd "$target" && npm publish --access=public $registry ); then
        err "failed to publish node module"
        cat "$target/npm-debug.log"
        return 1
    fi
}

function main() {
    publish "$@" || return 1
}

main "$@" || exit 1
exit 0
