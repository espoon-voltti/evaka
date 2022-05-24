INSERT INTO evaka_user (id, type, citizen_id, name)
SELECT id, 'CITIZEN', id, first_name || ' ' || last_name
FROM person
WHERE id IN (
    SELECT guardian_id FROM application
)
ON CONFLICT DO NOTHING;
