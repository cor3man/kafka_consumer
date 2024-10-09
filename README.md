## Run

local:

    docker-compose up redis
    run app

container:

    build app
        docker-compose up --build

## Database

    http://localhost:8081/h2-console/

## JDBC Connector install

docker run --rm -v /tmp:/tmp confluentinc/cp-base:latest bash -c "confluent-hub install confluentinc/kafka-connect-jdbc:latest --dest /tmp"

