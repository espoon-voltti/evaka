ALTER TABLE income_statement
    ADD COLUMN company_name text NOT NULL DEFAULT '',
    ADD COLUMN business_id text NOT NULL DEFAULT '';
ALTER TABLE income_statement
    ALTER COLUMN company_name DROP DEFAULT,
    ALTER COLUMN business_id DROP DEFAULT;
