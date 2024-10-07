ALTER TABLE invoice_correction ADD COLUMN target_month date;

WITH applied_corrections AS (
    SELECT ic.id AS original_correction_id, ext.uuid_generate_v1mc() AS new_correction_id, ir.id AS invoice_row_id, ic.created, ic.head_of_family_id, ic.child_id, ic.unit_id, ic.product, ic.period, ir.amount, ir.unit_price, ic.description, ic.note, i.period_start AS target_month
    FROM invoice_correction ic
    JOIN invoice_row ir ON ir.correction_id = ic.id
    JOIN invoice i ON ir.invoice_id = i.id
), inserted_corrections AS (
    INSERT INTO invoice_correction (id, created, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note, applied_completely, target_month)
    SELECT ac.new_correction_id, ac.created, ac.head_of_family_id, ac.child_id, ac.unit_id, ac.product, ac.period, ac.amount, ac.unit_price, ac.description, ac.note, FALSE, ac.target_month
    FROM applied_corrections ac
), updated_invoice_rows AS (
    UPDATE invoice_row ir
    SET correction_id = ac.new_correction_id
    FROM applied_corrections ac
    WHERE ir.id = ac.invoice_row_id
    RETURNING ir.*
), remaining_corrections AS (
    SELECT ic.id, ic.amount * ic.unit_price - SUM(coalesce(ir.amount, 0) * coalesce(ir.unit_price, 0)) AS remaining_correction
    FROM invoice_correction ic
    LEFT JOIN applied_corrections ac ON ac.original_correction_id = ic.id
    LEFT JOIN updated_invoice_rows ir ON ir.correction_id = ac.new_correction_id
    WHERE ic.target_month IS NULL
    GROUP BY ic.id
), first_uninvoiced_month AS (
    -- Invoices of month N are sent in month N+1, so the invoice date of the last sent invoice is the first uninvoiced month.
    SELECT date_trunc('month', MAX(invoice_date)) AS month
    FROM invoice
    WHERE status = 'SENT'
)
UPDATE invoice_correction ic
SET
    amount = CASE WHEN rc.remaining_correction % ic.unit_price = 0 THEN rc.remaining_correction / ic.unit_price ELSE ic.amount END,
    unit_price = CASE WHEN rc.remaining_correction % ic.unit_price = 0 THEN ic.unit_price ELSE rc.remaining_correction / ic.amount END,
    target_month = coalesce((SELECT month FROM first_uninvoiced_month), date_trunc('month', now()) + interval '1 month'),
    applied_completely = FALSE
FROM remaining_corrections rc
WHERE ic.id = rc.id AND rc.remaining_correction != 0;

DELETE FROM invoice_correction WHERE target_month IS NULL AND applied_completely;

ALTER TABLE invoice_correction
    DROP COLUMN applied_completely,
    ALTER COLUMN target_month SET NOT NULL,
    ADD CONSTRAINT check$invoice_correction_target_month CHECK (EXTRACT(DAY FROM target_month) = 1);

CREATE INDEX idx$invoice_correction_target_month_head_of_family ON invoice_correction (target_month, head_of_family_id);
