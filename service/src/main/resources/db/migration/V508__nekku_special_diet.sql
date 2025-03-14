CREATE TYPE public.nekku_special_diet_type AS ENUM ('TEXT', 'CHECKBOXLIST');

CREATE TABLE nekku_special_diet (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE nekku_special_diet_field (
    id TEXT PRIMARY KEY ,
    name TEXT NOT NULL,
    type nekku_special_diet_type NOT NULL,
    diet_id TEXT REFERENCES nekku_special_diet(id) ON DELETE CASCADE
);

CREATE TABLE nekku_special_diet_option (
    weight INT NOT NULL,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    field_id TEXT REFERENCES nekku_special_diet_field(id) ON DELETE CASCADE
);

ALTER TABLE nekku_special_diet_option ADD CONSTRAINT unique_field_value UNIQUE (field_id, value);

CREATE INDEX idx$nekku_special_diet_option_field
    ON nekku_special_diet_option (field_id);

CREATE INDEX idx$nekku_special_diet_field_diet
    ON nekku_special_diet_field (diet_id);