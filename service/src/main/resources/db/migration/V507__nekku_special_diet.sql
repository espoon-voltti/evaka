CREATE TYPE public.nekku_special_diet_type AS ENUM ('TEXT', 'CHECKBOXLIST');

CREATE TABLE nekku_special_diet (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE nekku_special_diets_field (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type nekku_special_diet_type NOT NULL,
    diet_id TEXT REFERENCES nekku_special_diet(id)
);

CREATE TABLE nekku_special_diet_option (
    weight INT NOT NULL,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    field_id TEXT REFERENCES nekku_special_diets_field(id)
);