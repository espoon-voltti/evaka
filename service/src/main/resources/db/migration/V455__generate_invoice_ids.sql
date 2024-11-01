UPDATE invoice_row SET description = '' WHERE description IS NULL;

ALTER TABLE invoice ALTER COLUMN id SET DEFAULT ext.uuid_generate_v1mc();
ALTER TABLE invoice_row
    ALTER COLUMN id SET DEFAULT ext.uuid_generate_v1mc(),
    ALTER COLUMN description SET NOT NULL;
