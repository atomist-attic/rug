#!/bin/bash

set -o pipefail

declare Pkg=npm-publish
declare Version=0.1.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

function publish() {
    local module_version=$1
    local mmodule_name=$2
    if [[ ! $module_version ]]; then
        err "first parameter must be the version number of the module to publish"
        return 10
    fi

    if [[ ! $module_name ]]; then
        err "second parameter must be the name of the module to publish"
        return 10
    fi

    local target="target/.atomist/node_modules/@atomist/${module_name}}"
    local package="$target/package.json"
    if ! sed "/version/s/REPLACE_ME/$module_version/" "$package.in" > "$package"; then
        err "failed to set version in $package"
        return 1
    fi
    rm -f "$package.in"

    if [[ $NPM_TOKEN ]]; then
        msg "Creating local .npmrc using API key from environment"
        if ! ( umask 077 && echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > "$HOME/.npmrc" ); then
            err "failed to create $HOME/.npmrc"
            return 1
        fi
    else
        msg "assuming your .npmrc is setup correctly for this project"
    fi

    # npm honors this
    rm -f "$target/.gitignore"

    if ! ( cd "$target" && npm publish --access=public ); then
        err "failed to publish node module"
        cat "$target/npm-debug.log"
        return 1
    fi
}

main "$@" || exit 1
exit 0
