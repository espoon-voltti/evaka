-- Daycare decisions
INSERT INTO child_document_read (document_id, person_id, read_at)
SELECT
    cd.id,
    p.person_id,
    NOW()
FROM
    child_document cd
JOIN
    assistance_need_decision and_old ON cd.id = and_old.id
JOIN
    (
        SELECT child_id, guardian_id AS person_id FROM guardian
        UNION
        SELECT child_id, parent_id AS person_id FROM foster_parent
    ) p ON cd.child_id = p.child_id
WHERE
    cd.type = 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'
    AND (and_old.unread_guardian_ids IS NULL OR NOT (p.person_id = ANY(and_old.unread_guardian_ids)))
ON CONFLICT (document_id, person_id) DO NOTHING;

-- Preschool decisions
INSERT INTO child_document_read (document_id, person_id, read_at)
SELECT
    cd.id,
    p.person_id,
    NOW()
FROM
    child_document cd
JOIN
    assistance_need_preschool_decision anpd_old ON cd.id = anpd_old.id
JOIN
    (
        SELECT child_id, guardian_id AS person_id FROM guardian
        UNION
        SELECT child_id, parent_id AS person_id FROM foster_parent
    ) p ON cd.child_id = p.child_id
WHERE
    cd.type = 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'
    AND (anpd_old.unread_guardian_ids IS NULL OR NOT (p.person_id = ANY(anpd_old.unread_guardian_ids)))
ON CONFLICT (document_id, person_id) DO NOTHING;
