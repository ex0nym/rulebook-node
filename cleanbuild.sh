#!/bin/bash
mvn clean install -DskipTests

#cd io.exonym.x0basic
#docker build -t x0basic:latest .

cd io.exonym.rulebook
docker build -t rulebook:latest .
