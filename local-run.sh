 #!/bin/bash
 DOCKER_NAME="ide-operator"
 VERSION="1.0"

 docker run --rm -it --name $DOCKER_NAME -p 8080:8080 $DOCKER_NAME:$VERSION
