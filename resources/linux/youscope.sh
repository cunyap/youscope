#!/usr/bin/env bash
#
# youscope.sh
#
# Launches YouScope on Linux with JDK 21 and the required JVM options.
# run this script instead of calling java by hand. It can be started from 
# a terminal or by double-click in a file manager.
#
# Java is searched in this order:
#   1. ./runtime/bin/java   (bundled JDK, only present in the "with-jdk21" archive)
#   2. $JAVA_HOME/bin/java
#   3. "java" on the PATH
# The selected Java must be version 21 or newer.
#
# Optional environment variables:
#   YOUSCOPE_JAVA_OPTS   Additional JVM options, e.g. "-Xmx4g".

# Locate the installation folder (follows symlinks)
SOURCE="${BASH_SOURCE[0]}"
while [ -L "$SOURCE" ]; do
    LINK_DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
    SOURCE="$(readlink "$SOURCE")"
    case "$SOURCE" in
        /*) ;;
        *) SOURCE="$LINK_DIR/$SOURCE" ;;
    esac
done
YOUSCOPE_DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

# YouScope uses paths relative to the working directory (lib/, plugins/, ...).
cd "$YOUSCOPE_DIR" || exit 1

# Error reporting (also visible when started without a terminal)
fail() {
    echo "ERROR: $*" >&2
    if [ ! -t 2 ]; then
        if command -v zenity >/dev/null 2>&1; then
            zenity --error --title="YouScope" --no-markup --text="$*" 2>/dev/null
        elif command -v xmessage >/dev/null 2>&1; then
            xmessage -center "YouScope: $*" 2>/dev/null
        fi
    fi
    exit 1
}

# Find Java
JAVA_BIN=""
if [ -x "$YOUSCOPE_DIR/runtime/bin/java" ]; then
    JAVA_BIN="$YOUSCOPE_DIR/runtime/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(command -v java)"
else
    fail "No Java found. Install JDK 21 (e.g. Temurin 21 from https://adoptium.net/), set JAVA_HOME, or use the YouScope archive that includes a bundled JDK 21."
fi

JAVA_MAJOR="$("$JAVA_BIN" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)"
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 21 ]; then
    fail "YouScope requires Java 21 or newer, but '$JAVA_BIN' reports version '${JAVA_MAJOR:-unknown}'."
fi

# Display 
if [ -z "${DISPLAY:-}" ] && [ -z "${WAYLAND_DISPLAY:-}" ]; then
    fail "No graphical display found (DISPLAY is not set). YouScope needs a desktop session."
fi

# Native libraries
# Device drivers are not part of the current Linux archives. If they are added
# later (folder "drivers/"), they are found here.
DRIVER_DIR="$YOUSCOPE_DIR/drivers"
export LD_LIBRARY_PATH="$DRIVER_DIR${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

JAVA_ARGS=(
    "-Djava.library.path=$DRIVER_DIR"
    "-Dpython.console.encoding=UTF-8"
    "--enable-native-access=ALL-UNNAMED"
    "--add-opens=java.base/java.util=ALL-UNNAMED"
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
    "--add-opens=java.base/java.text=ALL-UNNAMED"
    "--add-opens=java.base/java.math=ALL-UNNAMED"
    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
)

# Additional user-defined options (intentionally word-split).
# shellcheck disable=SC2206
EXTRA_ARGS=( ${YOUSCOPE_JAVA_OPTS:-} )

echo "Using Java: $JAVA_BIN (version $JAVA_MAJOR)"

# Start
if [ -t 1 ]; then
    # Started from a terminal: output goes to the terminal.
    exec "$JAVA_BIN" "${JAVA_ARGS[@]}" "${EXTRA_ARGS[@]}" -jar "$YOUSCOPE_DIR/lib/youscope.jar"
else
    # Started without a terminal (e.g. double-click): keep the output in a log file.
    LOG_FILE="$YOUSCOPE_DIR/youscope-launch.log"
    if ! : >> "$LOG_FILE" 2>/dev/null; then
        LOG_FILE="${TMPDIR:-/tmp}/youscope-launch.log"
    fi
    {
        echo "=== $(date) ==="
        echo "Using Java: $JAVA_BIN (version $JAVA_MAJOR)"
    } >> "$LOG_FILE" 2>&1
    exec "$JAVA_BIN" "${JAVA_ARGS[@]}" "${EXTRA_ARGS[@]}" -jar "$YOUSCOPE_DIR/lib/youscope.jar" >> "$LOG_FILE" 2>&1
fi
