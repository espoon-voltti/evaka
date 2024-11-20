CREATE TYPE invoice_replacement_reason AS ENUM (
    'SERVICE_NEED',
    'ABSENCE',
    'INCOME',
    'FAMILY_SIZE',
    'RELIEF_RETROACTIVE',
    'OTHER'
);

ALTER TABLE invoice
    ADD COLUMN replacement_reason invoice_replacement_reason,
    ADD COLUMN replacement_notes text;
