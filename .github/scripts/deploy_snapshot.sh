#!/bin/bash

SLUG="anggrayudi/MaterialPreference"

set -e

if [ "$GITHUB_REPOSITORY" != "$SLUG" ]; then
  echo "Skipping deployment: wrong repository. Expected '$SLUG' but was '$GITHUB_REPOSITORY'."
elif [ "${GITHUB_REF##*/}" != "master" ]; then
  echo "Skipping deployment: wrong branch. Expected 'master' but was '${GITHUB_REF##*/}'."
else
  echo "Deploying snapshot..."
  ./gradlew :materialpreference:publishAllPublicationsToMavenCentral --no-daemon --no-parallel --stacktrace
  echo "Snapshot released!"
fi
