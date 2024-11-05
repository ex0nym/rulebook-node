#!/bin/bash

# Enable strict error handling
set -e

usage() {
    echo "Usage: $0 [clean] [--no-build] [--no-start]"
    echo "  clean       Run 'mvn clean install' instead of 'mvn install'"
    echo "  --no-build  Skip the build process"
    echo "  --no-start  Skip starting the Docker containers"
    exit 1
}

# Default behavior: do everything
CLEAN_FLAG=false
NO_BUILD_FLAG=false
NO_START_FLAG=false

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        clean) CLEAN_FLAG=true ;;
        --no-build) NO_BUILD_FLAG=true ;;
        --no-start) NO_START_FLAG=true ;;
        *) usage ;;
    esac
    shift
done


# Build the Docker image unless --no-build is passed
if [ "$NO_BUILD_FLAG" = false ]; then
  if [ "$CLEAN_FLAG" = true ]; then
      echo "Running 'mvn clean install'..."
      mvn clean install -DskipTests
  else
      echo "Running 'mvn install'..."
      mvn install -DskipTests
  fi
  echo "Building Docker image..."
  cd io.exonym.rulebook
  docker build -t rulebook:latest .
else
    echo "Skipping Docker build..."
fi

# Start the Docker containers unless --no-start is passed
# Allows VPN usage for communicating with local machine
if [ "$NO_START_FLAG" = false ]; then
    echo "Starting Docker containers..."
    HOST_IP=$(ifconfig en0 | grep "inet " | awk '{ print $2 }')
    HOST_NAME=$(hostname)
    echo "${HOST_NAME}:${HOST_IP}"
    HOST_IP=$HOST_IP HOST_NAME=$HOST_NAME docker compose up -d
else
    echo "Skipping Docker start..."
fi

# Summary
if [ "$NO_BUILD_FLAG" = true ] && [ "$NO_START_FLAG" = true ]; then
    echo "No actions performed due to --no-build and --no-start flags."
elif [ "$NO_BUILD_FLAG" = true ]; then
    echo "Skipped build, only started containers."
elif [ "$NO_START_FLAG" = true ]; then
    echo "Built Docker image, but skipped starting containers."
else
    echo "All steps completed."
fi
