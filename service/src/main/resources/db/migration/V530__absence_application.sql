CREATE TYPE absence_application_status AS ENUM ('WAITING_DECISION', 'ACCEPTED', 'REJECTED');

CREATE TABLE absence_application
(
    id              uuid PRIMARY KEY                    DEFAULT ext.uuid_generate_v1mc(),
    created_at      timestamp with time zone   NOT NULL DEFAULT now(),
    created_by      uuid                       NOT NULL REFERENCES evaka_user (id),
    updated_at      timestamp with time zone   NOT NULL DEFAULT now(),
    modified_at     timestamp with time zone   NOT NULL,
    modified_by     uuid                       NOT NULL REFERENCES evaka_user (id),
    child_id        uuid                       NOT NULL REFERENCES child (id),
    start_date      date                       NOT NULL,
    end_date        date                       NOT NULL,
    description     text                       NOT NULL,
    status          absence_application_status NOT NULL,
    decided_at      timestamp with time zone,
    decided_by      uuid REFERENCES evaka_user (id),
    rejected_reason text,
    CONSTRAINT check$start_date_before_end_date CHECK (start_date <= end_date),
    CONSTRAINT check$decided_valid CHECK ((decided_at IS NULL) = (decided_by IS NULL)),
    CONSTRAINT check$rejected_reason_valid CHECK ((status = 'REJECTED') = (rejected_reason IS NOT NULL))
);

CREATE INDEX idx$absence_application_created_by ON absence_application (created_by);
CREATE INDEX idx$absence_application_modified_by ON absence_application (modified_by);
CREATE INDEX idx$absence_application_child_id ON absence_application (child_id);
CREATE INDEX idx$absence_application_decided_by ON absence_application (decided_by);

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON absence_application
    FOR EACH ROW
EXECUTE PROCEDURE trigger_refresh_updated_at();
