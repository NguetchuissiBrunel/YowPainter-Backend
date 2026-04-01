# YowPainter Backend API 🎨

Une plateforme SaaS multi-tenant moderne conçue pour les artistes et les collectionneurs d'art. Le backend gère l'isolation complète des données pour chaque artiste (tenant) tout en offrant une expérience fluide pour les acheteurs sur le schéma public.

## 🚀 Stack Technique

Le projet a été récemment modernisé vers les standards les plus récents :

*   **Framework** : Spring Boot 4.0.0 (basé sur Spring Framework 7.0)
*   **Base de Données** : PostgreSQL 18.0
*   **Multi-Tenancy** : Isolation par schéma (Schema-per-tenant) via Hibernate 6
*   **Sécurité** : Spring Security 7.0 avec Authentification JWT (Stateless)
*   **Paiements** : Intégration mobile money via **CamPay** (MTN, Orange, MoMo Pay)
*   **Migrations** : Flyway (gestion automatique des schémas public et artistes)
*   **Documentation** : OpenAPI 3 / Swagger UI (SpringDoc v3)
*   **E-mails** : Spring Boot Mail (pour la récupération de mot de passe)
*   **Utilitaires** : Lombok, Jakarta Validation

## ✨ Fonctionnalités Clés

### 🔐 Authentification & Sécurité
*   **Inscription & Connexion** : Support des rôles ARTISTE, ACHETEUR et ADMIN.
*   **Multi-Tenant Provisioning** : Chaque nouvel artiste reçoit automatiquement son propre schéma de base de données isolé.
*   **Réinitialisation de mot de passe** : Système complet par jeton UUID envoyé par e-mail sécurisé.

### 💰 Système de Paiement (CamPay)
*   **Flux Mobile Money** : Notification USSD Push directe sur le téléphone de l'acheteur.
*   **Boutique (Shop)** : Paiement des commandes d'articles d'art.
*   **Événements** : Réservation et paiement de billets pour des vernissages ou expositions.
*   **Callbacks** : Réception automatique des notifications de succès/échec de paiement.

### 🎨 Gestion Artistique
*   **Artwork** : Gestion des collections et des images d'œuvres.
*   **Artiste** : Profil personnalisable avec slug unique (utilisé comme identifiant de tenant).
*   **Recherche** : Système de recherche global et filtré.

### 🔔 Notifications & Abonnements
*   **Alertes** : Système de notification interne pour les nouvelles commandes ou réservations.
*   **Abonnements** : Gestion des abonnés aux profils d'artistes.

## ⚙️ Configuration & Installation

### Prérequis
*   Java 17 ou supérieur
*   Maven 3.9+
*   PostgreSQL 18

### Configuration (`src/main/resources/application.yml`)
Vous devez configurer les variables suivantes :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nom_votre_db
    username: votre_user
    password: votre_password
  mail:
    host: smtp.votre_fournisseur.com
    username: votre_email@exemple.com
    password: votre_mot_de_passe_app

jwt:
  secret: votre_cle_secrete_de_32_caracteres_minimum

app:
  payment:
    campay:
      app-username: votre_campay_user
      app-password: votre_campay_password
  frontend-url: http://localhost:3000
```

### Lancement
```bash
mvn clean install
mvn spring-boot:run
```

## 📖 API Documentation
Une fois l'application lancée, la documentation interactive Swagger est accessible à l'adresse :
`http://localhost:8080/swagger-ui.html`

---
*YowPainter - Propulsant la nouvelle génération d'artistes.*
