CREATE TYPE daycare_assistance_level AS ENUM (
    'GENERAL_SUPPORT',
    'GENERAL_SUPPORT_WITH_DECISION',
    'INTENSIFIED_SUPPORT',
    'SPECIAL_SUPPORT'
);

CREATE TYPE preschool_assistance_level AS ENUM (
    'INTENSIFIED_SUPPORT',
    'SPECIAL_SUPPORT',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'
);

CREATE TYPE other_assistance_measure_type AS ENUM (
    'TRANSPORT_BENEFIT',
    'ACCULTURATION_SUPPORT'
);

CREATE TABLE assistance_factor (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    capacity_factor numeric NOT NULL,

    CONSTRAINT check$range_valid CHECK (NOT (lower_inf(valid_during) OR upper_inf(valid_during))),
    CONSTRAINT fk$modified_by FOREIGN KEY (modified_by) REFERENCES evaka_user(id) ON DELETE CASCADE,
    CONSTRAINT fk$child FOREIGN KEY (child_id) REFERENCES child(id) ON DELETE CASCADE,
    CONSTRAINT exclude$assistance_factor_no_overlap EXCLUDE USING gist (child_id WITH =, valid_during WITH &&)
);
CREATE INDEX idx$assistance_factor_child ON assistance_factor (child_id);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_factor FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE daycare_assistance (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    level daycare_assistance_level NOT NULL,

    CONSTRAINT check$range_valid CHECK (NOT (lower_inf(valid_during) OR upper_inf(valid_during))),
    CONSTRAINT fk$child FOREIGN KEY (child_id) REFERENCES child(id) ON DELETE CASCADE,
    CONSTRAINT fk$modified_by FOREIGN KEY (modified_by) REFERENCES evaka_user(id) ON DELETE CASCADE,
    CONSTRAINT exclude$daycare_assistance_no_overlap EXCLUDE USING gist (child_id WITH =, valid_during WITH &&)
);
CREATE INDEX idx$daycare_assistance_child ON daycare_assistance (child_id);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_assistance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE preschool_assistance (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    level preschool_assistance_level NOT NULL,

    CONSTRAINT check$range_valid CHECK (NOT (lower_inf(valid_during) OR upper_inf(valid_during))),
    CONSTRAINT fk$child FOREIGN KEY (child_id) REFERENCES child(id) ON DELETE CASCADE,
    CONSTRAINT fk$modified_by FOREIGN KEY (modified_by) REFERENCES evaka_user(id) ON DELETE CASCADE,
    CONSTRAINT exclude$preschool_assistance_no_overlap EXCLUDE USING gist (child_id WITH =, valid_during WITH &&)
);
CREATE INDEX idx$preschool_assistance_child ON preschool_assistance (child_id);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON preschool_assistance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE other_assistance_measure (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    type other_assistance_measure_type NOT NULL,

    CONSTRAINT check$range_valid CHECK (NOT (lower_inf(valid_during) OR upper_inf(valid_during))),
    CONSTRAINT fk$child FOREIGN KEY (child_id) REFERENCES child(id) ON DELETE CASCADE,
    CONSTRAINT fk$modified_by FOREIGN KEY (modified_by) REFERENCES evaka_user(id) ON DELETE CASCADE,
    CONSTRAINT exclude$other_assistance_measure_no_overlap EXCLUDE USING gist (child_id WITH =, type WITH =, valid_during WITH &&)
);
CREATE INDEX idx$other_assistance_measure_child ON other_assistance_measure (child_id);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON other_assistance_measure FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
