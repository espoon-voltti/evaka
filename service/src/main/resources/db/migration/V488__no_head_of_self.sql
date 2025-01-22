DELETE FROM fridge_child WHERE child_id = head_of_child;

ALTER TABLE fridge_child ADD CONSTRAINT no_head_of_self CHECK (child_id != head_of_child);
