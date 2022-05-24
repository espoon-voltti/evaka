ALTER TABLE varda_decision ADD COLUMN should_be_deleted boolean;
ALTER TABLE varda_decision ADD COLUMN deleted timestamp;

ALTER TABLE varda_placement ADD COLUMN should_be_deleted boolean;
ALTER TABLE varda_placement ADD COLUMN deleted timestamp;

