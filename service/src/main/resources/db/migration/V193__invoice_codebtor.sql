ALTER TABLE invoice ADD COLUMN codebtor uuid REFERENCES person(id);
