#!/usr/bin/env bash
# Uploads the assembled Maven Central deployment bundle to Sonatype's
# Central Portal and polls it to PUBLISHED / FAILED.
#
# Prerequisites:
#   - A namespace verified at central.sonatype.com (e.g. io.github.mahdimerhi)
#   - A Portal user token exported as SONATYPE_TOKEN = "<username>:<password>"
#   - GPG-signed artifacts: run assembleCentralBundle with signing.key /
#     signing.password set (the portal rejects unsigned bundles)
#
# Usage: scripts/publish-central.sh [path-to-bundle.zip]
set -euo pipefail

BUNDLE="${1:-build/central/kotlintrace-central-bundle.zip}"
UPLOAD_URL="https://central.sonatype.com/api/v1/publisher/upload"
STATUS_URL="https://central.sonatype.com/api/v1/publisher/status"
PUBLISH_URL="https://central.sonatype.com/api/v1/publisher/deployment"

if [[ -z "${SONATYPE_TOKEN:-}" ]]; then
    echo "error: SONATYPE_TOKEN env var not set (central.sonatype.com portal token)" >&2
    exit 1
fi
if [[ ! -f "$BUNDLE" ]]; then
    echo "error: bundle not found: $BUNDLE (run ./gradlew assembleCentralBundle)" >&2
    exit 1
fi

AUTH="Authorization: Bearer $(printf '%s' "$SONATYPE_TOKEN" | base64)"

echo "Uploading $BUNDLE ..."
RESP="$(curl -sS --request POST \
    --header "$AUTH" \
    --form "bundle=@${BUNDLE}" \
    --form publishingType=AUTOMATIC \
    --form name=kotlintrace \
    "$UPLOAD_URL")"

DEPLOYMENT_ID="$(printf '%s' "$RESP" | python3 -c '
import sys, json
raw = sys.stdin.read().strip()
try:
    print(json.loads(raw)["deploymentId"])
except Exception:
    print(raw)
')"
echo "Deployment accepted: id=$DEPLOYMENT_ID"

for i in $(seq 1 18); do
    sleep 10
    STATE="$(curl -sS --request POST --header "$AUTH" --data-urlencode "id=${DEPLOYMENT_ID}" "$STATUS_URL" \
        | python3 -c 'import sys,json; print(json.load(sys.stdin)["deploymentState"])')"
    echo "[$((10 * i))s] state=$STATE"
    case "$STATE" in
        PUBLISHED)
            echo "Successfully published to Maven Central."; exit 0 ;;
        FAILED | VALIDATION_FAILED)
            echo "Deployment failed — check https://central.sonatype.com for details." >&2; exit 2 ;;
    esac
done

echo "Still processing; monitor the deployment at central.sonatype.com (id=$DEPLOYMENT_ID)." >&2
exit 3