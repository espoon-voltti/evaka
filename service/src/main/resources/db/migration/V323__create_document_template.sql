CREATE TYPE document_template_type AS ENUM (
    'PEDAGOGICAL_REPORT',
    'PEDAGOGICAL_ASSESSMENT'
);

CREATE TABLE document_template (
    id          uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created     timestamp with time zone NOT NULL DEFAULT now(),
    updated     timestamp with time zone NOT NULL DEFAULT now(),
    name        TEXT                     NOT NULL,
    type        document_template_type   NOT NULL,
    validity    daterange                NOT NULL CONSTRAINT validity_start_not_null CHECK ( lower(validity) IS NOT NULL ),
    published   bool                     NOT NULL DEFAULT false,
    content     jsonb                    NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON document_template FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
