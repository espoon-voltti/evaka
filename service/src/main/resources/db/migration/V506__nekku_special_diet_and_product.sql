CREATE TABLE nekku_special_diet
(
    id          text    PRIMARY KEY NOT NULL,
    name        text    NOT NULL,
    type        text    NOT NULL,
    weight      integer,
    field_id     text
    key         text,
    value       text
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
