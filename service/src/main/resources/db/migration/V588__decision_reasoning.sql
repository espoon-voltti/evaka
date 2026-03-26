CREATE TYPE decision_reasoning_collection_type AS ENUM ('DAYCARE', 'PRESCHOOL');

CREATE TABLE decision_reasoning_generic (
    id uuid DEFAULT ext.uuid_generate_v1mc() PRIMARY KEY,
    collection_type decision_reasoning_collection_type NOT NULL,
    valid_from date NOT NULL,
    text_fi text NOT NULL,
    text_sv text NOT NULL,
    ready boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON decision_reasoning_generic
    FOR EACH ROW EXECUTE FUNCTION trigger_refresh_updated_at();

CREATE TABLE decision_reasoning_individual (
    id uuid DEFAULT ext.uuid_generate_v1mc() PRIMARY KEY,
    collection_type decision_reasoning_collection_type NOT NULL,
    title_fi text NOT NULL,
    title_sv text NOT NULL,
    text_fi text NOT NULL,
    text_sv text NOT NULL,
    removed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON decision_reasoning_individual
    FOR EACH ROW EXECUTE FUNCTION trigger_refresh_updated_at();
