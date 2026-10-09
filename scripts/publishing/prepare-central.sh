#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODE="${1:-signed}"
if (($#)); then shift; fi
case "$MODE" in
    signed|unsigned) ;;
    *) echo 'Usage: prepare-central.sh [signed|unsigned] [Gradle arguments...]' >&2; exit 2 ;;
esac

# Git's GnuPG uses POSIX socket paths, including when called by a Windows JVM.
if [[ -n "${GNUPGHOME:-}" ]] && command -v cygpath >/dev/null; then
    export GNUPGHOME="$(cygpath -u "$GNUPGHOME")"
    export MSYS2_ENV_CONV_EXCL="${MSYS2_ENV_CONV_EXCL:+$MSYS2_ENV_CONV_EXCL;}GNUPGHOME"
fi

TARGETS=(forge-1.18.2 forge-1.19.2 forge-1.20.1 fabric-1.20.1 fabric-1.21.1 fabric-26.1 neoforge-1.21.1 neoforge-26.1 neoforge-26.2)
read_property() {
    sed -n "s/^$2=//p" "$1" | tr -d '\r' | head -1
}
VERSION="$(read_property "$ROOT/gradle.properties" mod_version)"
NAME="$(read_property "$ROOT/gradle.properties" mod_name)"
GROUP="$(read_property "$ROOT/gradle.properties" mod_group_id)"
GROUP_PATH="${GROUP//./\/}"
JAR_ARGS=()
SIGN_ARGS=()
if [[ "$MODE" == signed ]]; then SIGN_ARGS=(-PcentralSigning=true); fi

for target in "${TARGETS[@]}"; do
    wanted="$(read_property "$ROOT/targets/$target/ci.properties" 'ci\.java')"
    override="JAVA_HOME_${wanted}"
    jdk="${!override:-${JAVA_HOME:-}}"
    if [[ ! -x "$jdk/bin/java.exe" && ! -x "$jdk/bin/java" ]]; then
        echo "Set $override or JAVA_HOME to a Java $wanted installation." >&2
        exit 1
    fi
    echo "Preparing $target ($MODE)"
    (
        cd "$ROOT/targets/$target"
        JAVA_HOME="$jdk" ./gradlew publishMavenJavaPublicationToCentralStagingRepository --console=plain "${SIGN_ARGS[@]}" "$@"
    )
    component="$GROUP_PATH/$NAME-$target/$VERSION"
    repository="$ROOT/targets/$target/build/central/repository"
    for extension in jar sources.jar javadoc.jar pom; do
        case "$extension" in
            sources.jar|javadoc.jar) filename="$NAME-$target-$VERSION-$extension" ;;
            *) filename="$NAME-$target-$VERSION.$extension" ;;
        esac
        for suffix in '' .md5 .sha1; do
            [[ -s "$repository/$component/$filename$suffix" ]] || {
                echo "Missing publication file: $component/$filename$suffix" >&2
                exit 1
            }
        done
        if [[ "$MODE" == signed && ! -s "$repository/$component/$filename.asc" ]]; then
            echo "Missing signature: $component/$filename.asc" >&2
            exit 1
        fi
    done
    JAR_ARGS+=(-C "$repository" "$component")
done

if [[ "$MODE" == unsigned ]]; then
    echo 'Nine unsigned components are staged. No upload bundle was created.'
    exit 0
fi

mkdir -p "$ROOT/build/central"
bundle="$ROOT/build/central/$NAME-$VERSION.zip"
"$jdk/bin/jar" --create --no-manifest --file "$bundle" "${JAR_ARGS[@]}"
echo "Central upload bundle: $bundle"
