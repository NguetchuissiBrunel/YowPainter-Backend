# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler le JAR
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Installation de sed et bash pour le script d'entrée
RUN apk add --no-cache sed bash

# Copier le JAR généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Copier le script d'entrée
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

# Port par défaut (Render injectera PORT)
EXPOSE 8080

# Utilisation du script comme point d'entrée
ENTRYPOINT ["./entrypoint.sh"]
