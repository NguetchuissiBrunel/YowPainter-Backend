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

# Installation de sed (déjà présent dans alpine mais pour la forme)
RUN apk add --no-cache sed

# Copier le JAR généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Optimisation pour environnements Cloud (Render Free Tier : 512MB RAM)
ENV JAVA_OPTS="-Xmx384m -Xms384m"

# Commande de lancement :
# 1. On convertit dynamiquement le protocole (postgres: ou postgresql:) en jdbc:postgresql:
# 2. On passe l'URL en argument --spring.datasource.url pour qu'elle soit prioritaire
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080} --spring.datasource.url=$(echo $DATABASE_URL | sed 's|^[^:]*|jdbc:postgresql|')"]
