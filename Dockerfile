FROM eclipse-temurin:21-jdk-jammy as build

WORKDIR /temp
COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre-jammy

COPY --from=build /temp/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]