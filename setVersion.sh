#!/bin/sh

if [ $# -eq 0 ]; then
    echo "No arguments provided: pass the version number"
    exit 1
fi

mvn versions:set \
	-DgenerateBackupPoms=false \
	-DnewVersion=$1

