FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven descriptor and sources
COPY pom.xml .
COPY src ./src

# Build Spring Boot fat JAR (tests can be skipped in CI if needed)
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar (any backend-*.jar) to a stable name
COPY --from=build /app/target/backend-*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


