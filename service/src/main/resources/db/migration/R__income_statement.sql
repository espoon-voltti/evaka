ALTER TABLE attachment
    DROP CONSTRAINT IF EXISTS attachment_income_statement_id_fkey,
    DROP CONSTRAINT IF EXISTS created_for_fk,
    DROP COLUMN IF EXISTS income_statement_id;

DROP TABLE IF EXISTS income_statement;
DROP TYPE IF EXISTS income_statement_type;
DROP TYPE IF EXISTS income_source;
DROP TYPE IF EXISTS other_income_type;
DROP TYPE IF EXISTS self_employed_type;

-- Used earlier
DROP TYPE IF EXISTS gross_income_source;
DROP TYPE IF EXISTS limited_company_income_source;
DROP TYPE IF EXISTS company_type;
DROP TYPE IF EXISTS self_employed_income_source;

CREATE TYPE income_statement_type AS ENUM (
    'HIGHEST_FEE',
    'INCOME'
);

CREATE TYPE income_source AS ENUM (
    'INCOMES_REGISTER',
    'ATTACHMENTS'
);

CREATE TYPE other_income_type AS ENUM (
    'SHIFT_WORK_ADD_ON',
    'PERKS',
    'SECONDARY_INCOME',
    'PENSION',
    'UNEMPLOYMENT_BENEFITS',
    'SICKNESS_ALLOWANCE',
    'PARENTAL_ALLOWANCE',
    'HOME_CARE_ALLOWANCE',
    'ALIMONY',
    'OTHER_INCOME'
);

CREATE TYPE self_employed_type AS ENUM (
    'ATTACHMENTS',
    'ESTIMATION'
);

CREATE TABLE income_statement (
  id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
  person_id uuid NOT NULL CONSTRAINT fk$person REFERENCES person (id) ON DELETE CASCADE,
  start_date date NOT NULL,
  type income_statement_type NOT NULL,
  gross_income_source income_source,
  gross_other_income other_income_type[],
  self_employed_type self_employed_type,
  self_employed_estimated_monthly_income int,
  self_employed_income_start_date date,
  self_employed_income_end_date date,
  limited_company_income_source income_source,
  partnership bool,
  startup_grant bool,
  other_info text,
  created timestamp with time zone NOT NULL DEFAULT now(),
  updated timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON income_statement FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE attachment
    ADD COLUMN income_statement_id UUID,
    ADD CONSTRAINT attachment_income_statement_id_fkey FOREIGN KEY (income_statement_id) REFERENCES income_statement (id),
    ADD CONSTRAINT created_for_fk CHECK (num_nonnulls(application_id, income_statement_id) <= 1);
