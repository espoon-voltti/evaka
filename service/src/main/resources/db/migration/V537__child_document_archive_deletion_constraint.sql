-- Prevent deletion of child documents that should be archived externally but haven't been archived yet
CREATE FUNCTION trigger_prevent_unarchived_document_deletion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    -- Check if the document template requires external archiving,
    -- the document hasn't been archived yet, and the status is not DRAFT
    IF EXISTS (
        SELECT 1 
        FROM document_template dt 
        WHERE dt.id = OLD.template_id 
        AND dt.archive_externally = true 
        AND OLD.archived_at IS NULL
        AND OLD.status != 'DRAFT'
    ) THEN
        RAISE EXCEPTION 'Cannot delete child document (id: %) - document must be archived before deletion (template requires external archiving and document status is %)', OLD.id, OLD.status;
    END IF;
    RETURN OLD;
END
$$;

-- Create the trigger that fires before deletion
CREATE TRIGGER prevent_unarchived_document_deletion 
    BEFORE DELETE ON child_document
    FOR EACH ROW 
    EXECUTE FUNCTION trigger_prevent_unarchived_document_deletion();