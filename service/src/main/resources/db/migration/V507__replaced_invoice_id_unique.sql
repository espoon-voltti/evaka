DROP INDEX idx$invoice_replaced_invoice_id;
CREATE UNIQUE INDEX uniq$invoice_replaced_invoice_id ON invoice (replaced_invoice_id) WHERE replaced_invoice_id IS NOT NULL;
