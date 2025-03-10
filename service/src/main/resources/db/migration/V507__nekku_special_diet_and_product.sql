CREATE TYPE public.nekku_special_diet_type AS ENUM ('TEXT', 'CHECKBOXLIST');

CREATE TABLE nekku_special_diet (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE nekku_special_diets_field (
    id VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL,
    type nekku_special_diet_type NOT NULL,
    diet_id VARCHAR REFERENCES nekku_special_diet(id)
);

CREATE TABLE nekku_special_diet_option (
    weight INT NOT NULL,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    field_id TEXT REFERENCES nekku_special_diets_field(id)
);

CREATE TABLE nekku_product
(
    sku         text PRIMARY KEY NOT NULL,
    name        text NOT NULL,
    options_id  text,
    unit_size   text NOT NULL,
    meal_time   text, -- this is list of enums
    meal_type   text
);
