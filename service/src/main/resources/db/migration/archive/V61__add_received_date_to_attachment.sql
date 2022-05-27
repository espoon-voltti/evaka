ALTER TABLE attachment
    ADD COLUMN received_at timestamp with time zone DEFAULT now();

UPDATE attachment SET received_at = updated;

ALTER TABLE attachment
    ALTER COLUMN received_at SET NOT NULL;
