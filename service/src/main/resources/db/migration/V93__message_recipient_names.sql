ALTER TABLE message ADD COLUMN recipient_names text[] DEFAULT '{}' NOT NULL;
