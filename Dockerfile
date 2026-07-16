# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependências: baixa antes de copiar o código-fonte.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuário não-root.
RUN groupadd --system worklog && useradd --system --gid worklog worklog

# O repackage do Spring Boot gera exatamente um *.jar executável em target/
# (o *.jar.original não casa com o glob).
COPY --from=build /app/target/*.jar app.jar

USER worklog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
