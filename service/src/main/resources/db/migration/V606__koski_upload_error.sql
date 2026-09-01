CREATE TABLE koski_upload_error (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    type koski_study_right_type NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    error text NOT NULL,
    status_code integer NOT NULL,
    errored_at timestamp with time zone NOT NULL,
    errored_since timestamp with time zone NOT NULL
);

ALTER TABLE koski_upload_error
    ADD CONSTRAINT fk$child_id FOREIGN KEY (child_id) REFERENCES child (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk$unit_id FOREIGN KEY (unit_id) REFERENCES daycare (id);

CREATE UNIQUE INDEX uniq$koski_upload_error_child_unit_type ON koski_upload_error (child_id, unit_id, type);

CREATE INDEX idx$koski_upload_error_unit_id ON koski_upload_error (unit_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON koski_upload_error
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
