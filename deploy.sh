#!/bin/sh

mvn -Psonatype-oss-release \
	clean package deploy -DskipTests

