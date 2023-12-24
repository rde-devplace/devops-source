#!/bin/bash

# Environment variables and settings
KEYCLOAK_URI="https://console-dev.amdp-dev.cloudzcp.io/iam"
CLIENT_ID="kube-proxy-renew"
CLIENT_SECRET="B3x4C4DARIYPVemmXghmxZZdFGhfofWN"
USERNAME="himang10"
PASSWORD="ywyi1004"
#KUBE_PROXY_BASE_URL="http://localhost:8080"
KUBE_PROXY_BASE_URL=kube-proxy.amdp-dev.skamdp.org
KUBE_PROXY_URL="$KUBE_PROXY_BASE_URL/api/route"

사용법_출력() {
    echo "사용법: $0 <NAME>"
    echo "예시: $0 himang10"
    echo "'--help'를 사용하여 자세한 정보를 확인하세요."
}

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    사용법_출력
    echo -e "\n이 스크립트는 제공된 NAME에 따라 원하는 환경을 설정합니다."
    echo "이름으로 'init'을 제공하면:"
    echo "  - 스크립트는 프록시에 초기값을 보냅니다."
    echo "특정 'userName'이 제공되면:"
    echo "  - 스크립트는 해당 사용자를 위한 프록시에 라우터를 추가합니다."
    echo "  - 사용자 환경 설정을 위해 IdeConfig가 적용됩니다."
    echo "토큰 검색, 프록시에 데이터 전송 및 IdeConfig 적용과 같은 작업을 수행합니다."
    exit 0
fi

if [ "$#" -ne 1 ]; then
    echo "누락되거나 잘못된 매개변수입니다."
    사용법_출력
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


send_data_to_proxy() {
    local path=$1
    local details=($2)
    local uri=${details[0]}
    local pattern=${details[1]}
    local replacement=${details[2]}
    local data='{
        "path": "'"$path"'",
        "uri": "'"$uri"'",
        "method": "GET,POST,PUT,DELETE,PATCH,OPTIONS",
        "host": null,
        "headerName": null,
        "headerValue": null,
        "tokenRelay": true,
        "userName":  "'"$NAME"'"
    }'

    if [ ! -z "$pattern" ]; then
        data=$(echo "$data" | jq --arg pattern "$pattern" --arg replacement "$replacement" '. + {"pathPattern": $pattern, "pathReplacement": $replacement}')
    fi

    RESPONSE=$(curl -s -L -X POST "$KUBE_PROXY_URL" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d "$data")

    echo -e "Sending ProxyRouter data to $KUBE_PROXY_URL with path $path and uri $uri...\nResponse:\n$(echo "$RESPONSE" | jq)\n"
}


# Special handling for 'init'
if [ "$NAME" == "init" ]; then
    send_data_to_proxy "/console/**" "http://frontend-ide-service:8080 /(?<segment>.*) /\${segment}"
    send_data_to_proxy "/api/ideconfig/ide" "http://ide-operator-service:8080 /(?<segment>.*) /\${segment}"
    exit 0
fi


# Process PATHS, URIS, PATTERNS, REPLACEMENTS
# 경로 배열
PATHS=(
    "/$NAME/vscode/**"
    "/$NAME/cli/**"
    "/$NAME/proxy/8080/**"
    "/$NAME/proxy/9090/**"
)

# URI 배열
URIS=(
    "http://$NAME-vscode-server-service:8443"
    "http://$NAME-vscode-server-service:3000"
    "http://$NAME-vscode-server-service:8080"
    "http://$NAME-vscode-server-service:9090"
)

# 패턴 배열
PATTERNS=(
    "/$NAME/(?<segment>.*)"
    "/(?<segment>.*)"
    "/$NAME/proxy/8080/(?<segment>.*)"
    "/$NAME/proxy/9090/(?<segment>.*)"
)

# 치환 값 배열
REPLACEMENTS=(
    "/\${segment}"
    "/\${segment}"
    "/\${segment}"
    "/\${segment}"
)

# 배열의 길이만큼 반복하여 각 값을 처리합니다.
for i in "${!PATHS[@]}"; do
    send_data_to_proxy "${PATHS[$i]}" "${URIS[$i]} ${PATTERNS[$i]} ${REPLACEMENTS[$i]}"
done

# Print result
echo "--------------------------"
curl --location --request GET "$KUBE_PROXY_BASE_URL/list" \
--header 'Accept: application/json' \
--header "Authorization: Bearer $ACCESS_TOKEN"
echo -e "\n--------------------------"

