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

# Copier le JAR généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Optimisation pour environnements Cloud (Render Free Tier : 512MB RAM)
# On limite la heap à 384MB pour laisser de la place au système
ENV JAVA_OPTS="-Xmx384m -Xms384m"

# Commande de lancement (sh -c permet l'expansion des variables d'env comme PORT)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
