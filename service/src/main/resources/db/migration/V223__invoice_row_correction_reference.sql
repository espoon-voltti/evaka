ALTER TABLE invoice_row ADD COLUMN correction_id uuid REFERENCES invoice_correction(id);

CREATE INDEX idx$invoice_row_correction_id ON invoice_row (correction_id);
