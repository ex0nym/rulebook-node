#!/bin/bash

set -e

if [ -n "$1" ]; then
  mvn clean install -DskipTests
  cd io.exonym.x0basic

  export name="x0basic"

  export linux_amd64="$name":linux-amd64-"$1"
  echo "$linux_amd64"
  docker build --platform linux/amd64 -t "$linux_amd64" . || exit 1
  docker tag "$linux_amd64" exox/"$linux_amd64" || exit 1
  docker push exox/"$linux_amd64" || exit 1

  export linux_arm64="$name":linux-arm64-"$1"  || exit 1
  echo "$linux_arm64"  || exit 1
  docker build --platform linux/arm64 -t "$linux_arm64" . || exit 1
  docker tag "$linux_arm64" exox/"$linux_arm64" || exit 1
  docker push exox/"$linux_arm64" || exit 1

  export linux_arm64v7="$name":linux-arm64-v7-"$1" || exit 1
  echo "$linux_arm64v7" || exit 1
  docker build --platform linux/arm/v7 -t "$linux_arm64v7" . || exit 1
  docker tag "$linux_arm64v7" exox/"$linux_arm64v7" || exit 1
  docker push exox/"$linux_arm64v7" || exit 1

  docker manifest create exox/"$name":"$1" \
       exox/"$linux_amd64" \
       exox/"$linux_arm64" \
       exox/"$linux_arm64v7" || exit 1

  docker manifest push exox/"$name":"$1"

#  cd ../io.exonym.x0basic
#  docker build --platform linux/amd64 -t x0basic:latest .
#  docker tag x0basic:latest exox/x0basic:$1
#  docker push exox/x0basic:$1

else
  echo "You need to set a version number - look up latest on docker hub"
  exit;
  
fi

