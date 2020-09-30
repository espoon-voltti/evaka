-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Report of all of the childred who requested for preparatory education 1.8.2020 onwards
-- Usage:
-- 1) make sure you can connect the target db with local psql
-- 2) execute this file using psql to produce .csv, example with PSQL 12 client:
-- psql --host=localhost --port 25432 --password --username=XXX
--      --dbname=evaka_service -f preparatory_children.sql --csv > output.csv
SELECT
    care_area.name "Haettu alue",
    d1.name AS "Haettu yksikkö",
    d2.name AS "Sijoitettu yksikkö",
    person.first_name AS "Etunimi",
    person.last_name AS "Sukunimi",
    person.social_security_number AS "Hetu"
FROM
    application_view appl
    LEFT JOIN daycare d1 ON appl.preferredunit = d1.id
    LEFT JOIN daycare d2 ON appl.placementdaycareunit = d2.id
    LEFT JOIN person ON appl.childid = person.id
    LEFT JOIN care_area ON d1.care_area_id = care_area.id
WHERE
    (appl.startDate >= '2020-08-01' OR appl.startDate IS NULL)
    AND appl.preparatoryeducation = true
    AND appl.sentdate >= '2020-01-08'
    AND appl.status <> ALL ( ARRAY[1, 2, 10, 12, 13] )
    AND appl.type = 'preschool'
ORDER BY
    care_area.name, d1.name ASC;