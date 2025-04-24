
CREATE TABLE nekku_order (
    order_id UUID PRIMARY KEY,
    delivery_date DATE NOT NULL,
    customer_number TEXT NOT NULL,
    group_id TEXT NOT NULL,
    description TEXT
);


CREATE TABLE nekku_item (
    item_id UUID PRIMARY KEY,
    nekku_order_id UUID NOT NULL,
    sku TEXT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (nekku_order_id) REFERENCES nekku_order (order_id)
);



CREATE TABLE nekku_item_product_options (
    nekku_item_id UUID NOT NULL,
    field_id TEXT NOT NULL,
    value TEXT NOT NULL,
    FOREIGN KEY (nekku_item_id) REFERENCES nekku_item (item_id)
);

