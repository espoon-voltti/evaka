ALTER TABLE invoice_correction
    ADD CONSTRAINT "check$period_finite" CHECK (NOT (lower_inf(period) OR upper_inf(period)));
