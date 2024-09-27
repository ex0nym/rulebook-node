#!/bin/bash
mvn clean install -DskipTests

cd io.exonym.rulebook
docker build -t rulebook:latest .
