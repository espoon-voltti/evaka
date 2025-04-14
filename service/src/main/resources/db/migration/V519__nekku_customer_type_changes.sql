ALTER TABLE nekku_product
    DROP COLUMN unit_size;

ALTER TABLE nekku_customer
    DROP COLUMN unit_size;

ALTER TABLE nekku_product
    ADD customer_types TEXT[];
