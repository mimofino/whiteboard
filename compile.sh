#!/bin/bash
# Compile all whiteboard source files

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
SRC="$ROOT"

mkdir -p "$OUT"

echo "Compiling..."
javac -d "$OUT" \
    "$SRC/common/"*.java \
    "$SRC/server/"*.java \
    "$SRC/client/"*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful. Class files in: $OUT"
    echo ""
    echo "Run server:"
    echo "  java -cp $OUT server.WhiteboardServer <port>"
    echo ""
    echo "Run manager (create):"
    echo "  java -cp $OUT client.CreateWhiteBoard localhost <port> <username>"
    echo ""
    echo "Run peer (join):"
    echo "  java -cp $OUT client.JoinWhiteBoard localhost <port> <username>"
else
    echo "Compilation failed."
    exit 1
fi
