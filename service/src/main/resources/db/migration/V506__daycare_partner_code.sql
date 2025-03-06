ALTER TABLE daycare ADD COLUMN partner_code TEXT NOT NULL DEFAULT '';
ALTER TABLE payment ADD COLUMN unit_partner_code TEXT;
