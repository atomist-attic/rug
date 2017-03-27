#!/bin/bash
# Push the HTML docs to the appropriate public repository
# serving our github pages

set -o pipefail
declare Pkg=gh-pages-deploy
declare Version=0.2.0

# print message to stdout
# usage: msg MESSAGE
function msg() {
    echo "$Pkg: $*"
}

# print message to stderr
# usage: err MESSAGE
function err() {
    msg "$*" 1>&2
}

# push site to github pages branch, default branch=gh-pages
# usage: main [REPO_SLUG [BRANCH]]
function main() {
    local repo_slug=$1
    local branch=$2

    if [[ $TRAVIS_PULL_REQUEST && $TRAVIS_PULL_REQUEST != false ]]; then
        return 0
    fi

    local repository
    if [[ $repo_slug ]]; then
        if [[ ! $GITHUB_TOKEN ]]; then
            err "repo slug given but GITHUB_TOKEN environment variable is not set"
            return 1
        fi
        repository=https://$GITHUB_TOKEN@github.com/$repo_slug.git
    else
        # need origin URL since we later delete .git
        repository=$(git remote get-url origin)
        if [[ $? -ne 0 || ! $repository ]]; then
            err "failed to get URL for origin"
            return 1
        fi
        repo_slug=origin
    fi

    if [[ ! $branch ]]; then
        branch=gh-pages
    fi
    local refspec=master:$branch

    if ! mkdir -p target/site/typedoc; then
        err "failed to create typdoc directory"
        return 1
    fi

    for module in rug cortex; do
        if ! mv "target/.atomist/node_modules/@atomist/$module/typedoc" "target/site/typedoc/$module"; then
            err "failed to move $module TypeDoc to site directory"
            return 1
        fi
    done

    if ! cd target/site; then
        err "failed to change to site directory"
        return 1
    fi
    rm -rf .git

    if ! git init; then
        err "failed to initialize git"
        return 1
    fi

    local commit_msg="Local site push"
    if [[ $TRAVIS == true ]]; then
        if ! git config user.email "travis-ci@atomist.com"; then
            err "failed to set git user email"
            return 1
        fi
        if ! git config user.name "Travis CI"; then
            err "failed to set git user name"
            return 1
        fi

        commit_msg="Generated from ${TRAVIS_REPO_SLUG} ${TRAVIS_COMMIT}"
    fi

    if ! echo apidocs.atomist.com > CNAME; then
        err "failed to create CNAME"
        return 1
    fi

    if ! touch .nojekyll; then
        err "failed to create nojekyll file, continuing"
    fi

    if ! git add .; then
        err "failed to add files for commit"
        return 1
    fi

    if ! git commit -m "$commit_msg"; then
        err "failed to commit site files"
        return 1
    fi

    if ! git push --force --quiet "$repository" "$refspec" > /dev/null 2>&1; then
        err "failed to push site"
        return 1
    fi
    msg "published site to $repo_slug"
}

main "$@" || exit 1
exit 0
