-- SPDX-FileCopyrightText: 2023-2025 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Usage:
-- - configure p1 dates for correct period
-- - verify p1 type inclusion/exclusion details
-- \copy (select * from palvelukykykysely) TO 'palvelukykykysely.csv' DELIMITER ',' CSV HEADER;

DROP VIEW IF EXISTS palvelukykykysely;

CREATE VIEW palvelukykykysely AS
SELECT 'https://varhaiskasvatus.tampere.fi/employee/child-information/' || child.id AS child_url,
       child.last_name                                                              AS child_last_name,
       child.first_name                                                             AS child_first_name,
       d1.name                                                                      AS placement_unit,
       p1.type                                                                      AS placement_type,
       p1.start_date                                                                AS placement_start_date,
       'https://varhaiskasvatus.tampere.fi/employee/profile/' || head.id            AS head_url,
       head.last_name                                                               AS head_last_name,
       head.first_name                                                              AS head_first_name,
       head.email                                                                   AS head_email,
       'https://varhaiskasvatus.tampere.fi/employee/profile/' || partner.id         AS partner_url,
       partner.last_name                                                            AS partner_last_name,
       partner.first_name                                                           AS partner_first_name,
       partner.email                                                                AS partner_email
FROM placement p1
         JOIN daycare d1 ON d1.id = p1.unit_id
         JOIN person child ON child.id = p1.child_id
         LEFT JOIN fridge_child fc ON fc.child_id = child.id
    AND p1.start_date BETWEEN fc.start_date AND fc.end_date
         LEFT JOIN person head ON head.id = fc.head_of_child
         LEFT JOIN fridge_partner fp1 ON fp1.person_id = head.id
    AND p1.start_date BETWEEN fp1.start_date AND fp1.end_date
         LEFT JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id
    AND p1.start_date BETWEEN fp2.start_date AND fp2.end_date
    AND fp1.indx != fp2.indx
         LEFT JOIN person partner ON partner.id = fp2.person_id
WHERE p1.start_date BETWEEN '2025-08-01'::date AND '2025-10-31'::date
  AND p1.type NOT IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_DAYCARE_ONLY')
  AND (EXISTS (SELECT 1
               FROM placement p2
                        JOIN daycare d2 ON d2.id = p2.unit_id
               WHERE p1.child_id = p2.child_id
                 AND p1.start_date = p2.end_date + interval '1 day'
                 AND (d1.provider_type != d2.provider_type OR p1.type != p2.type)
                 AND p2.type NOT IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_DAYCARE_ONLY')) OR
       NOT EXISTS (SELECT 1
                   FROM placement p2
                   WHERE p1.child_id = p2.child_id
                     AND p1.start_date = p2.end_date + interval '1 day'))
ORDER BY child.last_name, child.first_name, child.id;
