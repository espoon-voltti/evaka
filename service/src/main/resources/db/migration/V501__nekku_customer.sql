CREATE TABLE nekku_customer
(
    number  text    PRIMARY KEY NOT NULL,
    name    text    NOT NULL
);

ALTER TABLE daycare_group ADD COLUMN nekku_customer_number text;

ALTER TABLE daycare_group
    ADD CONSTRAINT fk$nekku_customer_number
        FOREIGN KEY (nekku_customer_number) REFERENCES nekku_customer (number)
            ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX idx$daycare_group_nekku_customer_number
    ON daycare_group (nekku_customer_number);