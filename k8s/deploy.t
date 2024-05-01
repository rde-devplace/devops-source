apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${IMAGE_NAME}
  namespace: ${NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${IMAGE_NAME}
  template:
    metadata:
      annotations:
        prometheus.io/scrape: 'true'
        prometheus.io/port: '8081'
        prometheus.io/path: '/actuator/prometheus'
        update: ${HASHCODE}
      labels:
        app: ${IMAGE_NAME}
    spec:
      serviceAccountName: cluster-admin-sa
      imagePullSecrets:
      - name: harbor-registry-secret
      containers:
      - name: ${IMAGE_NAME}
        image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${VERSION}
        imagePullPolicy: Always
        securityContext:
            runAsUser: 0
            privileged: true
        env:
        - name: LOGGING_LEVEL
          value: ${LOGGING_LEVEL}
        - name: COMDEV_PVC_NAME
          value: "com-dev-pvc"
        - name: USER_STORAGE_CLASS_NAME
          value: "gp2"
        - name: VSCODE_IMAGE_PATH
          value: "${DOCKER_REGISTRY}/devplace-vscode-server:2.1.4221"
        - name: SSHSERVER_IMAGE_PATH
          value: "${DOCKER_REGISTRY}/ssh-with-k9s-extend:1.0.1"
        - name: WETTY_IMAGE_PATH
          value: "${DOCKER_REGISTRY}/wetty-with-k9s:1.1"
        - name: NOTEBOOK_IMAGE_PATH
          value: "${DOCKER_REGISTRY}/devplace-notebook:1.0.10"
        - name: IDE_PROXY_DOMAIN
          value: "${IDE_PROXY_DOMAIN}"
        - name: APP_DOMAIN_TYPE
          value: ${APP_DOMAIN_TYPE}
