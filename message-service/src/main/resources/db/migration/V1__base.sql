-- Add extensions to 'ext' schema
CREATE SCHEMA IF NOT EXISTS ext;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;

-- Make default grants to application role for objects created by flyway
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, TRIGGER, INSERT, UPDATE, DELETE ON TABLES TO "${application_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO "${application_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO "${application_user}";
