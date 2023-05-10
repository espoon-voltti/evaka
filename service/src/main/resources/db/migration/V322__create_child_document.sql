CREATE TABLE child_document (
    id          uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created     timestamp with time zone NOT NULL DEFAULT now(),
    updated     timestamp with time zone NOT NULL DEFAULT now(),
    child_id    uuid                     NOT NULL REFERENCES person(id),
    template_id uuid                     NOT NULL REFERENCES document_template(id),
    published   bool                     NOT NULL DEFAULT false,
    content     jsonb                    NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$child_document_child_id ON child_document(child_id);
