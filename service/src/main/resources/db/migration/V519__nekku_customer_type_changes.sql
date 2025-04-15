ALTER TABLE nekku_product
    DROP COLUMN unit_size;

ALTER TABLE nekku_customer
    DROP COLUMN unit_size;

ALTER TABLE nekku_product
    ADD customer_types TEXT[];


CREATE TYPE weekday AS (
    description text
    );

CREATE TYPE customer_type AS (
    weekdays weekday[],
    type text
    );

ALTER TABLE nekku_customer
    ADD COLUMN customer_type customer_type;

