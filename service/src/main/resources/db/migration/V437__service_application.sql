CREATE TYPE service_application_decision_status AS ENUM ('ACCEPTED', 'REJECTED');

CREATE TABLE service_application(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    sent_at timestamp with time zone NOT NULL,
    person_id uuid NOT NULL REFERENCES person(id),
    child_id uuid NOT NULL REFERENCES child(id),
    start_date date NOT NULL,
    service_need_option_id uuid NOT NULL REFERENCES service_need_option(id),
    additional_info text NOT NULL,
    decision_status service_application_decision_status,
    decided_by uuid REFERENCES employee(id) CHECK (
        (decided_by IS NULL) = (decision_status IS NULL)
    ),
    decided_at timestamp with time zone CHECK (
        (decided_at IS NULL) = (decision_status IS NULL)
    ),
    rejected_reason text CHECK (
        (rejected_reason IS NOT NULL) = (decision_status = 'REJECTED')
    )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_application
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE INDEX idx$service_application_person_id ON service_application(person_id);
CREATE INDEX idx$service_application_child_id ON service_application(child_id);
CREATE INDEX idx$service_application_service_need_option_id ON service_application(service_need_option_id);
CREATE INDEX idx$service_application_decided_by ON service_application(decided_by);

CREATE UNIQUE INDEX uniq$service_application_child_id_undecided
    ON service_application(child_id) WHERE decision_status IS NULL
