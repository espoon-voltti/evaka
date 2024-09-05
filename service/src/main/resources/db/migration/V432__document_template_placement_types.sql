ALTER TABLE document_template ADD COLUMN placement_types placement_type[];

-- todo: set defaults

ALTER TABLE document_template ALTER COLUMN placement_types SET NOT NULL;

ALTER TYPE document_template_type ADD VALUE 'OTHER';
