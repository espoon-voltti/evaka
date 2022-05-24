ALTER TABLE income_statement
    ADD COLUMN gross_other_income_info text NOT NULL DEFAULT '',
    DROP COLUMN gross_income_start_date,
    DROP COLUMN gross_income_end_date;
