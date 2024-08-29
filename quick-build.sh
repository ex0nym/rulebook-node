#!/bin/bash

mvn install -DskipTests
cd io.exonym.rulebook
docker build -t rulebook:latest .
docker-compose up -d
cd ..
docker logs rulebook-node -f
