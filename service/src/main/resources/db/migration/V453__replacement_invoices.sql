ALTER TYPE invoice_status RENAME TO invoice_status_old;
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'WAITING_FOR_SENDING', 'SENT', 'DRAFT_REPLACEMENT', 'REPLACED');
ALTER TABLE invoice ADD COLUMN status_new invoice_status;
UPDATE invoice SET status_new = (CASE status WHEN 'CANCELED' THEN NULL ELSE status::text END)::invoice_status;
ALTER TABLE invoice DROP COLUMN status;
ALTER TABLE invoice RENAME COLUMN status_new TO status;
DROP TYPE invoice_status_old;

ALTER TABLE invoice ADD COLUMN revision_number integer NOT NULL DEFAULT 0;
ALTER TABLE invoice ALTER COLUMN revision_number DROP DEFAULT;
