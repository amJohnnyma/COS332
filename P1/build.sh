#!/usr/bin/env bash
set -e

echo "Building project..."

CLASSES_DIR="dist/classes"
CGI_DIR="dist/cgi-bin"

mkdir -p "$CLASSES_DIR" "$CGI_DIR"

# Compile all Java sources
javac -d "$CLASSES_DIR" src/*.java

# Copy and make CGI scripts executable
cp cgi-bin/*.cgi "$CGI_DIR/"
chmod +x "$CGI_DIR/"*.cgi

echo "Build complete. Output in dist/"

# --- Create submission archive with absolute paths ---
echo "Creating submission archive..."

# Staging directory that mirrors the absolute paths
STAGE="$(pwd)/stage"
rm -rf "$STAGE"

# CGI scripts go to /usr/lib/cgi-bin/
mkdir -p "$STAGE/usr/lib/cgi-bin/classes"
cp cgi-bin/*.cgi "$STAGE/usr/lib/cgi-bin/"
chmod +x "$STAGE/usr/lib/cgi-bin/"*.cgi

# Compiled classes go alongside, in a classes/ subfolder
cp dist/classes/*.class "$STAGE/usr/lib/cgi-bin/classes/"

# Source files go to your home directory (as spec requires for compiled programs)
mkdir -p "$STAGE${HOME}/P1/src"
cp src/*.java "$STAGE${HOME}/P1/src/"

# Create zip with paths rooted at /
cd "$STAGE"
zip -r "$(dirname "$STAGE")/submission.zip" usr/ home/
cd - > /dev/null

rm -rf "$STAGE"
echo "Submission archive created: submission.zip"
echo "Run with ./run.sh for local testing"
