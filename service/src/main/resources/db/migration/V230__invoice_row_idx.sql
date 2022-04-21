LOCK TABLE invoice_row;

ALTER TABLE invoice_row ADD COLUMN idx smallint;

-- Loop invoices with seqscan disabled to try to make sure we use the (invoice_id) index, which contains
-- the row order we want
DO $$
DECLARE
    current_invoice uuid;
BEGIN
    SET enable_seqscan = FALSE;
    FOR current_invoice IN SELECT id FROM invoice LOOP
        UPDATE invoice_row
        SET idx = f.idx
        FROM (
          SELECT id, (row_number() OVER () - 1) AS idx
          FROM invoice_row
          WHERE invoice_id = current_invoice
        ) f
        WHERE invoice_row.id = f.id;
    END LOOP;
END $$ LANGUAGE plpgsql;

ALTER TABLE invoice_row ALTER COLUMN idx SET NOT NULL;
ALTER TABLE invoice_row ADD CONSTRAINT uniq$invoice_row_invoice_idx UNIQUE (invoice_id, idx);

DROP INDEX invoice_row_invoice_id_idx;

