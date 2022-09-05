ALTER TABLE curriculum_document ADD COLUMN permission_to_share_required BOOLEAN;
UPDATE curriculum_document SET permission_to_share_required = TRUE;
ALTER TABLE curriculum_document ALTER COLUMN permission_to_share_required SET NOT NULL;
