CREATE TYPE curriculum_language AS ENUM ('FI', 'SV');

CREATE TABLE curriculum_template(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    valid daterange NOT NULL,
    language curriculum_language NOT NULL,
    name text NOT NULL,
    content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON curriculum_template FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE curriculum_document(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL REFERENCES child(id) ON DELETE RESTRICT,
    basics jsonb NOT NULL,
    template_id uuid NOT NULL REFERENCES curriculum_template(id),
    modified_at timestamp with time zone NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON curriculum_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$curriculum_document_child_id ON curriculum_document(child_id);
CREATE INDEX idx$curriculum_document_template_id ON curriculum_document(template_id);

CREATE TABLE curriculum_content(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    document_id uuid NOT NULL REFERENCES curriculum_document(id),
    published_at timestamp with time zone DEFAULT NULL,
    master bool GENERATED ALWAYS AS (published_at IS NULL) STORED,
    content jsonb NOT NULL,
    authors_content jsonb NOT NULL,
    curriculum_discussion_content jsonb NOT NULL,
    evaluation_discussion_content jsonb NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON curriculum_content FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$curriculum_content_document_id_published_at ON curriculum_content(document_id, published_at);
CREATE UNIQUE INDEX uniq$curriculum_content_document_id_master ON curriculum_content(document_id) WHERE master = TRUE;

CREATE TYPE curriculum_document_event_type AS ENUM (
    'PUBLISHED',
    'MOVED_TO_READY',
    'RETURNED_TO_READY',
    'MOVED_TO_REVIEWED',
    'RETURNED_TO_REVIEWED',
    'MOVED_TO_CLOSED'
);

CREATE TABLE curriculum_document_event(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    curriculum_document_id uuid NOT NULL REFERENCES curriculum_document(id),
    employee_id uuid NOT NULL REFERENCES employee(id),
    event_type curriculum_document_event_type NOT NULL
);

CREATE INDEX idx$curriculum_document_event_document_id ON curriculum_document_event(curriculum_document_id);
