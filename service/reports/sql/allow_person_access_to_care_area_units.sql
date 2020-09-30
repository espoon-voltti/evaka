-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Give a person rights to all daycares of specified care are, based on person.email
-- The person must be on eVaka PIS with the specified email, and the care
-- area name must match the name field in care_area.
--
-- Helpful stuff:
-- Current acl list:
-- select person.first_name, person.last_name, daycare.name from daycare_acl, daycare, person where daycare_acl.daycare_id = daycare.id and daycare_acl.person_id = person.id;
-- Care area names:
-- Leppävaara (länsi), Leppävaara (itä), Matinkylä-Olari, Tapiola, Espoon keskus (eteläinen), Espoon keskus (pohjoinen), Svenska bildningstjänster
INSERT INTO
    daycare_acl(daycare_id, person_id)
SELECT
    daycare.id AS daycare_id, person.id AS person_id
FROM
    daycare LEFT JOIN care_area ON daycare.care_area_id = care_area.id, (
    SELECT
        id
    FROM
        person
    WHERE
        LOWER(person.email) = LOWER('seppo.sorsa@espoo.fi')
) person
WHERE
        care_area.name = 'Tapiola'
ON CONFLICT DO NOTHING;