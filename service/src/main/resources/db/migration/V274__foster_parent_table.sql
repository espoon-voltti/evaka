CREATE TABLE foster_parent (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    child_id uuid NOT NULL REFERENCES person (id),
    parent_id uuid NOT NULL REFERENCES person (id),
    valid_during daterange NOT NULL,

    CONSTRAINT exclude$no_overlaps EXCLUDE USING gist (child_id WITH =, parent_id WITH =, valid_during WITH &&)
);

CREATE INDEX idx$foster_parent_child_id ON foster_parent (child_id);
CREATE INDEX idx$foster_parent_parent_id ON foster_parent (parent_id);
