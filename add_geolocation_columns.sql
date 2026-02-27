-- Script SQL pour ajouter les colonnes de géolocalisation à job_offer
-- Exécutez ce script dans votre base de données MySQL

-- Ajouter les colonnes latitude et longitude
ALTER TABLE job_offer
ADD COLUMN latitude DOUBLE NULL AFTER location,
ADD COLUMN longitude DOUBLE NULL AFTER latitude;

-- Vérification
DESCRIBE job_offer;

