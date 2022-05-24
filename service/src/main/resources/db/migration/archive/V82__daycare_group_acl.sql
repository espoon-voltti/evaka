CREATE TABLE daycare_group_acl (
    daycare_group_id uuid NOT NULL REFERENCES daycare_group ON DELETE CASCADE,
    employee_id uuid NOT NULL REFERENCES employee ON DELETE CASCADE,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_group_acl FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$daycare_group_acl_employee_group ON daycare_group_acl (employee_id, daycare_group_id);
CREATE INDEX idx$daycare_group_acl_group_employee ON daycare_group_acl (daycare_group_id, employee_id);
