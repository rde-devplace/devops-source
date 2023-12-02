#!/bin/bash

# Environment variables and settings
KEYCLOAK_URI="https://console-dev.amdp-dev.cloudzcp.io/iam"
CLIENT_ID="kube-proxy-renew"
CLIENT_SECRET="B3x4C4DARIYPVemmXghmxZZdFGhfofWN"
USERNAME="himang10"
PASSWORD="ywyi1004"
#KUBE_PROXY_URL="http://localhost:8080/api/route/user/"  # Changed the quotes
KUBE_PROXY_URL="kube-proxy.amdp-dev.skamdp.org/api/route/user/"

# Validate the provided arguments
if [ "$#" -ne 1 ]; then
    echo "Missing required parameters."
    echo "Usage: $0 <NAME>"
    echo "Example: $0 himang10"
    exit 1
fi

NAME=$1

# Retrieve token
TOKEN_RESPONSE=$(curl -s -L -X POST \
  "$KEYCLOAK_URI/realms/amdp-dev/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode "client_secret=$CLIENT_SECRET" \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'scope=email roles profile' \
  --data-urlencode "username=$USERNAME" \
  --data-urlencode "password=$PASSWORD")

# Print token response
echo -e "Token Response:\n$(echo "$TOKEN_RESPONSE" | jq)\n"

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r .access_token)
[ -z "$ACCESS_TOKEN" ] && echo "Failed to retrieve access token." && exit 1

delete_from_proxy() {
    local name=$1

    # Here, assuming DELETE method works with your proxy setup. If not, adjust accordingly.
    RESPONSE=$(curl -s -L -X DELETE "$KUBE_PROXY_URL$name" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $ACCESS_TOKEN")

    echo -e "Deleting ProxyRouter data from $KUBE_PROXY_URL with path $path...\nResponse:\n$(echo "$RESPONSE" | jq)\n"
}

delete_from_proxy "$NAME"

# Delete IdeConfig
kubectl delete ideconfig "$NAME-vscode-server"

if [ $? -ne 0 ]; then
    echo "Failed to delete IdeConfig. Exiting..."
    exit 1
fi

echo -e "\nIdeConfig deleted successfully.\n"

