ALTER TABLE varda_fee_data
    ADD COLUMN varda_decision_id uuid REFERENCES varda_decision (id),
    DROP COLUMN evaka_placement_id,
    ADD UNIQUE (evaka_fee_decision_id, varda_decision_id),
    ADD COLUMN varda_child_id uuid REFERENCES varda_child (id);

ALTER TABLE varda_fee_data RENAME varda_fee_data_id TO varda_id;
