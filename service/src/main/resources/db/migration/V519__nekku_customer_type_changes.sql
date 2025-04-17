ALTER TABLE nekku_product
    DROP COLUMN unit_size;

ALTER TABLE nekku_customer
    DROP COLUMN unit_size;

ALTER TABLE nekku_product
    ADD customer_types TEXT[];

CREATE TYPE public.nekku_customer_weekday AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'WEEKDAYHOLIDAY');

CREATE TABLE nekku_customer_type (
    weekdays nekku_customer_weekday[],
    type text,
    customer_number TEXT REFERENCES nekku_customer(number) ON DELETE CASCADE
    );


ALTER TABLE nekku_customer_type
    ADD CONSTRAINT uniq$unique_customer_number UNIQUE (customer_number);

CREATE INDEX idx$nekku_customer_type_number
    ON nekku_customer_type (customer_number);