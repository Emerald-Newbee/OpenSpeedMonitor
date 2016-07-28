#!/bin/bash

set -e

GITHUB_API_ACCESS_TOKEN_ITERASPEED=$bamboo_github_API_ACCESS_TOKEN_ITERASPEED

GITHUB_REPOSITORY_URL=$bamboo_github_REPOSITORY_URL

GITHUB_RELEASE_URL=$bamboo_github_RELEASE_URL


# create github release ###############################

echo "sending release notes to github"

TAG_NAME=v$bamboo_ci_app_version
RELEASE_NOTES="tbd"

response=$(/usr/bin/curl -i -H "Authorization: token $GITHUB_API_ACCESS_TOKEN_ITERASPEED" -H "Accept: application/vnd.github+json" --data "{\"tag_name\": \"$TAG_NAME\", \"body\": \"$RELEASE_NOTES\"}" "$GITHUB_RELEASE_URL")
status_code=$(echo "$response" | grep "HTTP/1.1" | cut -d" " -f 2)

if [ $status_code -eq "201" ];
then
	return 0
else
  	return 1
fi
