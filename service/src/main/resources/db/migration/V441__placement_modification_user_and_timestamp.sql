ALTER TABLE placement ADD COLUMN modified_at timestamp with time zone;

ALTER TABLE placement ADD COLUMN modified_by uuid REFERENCES evaka_user(id);
