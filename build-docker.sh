 #!/bin/bash
 DOCKER_NAME="ide-operator"
 VERSION="1.0"

JAR_FILE_PATH=target/ideoperators-0.0.1-SNAPSHOT.jar

mvn clean install -DskipTests=true && \

docker build --tag $DOCKER_NAME:$VERSION --build-arg JAR_FILE=${JAR_FILE_PATH} --platform=linux/arm64 --no-cache .
