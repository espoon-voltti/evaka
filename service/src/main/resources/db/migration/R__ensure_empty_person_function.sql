DROP FUNCTION IF EXISTS ensure_empty_person(uuid);

CREATE FUNCTION ensure_empty_person(uuid) RETURNS void AS $$
BEGIN
    IF (
        (SELECT count(*) FROM absence WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM application WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM application WHERE guardian_id = $1) > 0 OR
        (SELECT count(*) FROM assistance_need WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM assistance_action WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM backup_care WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM fee_alteration WHERE person_id = $1) > 0 OR
        (SELECT count(*) FROM fee_decision WHERE head_of_family = $1) > 0 OR
        (SELECT count(*) FROM fee_decision WHERE partner = $1) > 0 OR
        (SELECT count(*) FROM fee_decision_part WHERE child = $1) > 0 OR
        (SELECT count(*) FROM fridge_child WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM fridge_child WHERE head_of_child = $1) > 0 OR
        (SELECT count(*) FROM fridge_partner WHERE person_id = $1) > 0 OR
        (SELECT count(*) FROM income WHERE person_id = $1) > 0 OR
        (SELECT count(*) FROM invoice WHERE head_of_family = $1) > 0 OR
        (SELECT count(*) FROM invoice_row WHERE child = $1) > 0 OR
        (SELECT count(*) FROM placement WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM service_need WHERE child_id = $1) > 0 OR
        (SELECT count(*) FROM message WHERE sender_id = (SELECT id FROM message_account WHERE person_id = $1)) > 0 OR
        (SELECT count(*) FROM message_content WHERE author_id = (SELECT id FROM message_account WHERE person_id = $1)) > 0 OR
        (SELECT count(*) FROM message_recipients WHERE recipient_id = (SELECT id FROM message_account WHERE person_id = $1)) > 0
    ) THEN RAISE EXCEPTION 'Person still has references.';
    END IF;
END;
$$ LANGUAGE plpgsql;
