CREATE TABLE acl_history (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    employee_id uuid NOT NULL
        CONSTRAINT acl_history_employee_id REFERENCES employee,
    daycare_id uuid
        CONSTRAINT fk$acl_history_daycare_id REFERENCES daycare,
    role user_role NOT NULL
        CONSTRAINT chk$valid_role
            CHECK ((daycare_id IS NOT NULL) = (role = ANY (ARRAY [
                'UNIT_SUPERVISOR'::user_role,
                'STAFF'::user_role,
                'SPECIAL_EDUCATION_TEACHER'::user_role,
                'EARLY_CHILDHOOD_EDUCATION_SECRETARY'::user_role
            ]))),
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone
        CONSTRAINT chk$start_before_end
            CHECK ( valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON acl_history
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX fk$acl_history_employee_id ON acl_history(employee_id);
CREATE INDEX fk$acl_history_daycare_id ON acl_history(daycare_id);

ALTER TABLE acl_history
    ADD CONSTRAINT check$no_overlap EXCLUDE USING gist (
        employee_id WITH =,
        daycare_id WITH =,
        role WITH =,
        tstzrange(valid_from, valid_to) WITH &&
    );

INSERT INTO acl_history (employee_id, daycare_id, role, valid_from, valid_to)
SELECT acl.employee_id, acl.daycare_id, acl.role, now(), NULL
FROM daycare_acl acl;

INSERT INTO acl_history (employee_id, daycare_id, role, valid_from, valid_to)
SELECT e.id, NULL, unnest(e.roles), now(), NULL
FROM employee e;
