DROP TABLE IF EXISTS vasu_document_event CASCADE;
DROP TABLE IF EXISTS vasu_content CASCADE;
DROP TABLE IF EXISTS vasu_document CASCADE;
DROP TABLE IF EXISTS vasu_template CASCADE;
DROP TYPE IF EXISTS vasu_language;
DROP TYPE IF EXISTS vasu_document_event_type;

CREATE TYPE vasu_language AS ENUM ('FI', 'SV');

CREATE TABLE vasu_template(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    valid daterange NOT NULL,
    language vasu_language NOT NULL,
    name text NOT NULL,
    content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_template FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE vasu_document(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL REFERENCES child(id),
    template_id uuid NOT NULL REFERENCES vasu_template(id),
    modified_at timestamp with time zone NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$vasu_document_child_id ON vasu_document(child_id);
CREATE INDEX idx$vasu_document_template_id ON vasu_document(template_id);

CREATE TABLE vasu_content(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    document_id uuid NOT NULL REFERENCES vasu_document(id),
    published_at timestamp with time zone DEFAULT NULL,
    master bool GENERATED ALWAYS AS (published_at IS NULL) STORED,
    content jsonb NOT NULL,
    authors_content jsonb NOT NULL,
    vasu_discussion_content jsonb NOT NULL,
    evaluation_discussion_content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vasu_content FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$vasu_content_document_id_published_at ON vasu_content(document_id, published_at);
CREATE UNIQUE INDEX uniq$vasu_content_document_id_master ON vasu_content(document_id) WHERE master = TRUE;

CREATE TYPE vasu_document_event_type AS ENUM (
    'PUBLISHED',
    'MOVED_TO_READY',
    'RETURNED_TO_READY',
    'MOVED_TO_REVIEWED',
    'RETURNED_TO_REVIEWED',
    'MOVED_TO_CLOSED'
);

CREATE TABLE vasu_document_event(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    vasu_document_id uuid NOT NULL REFERENCES vasu_document(id),
    employee_id uuid NOT NULL REFERENCES employee(id),
    event_type vasu_document_event_type NOT NULL
);

CREATE INDEX idx$vasu_document_event_document_id ON vasu_document_event(vasu_document_id);
