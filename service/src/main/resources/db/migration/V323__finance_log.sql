CREATE TABLE finance_log (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid references evaka_user(id),
    operation text NOT NULL,
    table_name text NOT NULL,
    person_id uuid references person(id) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    data json NOT NULL
);
