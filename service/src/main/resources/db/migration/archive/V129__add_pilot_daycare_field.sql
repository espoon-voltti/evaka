CREATE TYPE pilot_feature AS ENUM (
    'MESSAGING'
    );

ALTER TABLE daycare ADD COLUMN enabled_pilot_features pilot_feature[] NOT NULL DEFAULT '{}';
