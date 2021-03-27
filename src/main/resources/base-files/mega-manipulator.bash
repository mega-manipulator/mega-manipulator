#!/bin/bash

set -exo pipefail

trap 'Error at line $LINENO' ERR
trap 'echo "Done!"' EXIT

echo 'Hello world'
echo 'Wanna execute your own script? Delegate that from here!'
