#!/bin/sh

mvn versions:set \
	-DgenerateBackupPoms=false \
	-DremoveSnapshot=true

