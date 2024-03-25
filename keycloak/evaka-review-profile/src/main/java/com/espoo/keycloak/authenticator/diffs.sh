#!/bin/bash

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}" )"

version="${1:-}"

if test -z "$version"; then
    echo "Script to generate diff files"
    echo "Run ./update.sh script first to fetch original files"
    echo "Usage:   $0 <current-keycloak-version>"
    echo "Example: $0 22.0.3"
    exit 1
fi

diff -u "../../../../../../../../update/IdpReviewProfileAuthenticator.java.${version}.orig" EvakaReviewProfileAuthenticator.java > EvakaReviewProfileAuthenticator.java.diff || true
diff -u "../../../../../../../../update/IdpReviewProfileAuthenticatorFactory.java.${version}.orig" EvakaReviewProfileAuthenticatorFactory.java > EvakaReviewProfileAuthenticatorFactory.java.diff || true
