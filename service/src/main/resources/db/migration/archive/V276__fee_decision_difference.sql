CREATE TYPE fee_decision_difference AS ENUM (
    'GUARDIANS',
    'CHILDREN',
    'INCOME',
    'PLACEMENT',
    'SERVICE_NEED',
    'SIBLING_DISCOUNT',
    'FEE_ALTERATIONS',
    'FAMILY_SIZE',
    'FEE_THRESHOLDS'
);
ALTER TABLE fee_decision ADD COLUMN difference fee_decision_difference[];
UPDATE fee_decision SET difference = '{}';
ALTER TABLE fee_decision ALTER COLUMN difference SET NOT NULL;
