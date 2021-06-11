DROP TABLE IF EXISTS vasu_document;
DROP TABLE IF EXISTS vasu_content;
DROP TABLE IF EXISTS vasu_template;

CREATE TABLE vasu_template(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    valid daterange NOT NULL,
    name text NOT NULL,
    content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_template FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE vasu_content(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_content FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE vasu_document(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL REFERENCES child(id),
    template_id uuid NOT NULL REFERENCES vasu_template(id),
    content_id uuid NOT NULL REFERENCES vasu_content(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX fk$vasu_document_child_id ON vasu_document(child_id);
CREATE INDEX fk$vasu_document_template_id ON vasu_document(template_id);
CREATE INDEX fk$vasu_document_content_id ON vasu_document(content_id);
