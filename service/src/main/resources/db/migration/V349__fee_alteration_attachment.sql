ALTER TABLE attachment ADD COLUMN fee_alteration_id uuid REFERENCES fee_alteration (id);
CREATE INDEX idx$attachment_fee_alteration ON attachment (fee_alteration_id);