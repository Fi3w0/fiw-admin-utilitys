#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-https://api.modrinth.com/v2}"
PROJECT_ID="${PROJECT_ID:-f1yH9Ggq}"
VERSION="${VERSION:?VERSION is required}"
CHANGELOG_FILE="${CHANGELOG_FILE:-RELEASE_NOTES.md}"
DESCRIPTION_FILE="${DESCRIPTION_FILE:-MODRINTH.md}"
MODRINTH_TOKEN="${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi
if [ ! -s "$CHANGELOG_FILE" ]; then
  echo "$CHANGELOG_FILE does not exist or is empty" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
auth_header="Authorization: $MODRINTH_TOKEN"
versions_file="$tmp_dir/project-versions.json"

request() {
  local response_file="$1"
  shift
  local http_code
  http_code="$(curl -sS -o "$response_file" -w '%{http_code}' "$@")"
  if [ "$http_code" -ge 400 ]; then
    echo "::error::Modrinth request failed with HTTP $http_code: $(cat "$response_file")" >&2
    return 1
  fi
}

request "$versions_file" -H "$auth_header" "$API_BASE/project/$PROJECT_ID/version"

modrinth_version_exists() {
  local game_version="$1"
  local loader="$2"
  jq -e \
    --arg version_number "$VERSION" \
    --arg game_version "$game_version" \
    --arg loader "$loader" \
    '.[] | select(
      .version_number == $version_number
      and (.game_versions | index($game_version))
      and (.loaders | index($loader))
    )' "$versions_file" >/dev/null
}

publish_version() {
  local label="$1"
  local jar_path="$2"
  local game_version="$3"
  local loader="$4"
  local dependencies_json="$5"
  local data_file="$tmp_dir/$loader-$game_version.json"
  local response_file="$tmp_dir/$loader-$game_version-response.json"

  if [ ! -f "$jar_path" ]; then
    echo "::error file=$jar_path::Missing jar for Modrinth publish" >&2
    exit 1
  fi
  if modrinth_version_exists "$game_version" "$loader"; then
    echo "Modrinth $VERSION ($loader $game_version) already exists; skipping"
    return
  fi

  jq -n \
    --rawfile changelog "$CHANGELOG_FILE" \
    --arg name "Fiw Admin Tools $VERSION $label" \
    --arg version_number "$VERSION" \
    --arg project_id "$PROJECT_ID" \
    --arg game_version "$game_version" \
    --arg loader "$loader" \
    --argjson dependencies "$dependencies_json" \
    '{
      name: $name,
      version_number: $version_number,
      changelog: $changelog,
      dependencies: $dependencies,
      game_versions: [$game_version],
      version_type: "release",
      loaders: [$loader],
      featured: true,
      status: "listed",
      requested_status: "listed",
      project_id: $project_id,
      file_parts: ["file"],
      primary_file: "file"
    }' > "$data_file"

  echo "Publishing $VERSION ($loader $game_version)"
  request "$response_file" -X POST "$API_BASE/version" \
    -H "$auth_header" \
    -F "data=@$data_file;type=application/json" \
    -F "file=@$jar_path"
}

fabric_dependencies='[
  {"project_id":"P7dR8mSH","dependency_type":"required"},
  {"project_id":"Ha28R6CL","dependency_type":"required"}
]'
no_dependencies='[]'

publish_version "Fabric 1.21.11" "fabric-1.21.11/build/libs/fiw-admin-tools-fabric-1.21.11-$VERSION.jar" "1.21.11" "fabric" "$fabric_dependencies"
publish_version "NeoForge 1.21.11" "neoforge-1.21.11/build/libs/fiw-admin-tools-neoforge-1.21.11-$VERSION.jar" "1.21.11" "neoforge" "$no_dependencies"
publish_version "Fabric 1.21.8" "fabric-1.21.8/build/libs/fiw-admin-tools-fabric-1.21.8-$VERSION.jar" "1.21.8" "fabric" "$fabric_dependencies"
publish_version "NeoForge 1.21.8" "neoforge-1.21.8/build/libs/fiw-admin-tools-neoforge-1.21.8-$VERSION.jar" "1.21.8" "neoforge" "$no_dependencies"
publish_version "Fabric 1.21.1" "fabric-1.21.1/build/libs/fiw-admin-tools-fabric-1.21.1-$VERSION.jar" "1.21.1" "fabric" "$fabric_dependencies"
publish_version "NeoForge 1.21.1" "neoforge-1.21.1/build/libs/fiw-admin-tools-neoforge-1.21.1-$VERSION.jar" "1.21.1" "neoforge" "$no_dependencies"
publish_version "Fabric 1.20.1" "fabric-1.20.1/build/libs/fiw-admin-tools-fabric-1.20.1-$VERSION.jar" "1.20.1" "fabric" "$fabric_dependencies"
publish_version "Forge 1.20.1" "forge-1.20.1/build/libs/fiw-admin-tools-forge-1.20.1-$VERSION.jar" "1.20.1" "forge" "$no_dependencies"

if [ -s "$DESCRIPTION_FILE" ]; then
  description_payload="$tmp_dir/project-description.json"
  description_response="$tmp_dir/project-description-response.json"
  jq -n --rawfile body "$DESCRIPTION_FILE" '{body: $body}' > "$description_payload"
  echo "Syncing Modrinth description from $DESCRIPTION_FILE"
  request "$description_response" -X PATCH "$API_BASE/project/$PROJECT_ID" \
    -H "$auth_header" \
    -H "Content-Type: application/json" \
    --data-binary "@$description_payload"
fi
