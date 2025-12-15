ALTER TABLE daycare_group
    ADD COLUMN created_at timestamp with time zone NOT NULL DEFAULT now();
ALTER TABLE daycare_group
    ADD COLUMN updated_at timestamp with time zone NOT NULL DEFAULT now();

UPDATE daycare_group dg
    SET created_at = d.created,
        updated_at = d.created
FROM daycare d
where dg.daycare_id = d.id;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_group
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
