-- ============================================================================
-- Initialize All Databases for Microservices
-- ============================================================================
-- This script runs when PostgreSQL container starts for the first time
-- ============================================================================

-- Create databases for microservices
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS emr_integration_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS pain_escalation_db;
CREATE DATABASE IF NOT EXISTS external_vas_db;
CREATE DATABASE IF NOT EXISTS backup_service;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE emr_integration_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pain_escalation_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE external_vas_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE backup_service TO postgres;
