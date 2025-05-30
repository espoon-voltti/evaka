ALTER TABLE archived_process ADD COLUMN migrated boolean NOT NULL DEFAULT false;
ALTER TABLE archived_process ALTER COLUMN migrated DROP DEFAULT;
