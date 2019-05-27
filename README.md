# scala-websocket-chat

to publish docker image:
```
sbt docker:publishLocal
```

create network for containers:
```
docker network create test-network
```

run postgres container with "scalachat" database:
```
docker run --rm -d -p 5432:5432 --name postgres-docker -e POSTGRES_PASSWORD=docker -e POSTGRES_DB=scalachat --network test-network postgres
```

manualy enter psql and create database (if it has not been created already):
```
docker exec -it postgres-docker psql -U postgres -W -c "create database scalachat;"
```

run application:
```
docker run --rm -p 8080:8080 --network test-network scala-websocket-chat:0.0.1-SNAPSHOT
```
