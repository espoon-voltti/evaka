ALTER TABLE varda_fee_data ADD COLUMN should_be_deleted boolean default false;
ALTER TABLE varda_fee_data ADD COLUMN deleted_at timestamp;

ALTER TABLE varda_placement RENAME COLUMN deleted TO deleted_at;
ALTER TABLE varda_placement ALTER COLUMN should_be_deleted SET DEFAULT false;

ALTER TABLE varda_decision RENAME COLUMN deleted TO deleted_at;
ALTER TABLE varda_decision ALTER COLUMN should_be_deleted SET DEFAULT false;

ALTER TABLE varda_fee_data DROP CONSTRAINT varda_fee_data_evaka_fee_decision_id_evaka_placement_id_key;
ALTER TABLE varda_fee_data
    ADD CONSTRAINT varda_fee_data_evaka_fee_decision_id_evaka_placement_id_key UNIQUE (evaka_fee_decision_id, evaka_placement_id, should_be_deleted);
