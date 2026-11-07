#!/bin/zsh

set -euo pipefail

readonly project_dir="${0:A:h:h}"
readonly signing_dir="${OPENMODEL_SIGNING_DIR:-${HOME}/Library/Application Support/OpenModel Preview/signing}"
readonly private_key_path="${signing_dir}/private-encrypted.pem"
readonly certificate_path="${signing_dir}/certificate-chain.crt"
readonly keychain_service="com.fin.openmodel.preview.signing"
readonly keychain_account="${USER:-$(id -un)}"

if [[ ! -f "$private_key_path" || ! -f "$certificate_path" ]]; then
    print -u2 "Signing material is missing. Run scripts/setup_local_signing.command first."
    exit 1
fi

private_key_password="$(security find-generic-password -w -a "$keychain_account" -s "$keychain_service")"
export CERTIFICATE_CHAIN="$(<"$certificate_path")"
export PRIVATE_KEY="$(<"$private_key_path")"
export PRIVATE_KEY_PASSWORD="$private_key_password"

cleanup() {
    unset CERTIFICATE_CHAIN PRIVATE_KEY PRIVATE_KEY_PASSWORD private_key_password
}
trap cleanup EXIT INT TERM

if [[ -z "${JAVA_HOME:-}" && -d "/Applications/WebStorm.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/WebStorm.app/Contents/jbr/Contents/Home"
fi

gradle_args=(clean test signPlugin verifyPluginSignature verifyPluginStructure verifyPluginProjectConfiguration)
if [[ -d "/Applications/WebStorm.app" ]]; then
    gradle_args+=("-PlocalIdePath=/Applications/WebStorm.app")
fi

cd "$project_dir"
./gradlew "${gradle_args[@]}"

signed_zip="$(find build/distributions -maxdepth 1 -type f -name '*-signed.zip' -print -quit)"
if [[ -z "$signed_zip" ]]; then
    print -u2 "Gradle finished without producing a signed plugin ZIP."
    exit 1
fi

print "Signed plugin: ${project_dir}/${signed_zip}"
shasum -a 256 "$signed_zip"
