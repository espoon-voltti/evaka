CREATE TABLE nekku_product
(
    sku             text    PRIMARY KEY NOT NULL,
    name            text    NOT NULL,
    options_id      text,
    unit_size       text,
    meal_time       text,
    meal_type       text
);