#!/bin/zsh

set -euo pipefail
umask 077

readonly signing_dir="${OPENMODEL_SIGNING_DIR:-${HOME}/Library/Application Support/OpenModel Preview/signing}"
readonly private_key_path="${signing_dir}/private-encrypted.pem"
readonly certificate_path="${signing_dir}/certificate-chain.crt"
readonly keychain_service="com.fin.openmodel.preview.signing"
readonly keychain_account="${USER:-$(id -un)}"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        print -u2 "Required command not found: $1"
        exit 1
    fi
}

require_command openssl
require_command security

mkdir -p "$signing_dir"
chmod 700 "$signing_dir"

if [[ -e "$private_key_path" || -e "$certificate_path" ]]; then
    if [[ -f "$private_key_path" && -f "$certificate_path" ]] \
        && security find-generic-password -a "$keychain_account" -s "$keychain_service" >/dev/null 2>&1; then
        chmod 600 "$private_key_path" "$certificate_path"
        print "OpenModel Preview signing is already configured. Existing material was not changed."
        exit 0
    fi

    print -u2 "Incomplete signing configuration found at: $signing_dir"
    print -u2 "Move the incomplete files aside, then run this script again."
    exit 1
fi

private_key_password="$(openssl rand -base64 48 | tr -d '\n')"
cleanup() {
    unset private_key_password
}
trap cleanup EXIT INT TERM

openssl genpkey \
    -algorithm RSA \
    -aes-256-cbc \
    -pass fd:3 \
    -pkeyopt rsa_keygen_bits:4096 \
    -out "$private_key_path" 3<<<"$private_key_password" >/dev/null 2>&1

openssl req \
    -new \
    -x509 \
    -sha256 \
    -days 3650 \
    -key "$private_key_path" \
    -passin fd:3 \
    -subj "/CN=OpenModel Preview/O=FIN Tools/OU=Open Source" \
    -out "$certificate_path" 3<<<"$private_key_password" >/dev/null 2>&1

security add-generic-password \
    -a "$keychain_account" \
    -s "$keychain_service" \
    -w "$private_key_password" \
    -U >/dev/null

chmod 600 "$private_key_path" "$certificate_path"
print "OpenModel Preview signing is configured outside the repository."
print "The encrypted private key password is stored in macOS Keychain."
