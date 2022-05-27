CREATE TABLE family_contact (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_id uuid NOT NULL REFERENCES person(id),
    contact_person_id uuid NOT NULL REFERENCES person(id),
    priority int NOT NULL,
    CONSTRAINT unique_child_contact_person_pair UNIQUE (child_id, contact_person_id) DEFERRABLE,
    CONSTRAINT unique_child_priority_pair UNIQUE (child_id, priority) DEFERRABLE
);
