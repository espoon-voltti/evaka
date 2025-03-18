CREATE TYPE public.nekku_product_meal_time AS ENUM ('BREAKFAST', 'LUNCH', 'SNACK', 'DINNER', 'SUPPER');

CREATE TYPE public.nekku_product_meal_type AS ENUM ('VEGAN', 'VEGETABLE');

CREATE TABLE nekku_product
(
    sku             text    PRIMARY KEY NOT NULL,
    name            text    NOT NULL,
    options_id      text,
    unit_size       text,
    meal_time       nekku_product_meal_time[],
    meal_type       text
);