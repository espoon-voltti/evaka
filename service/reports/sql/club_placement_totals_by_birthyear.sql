-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Club placement totals on the specified date, grouped by birth year
SELECT
    count(*) AS number_of_children,
    extract(YEAR FROM person.date_of_birth) AS birth_year
FROM
    club_placement,
    placement,
    application,
    person
WHERE
    club_placement.placement_id = placement.id
    AND placement.application_id = application.id
    AND application.child_id = person.id
    AND placement.start_date <= '2019-12-15'
    AND (placement.end_date >= '2019-12-15' OR placement.end_date IS NULL)
GROUP BY
    birth_year
ORDER BY
    birth_year asc;
