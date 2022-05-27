CREATE TABLE pedagogical_document_read (
    pedagogical_document_id uuid REFERENCES pedagogical_document(id),
    person_id uuid REFERENCES person(id),
    read_at timestamp
);

CREATE UNIQUE INDEX uniq$pedagogical_document_read_by_person ON pedagogical_document_read(pedagogical_document_id, person_id);
