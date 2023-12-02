#!/bin/bash

APP_NAME="ide-operator"

# Deployment 이름으로부터 POD 이름 추출
get_pod_name() {
  while true; do
    pod_count=$(kubectl get pods -l app=$APP_NAME -o json | jq '.items | length')
    if [[ $pod_count -eq 1 ]]; then
      kubectl get pods -l app=$APP_NAME -o jsonpath='{.items[0].metadata.name}'
      break
    else
      echo "Waiting for the pod count to become 1..."
      sleep 1
    fi
  done
}

get_pod_name2() {
  kubectl get pods -l app=$APP_NAME -o jsonpath='{.items[0].metadata.name}'
}

# POD의 상태 확인
get_pod_status() {
  kubectl get pod $(get_pod_name) -o jsonpath='{.status.phase}'
}


./build.sh && \
./docker-push.sh && \
kubectl delete -f ./k8s/deploy.yaml && \
kubectl apply -f ./k8s/deploy.yaml && \
kubectl get pod

# POD 이름 추출
POD_NAME=$(get_pod_name)

echo "pod name = $POD_NAME"

# POD가 존재하지 않으면, 또는 POD의 상태가 Running이 아니면 대기
while [[ -z "$POD_NAME" || "$(get_pod_status)" != "Running" ]]; do
  echo "current status is $(get_pod_status)"
  echo "Waiting for the pod to be in Running state..."
  sleep 1
  POD_NAME=$(get_pod_name)
done

kubectl logs -f $(get_pod_name)
