-- V2__improvements.sql
-- All columns already exist in V1 for fresh installs.
-- This migration is intentionally a no-op safe pass.
-- Flyway repair-on-migrate in application.yml handles any previous failed state.

SELECT 'V2 migration: schema already complete in V1, no changes needed' AS migration_status;