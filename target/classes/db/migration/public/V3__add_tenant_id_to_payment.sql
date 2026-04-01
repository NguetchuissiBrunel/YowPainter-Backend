-- Migration pour ajouter le tenant_id à la table payment
-- Nécessaire pour basculer sur le bon schéma lors du callback de paiement
ALTER TABLE payment ADD COLUMN tenant_id VARCHAR(100);
