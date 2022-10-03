CREATE TYPE voucher_value_decision_difference AS ENUM (
    'GUARDIANS',
    'INCOME',
    'FAMILY_SIZE',
    'PLACEMENT',
    'SERVICE_NEED',
    'SIBLING_DISCOUNT',
    'CO_PAYMENT',
    'FEE_ALTERATIONS',
    'FINAL_CO_PAYMENT',
    'BASE_VALUE',
    'VOUCHER_VALUE'
);
ALTER TABLE voucher_value_decision ADD COLUMN difference voucher_value_decision_difference[];
UPDATE voucher_value_decision SET difference = '{}';
ALTER TABLE voucher_value_decision ALTER COLUMN difference SET NOT NULL;
