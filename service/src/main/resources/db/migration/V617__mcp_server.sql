CREATE TABLE mcp_client (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    client_name text NOT NULL,
    client_uri text,
    software_id text,
    software_version text,
    redirect_uris text[] NOT NULL,
    token_endpoint_auth_method text NOT NULL,
    client_secret_hash text,
    registration_ip text
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON mcp_client
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE TABLE mcp_authorization (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    client_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    scope text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    -- OAuth authorization code (SHA-256 hash), exchanged once for the access token
    code_hash text,
    code_expires_at timestamp with time zone,
    code_challenge text NOT NULL,
    redirect_uri text NOT NULL,
    resource text,
    -- Bearer access token (SHA-256 hash)
    access_token_hash text,
    token_issued_at timestamp with time zone,
    last_used_at timestamp with time zone
);

ALTER TABLE mcp_authorization
    ADD CONSTRAINT fk$client FOREIGN KEY (client_id) REFERENCES mcp_client (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk$employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$mcp_authorization_code_hash UNIQUE (code_hash),
    ADD CONSTRAINT uniq$mcp_authorization_access_token_hash UNIQUE (access_token_hash);

CREATE INDEX idx$mcp_authorization_client_id ON mcp_authorization (client_id);
CREATE INDEX idx$mcp_authorization_employee_id ON mcp_authorization (employee_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON mcp_authorization
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE TABLE mcp_test_data_batch (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL,
    authorization_id uuid,
    name text NOT NULL,
    description text NOT NULL DEFAULT ''
);

ALTER TABLE mcp_test_data_batch
    ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user (id),
    ADD CONSTRAINT fk$authorization FOREIGN KEY (authorization_id) REFERENCES mcp_authorization (id) ON DELETE SET NULL,
    ADD CONSTRAINT uniq$mcp_test_data_batch_created_by_name UNIQUE (created_by, name);

CREATE INDEX idx$mcp_test_data_batch_authorization_id ON mcp_test_data_batch (authorization_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON mcp_test_data_batch
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE TABLE mcp_test_data_entity (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    batch_id uuid NOT NULL,
    table_name text NOT NULL,
    entity_id uuid NOT NULL,
    description text NOT NULL DEFAULT ''
);

ALTER TABLE mcp_test_data_entity
    ADD CONSTRAINT fk$batch FOREIGN KEY (batch_id) REFERENCES mcp_test_data_batch (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$mcp_test_data_entity UNIQUE (table_name, entity_id);

CREATE INDEX idx$mcp_test_data_entity_batch_id ON mcp_test_data_entity (batch_id);

CREATE TABLE mcp_upload (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    authorization_id uuid NOT NULL,
    token_hash text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone
);

ALTER TABLE mcp_upload
    ADD CONSTRAINT fk$authorization FOREIGN KEY (authorization_id) REFERENCES mcp_authorization (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$mcp_upload_token_hash UNIQUE (token_hash);

CREATE INDEX idx$mcp_upload_authorization_id ON mcp_upload (authorization_id);
