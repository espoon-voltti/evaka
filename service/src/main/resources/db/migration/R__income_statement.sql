DROP TABLE IF EXISTS income_statement;
DROP TYPE IF EXISTS income_statement_type;
DROP TYPE IF EXISTS income_source;
DROP TYPE IF EXISTS other_income_type;

-- Used earlier
DROP TYPE IF EXISTS gross_income_source;
DROP TYPE IF EXISTS limited_company_income_source;
DROP TYPE IF EXISTS company_type;
DROP TYPE IF EXISTS self_employed_income_source;

CREATE TYPE income_statement_type AS ENUM (
    'HIGHEST_FEE',
    'GROSS',
    'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION',
    'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS',
    'ENTREPRENEUR_LIMITED_COMPANY',
    'ENTREPRENEUR_PARTNERSHIP'
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

CREATE TABLE income_statement (
  id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
  person_id uuid NOT NULL CONSTRAINT fk$person REFERENCES person (id) ON DELETE CASCADE,
  start_date date NOT NULL,
  type income_statement_type NOT NULL,
  income_source income_source,
  other_income other_income_type[],
  self_employed_estimated_monthly_income int,
  self_employed_income_start_date date,
  self_employed_income_end_date date,
  created timestamp with time zone NOT NULL DEFAULT now(),
  updated timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON income_statement FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
