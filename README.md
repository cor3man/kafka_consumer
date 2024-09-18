## Run
local:

    docker-compose up redis
    run app

container:

    build app
        docker-compose up --build

## Database
    http://localhost:8081/h2-console/
    