ALTER TABLE fee_decision ADD CONSTRAINT check$head_of_family_is_not_partner CHECK (partner_id IS NULL OR head_of_family_id != partner_id);
ALTER TABLE voucher_value_decision ADD CONSTRAINT check$head_of_family_is_not_partner CHECK (partner_id IS NULL OR head_of_family_id != partner_id);
