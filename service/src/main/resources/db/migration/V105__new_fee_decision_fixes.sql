ALTER TABLE varda_fee_data DROP CONSTRAINT fk$evaka_fee_decision;

ALTER TABLE new_fee_decision_child
    ALTER COLUMN service_need_description_fi SET NOT NULL,
    ALTER COLUMN service_need_description_sv SET NOT NULL;
