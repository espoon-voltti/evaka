ALTER TABLE nekku_orders_report
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();

UPDATE nekku_orders_report
    SET created_at = (delivery_date + time '08:00')::timestamptz;