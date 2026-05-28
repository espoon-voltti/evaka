ALTER TABLE nekku_special_diet_choices
    ADD COLUMN id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE nekku_special_diet_choices
    ALTER COLUMN created_at DROP DEFAULT;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON nekku_special_diet_choices FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
