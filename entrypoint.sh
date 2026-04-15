#!/bin/sh

# On s'assure que le script s'arrête en cas d'erreur
set -e

echo "Démarrage de l'application YowPainter Backend..."

# Conversion de l'URL de base de données Render (postgres://) en JDBC (jdbc:postgresql://)
if [ -n "$DATABASE_URL" ]; then
    # sed remplace tout ce qui précède le premier ':' par 'jdbc:postgresql'
    export SPRING_DATASOURCE_URL=$(echo "$DATABASE_URL" | sed 's|^[^:]*|jdbc:postgresql|')
    echo "Configuration JDBC : SPRING_DATASOURCE_URL a été générée."
else
    echo "ERREUR : La variable DATABASE_URL n'est pas définie."
    exit 1
fi

# Application des limites de mémoire si JAVA_OPTS n'est pas défini
if [ -z "$JAVA_OPTS" ]; then
    export JAVA_OPTS="-Xmx384m -Xms384m"
fi

echo "Exécution : java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"

# Lancement de l'application
# exec permet au processus Java de devenir le PID 1 et de recevoir les signaux d'arrêt
exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}
