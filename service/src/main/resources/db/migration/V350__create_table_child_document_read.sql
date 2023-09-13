CREATE TABLE child_document_read (
    document_id uuid NOT NULL REFERENCES child_document(id),
    person_id   uuid NOT NULL REFERENCES person(id),
    read_at     timestamp with time zone NOT NULL DEFAULT now(),

    PRIMARY KEY (document_id, person_id)
);
