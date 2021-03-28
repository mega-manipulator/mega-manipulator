#!/bin/bash

# Really strict =)
set -Eexuo pipefail

# Error context - borrowed from https://unix.stackexchange.com/a/522815/86300
trap 'echo >&2 "Error - exited with status $? at line $LINENO:";
         pr -tn $0 | tail -n+$((LINENO - 3)) | head -n7' ERR
temp_dir="$(mktemp -d)"
export temp_dir
trap 'rm -rf "$temp_dir"' EXIT

echo 'Hello world'
echo 'Wanna execute your own script? Delegate that from here!' | grep -q 'foooooo' # Intentional failure
