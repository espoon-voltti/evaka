-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Get every unit and every child who has applied to it as the preferred
-- unit from defined day onwards
--
-- Usage:
-- 1) make sure you can connect the target db with local psql
-- 2) make sure that the date and application type in the query is what you want
-- 3) execute this file using psql to produce .csv
-- (See https://voltti.atlassian.net/browse/EVAKA-2843)
\copy (
WITH active_placements AS (
SELECT
    placement.child_id AS child_id,
    daycare.name AS daycare_name
FROM
    placement,
    daycare
WHERE
    placement.approved = true
    AND start_date <= '2020-01-20'
    AND end_date >= '2020-01-20'
    AND placement.unit_id = daycare.id
)
SELECT
    care_area.name AS "Alue",
    daycare.name AS "Pyydetty paikka",
    application.childname AS "Lapsi",
    application.childssn AS "Hetu",
    application.childstreeaddr AS "Lapsen osoite",
    application.guardianphonenumber AS "Puh.",
    placements.daycare_name AS "Nykyinen sijoitus",
    application.daycareassistanceneeded AS "Tuen tarve",
    application.preparatoryeducation AS "Valmistava",
    application.siblingbasis AS "Sisarusperuste",
    application.connecteddaycare AS "Liittyvä",
    application.startDate AS "Pyydetty aloitus",
    application.sentdate AS "Saapunut"
FROM
    daycare
LEFT JOIN
    care_area ON care_area.id = daycare.care_area_id
LEFT JOIN
    application_view application ON application.preferredunit = daycare.id
LEFT JOIN
    active_placements placements ON application.childid = placements.child_id
WHERE
    (application.startDate >= '2020-08-01' OR application.startDate IS NULL)
    AND application.sentdate >= '2020-01-08'
    AND application.status <> ALL ( ARRAY[1, 2, 10, 12, 13] )
    AND application.type = 'preschool'
ORDER BY
    "Alue", daycare.name
) TO 'sijoitusraportti.csv' CSV HEADER;