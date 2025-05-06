

CREATE TABLE nekku_orders_report (
        delivery_date DATE,
        daycare_id UUID,
        customer_group_id UUID,
        meal_sku TEXT,
        total_quantity INT,
        meal_time       nekku_product_meal_time[],
        meal_type       nekku_product_meal_type,
        meals_by_special_diet TEXT
);
