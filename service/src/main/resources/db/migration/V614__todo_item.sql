CREATE TABLE todo_item (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL,
    employee_id uuid NOT NULL REFERENCES employee (id),
    description text NOT NULL,
    deadline date
);

CREATE INDEX fk$todo_item_employee_id ON todo_item (employee_id);
