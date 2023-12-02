 #!/bin/bash
 DOCKER_NAME="ide-operator"

 docker rm $DOCKER_NAME &&
 docker rmi $DOCKER_NAME:1.0
