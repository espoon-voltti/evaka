CREATE INDEX CONCURRENTLY idx$invoice_sent ON invoice (sent_at) WHERE status = 'SENT';
CREATE INDEX CONCURRENTLY idx$invoice_date ON invoice (invoice_date);
CREATE INDEX CONCURRENTLY idx$invoice_codebtor ON invoice (codebtor) WHERE codebtor IS NOT NULL;
