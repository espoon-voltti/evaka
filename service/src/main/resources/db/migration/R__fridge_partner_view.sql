DROP VIEW IF EXISTS fridge_partner_view;

CREATE VIEW fridge_partner_view (
    partnership_id,
    start_date,
    end_date,
    conflict,
    person_id,
    first_name,
    last_name,
    partner_person_id,
    partner_first_name,
    partner_last_name
) AS
SELECT
    fp1.partnership_id,
    fp1.start_date,
    fp1.end_date,
    fp1.conflict,
    p1.id AS person_id,
    p1.first_name,
    p1.last_name,
    p2.id AS partner_person_id,
    p2.first_name AS partner_first_name,
    p2.last_name AS partner_last_name
FROM fridge_partner fp1
JOIN fridge_partner fp2
    ON fp1.partnership_id = fp2.partnership_id AND fp1.indx <> fp2.indx
JOIN person p1 on fp1.person_id = p1.id
JOIN person p2 on fp2.person_id = p2.id;
