# AtmApplication

## Overview

This is a simple HTTP Restful Web service built using spring boot Webflux. The content of the app itself is about a simple ATM Service.

## Java Version
`21`

## Technologies

- Kotlin
- Spring Boot Reactive Webflux
- Reactive Mongo
- Project Lombok mainly for auto-generating getter & setter
- Gradle build tools

## How to run test locally
```
$ ./gradlew test  # macOS/Linux
$ gradlew.bat test  # Windows
```

## How to run via Docker
```
$ cd docker
$ docker compose up -d
```

## Api Docs
http://localhost:8080/swagger-ui/index.html