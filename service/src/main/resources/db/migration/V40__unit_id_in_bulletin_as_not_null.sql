UPDATE bulletin
SET unit_id = (SELECT dg.daycare_id FROM daycare_group dg WHERE dg.id = bulletin.group_id)
WHERE unit_id IS NULL AND group_id IS NOT NULL;

DELETE FROM bulletin_instance WHERE id IN (SELECT id FROM bulletin WHERE unit_id IS NULL);

DELETE FROM bulletin WHERE unit_id IS NULL;

ALTER TABLE bulletin ALTER COLUMN unit_id SET NOT NULL;
