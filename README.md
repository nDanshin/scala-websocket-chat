# scala-websocket-chat

sbt docker:publishLocal

docker network create test-network
docker run --rm -d -p 5432:5432 --name postgres-docker -e POSTGRES_PASSWORD=docker -e POSTGRES_DB=scalachat --network test-network postgres
docker run --rm -p 8080:8080 --network test-network scala-websocket-chat:0.0.1-SNAPSHOT

default password: docker

to enter psql and create database (if it has not created already):
docker exec -it postgres-docker psql -U postgres -W -c "create database scalachat;"