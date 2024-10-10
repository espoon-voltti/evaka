ALTER TYPE invoice_status RENAME TO invoice_status_old;
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'WAITING_FOR_SENDING', 'SENT', 'REPLACEMENT_DRAFT', 'REPLACED');
ALTER TABLE invoice ADD COLUMN status_new invoice_status;
UPDATE invoice SET status_new = (CASE status WHEN 'CANCELED' THEN NULL ELSE status::text END)::invoice_status;
ALTER TABLE invoice DROP COLUMN status;
ALTER TABLE invoice RENAME COLUMN status_new TO status;
DROP TYPE invoice_status_old;

ALTER TABLE invoice
    ADD COLUMN revision_number integer NOT NULL DEFAULT 0 CONSTRAINT check$revision_number_non_negative CHECK (revision_number >= 0),
    ADD COLUMN replaced_invoice_id uuid CONSTRAINT fk$replaced_invoice_id REFERENCES invoice(id);

ALTER TABLE invoice ALTER COLUMN revision_number DROP DEFAULT;
CREATE INDEX idx$invoice_replaced_invoice_id ON invoice(replaced_invoice_id);
