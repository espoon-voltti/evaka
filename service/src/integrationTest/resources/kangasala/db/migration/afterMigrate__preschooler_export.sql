-- SPDX-FileCopyrightText: 2023-2025 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- \copy (SELECT * FROM preschooler_export(2019, current_date)) TO 'evaka_preschooler_export.csv' CSV DELIMITER ';'

DROP FUNCTION IF EXISTS preschooler_export;

CREATE FUNCTION preschooler_export(year_of_birth int, date date)
    RETURNS TABLE (
        "esioppilaan henkilötunnus"    TEXT,
        "esioppilaan sukunimi"         TEXT,
        "esioppilaan etunimet"         TEXT,
        "esioppilaan lähiosoite"       TEXT,
        "esioppilaan postinumero"      TEXT,
        "huoltajan 1 henkilötunnus"    TEXT,
        "huoltajan 1 sukunimi"         TEXT,
        "huoltajan 1 etunimet"         TEXT,
        "huoltajan 1 lähiosoite"       TEXT,
        "huoltajan 1 postinumero"      TEXT,
        "huoltajan 1 puhelinnumero"    TEXT,
        "huoltajan 1 sähköpostiosoite" TEXT,
        "huoltajan 2 henkilötunnus"    TEXT,
        "huoltajan 2 sukunimi"         TEXT,
        "huoltajan 2 etunimet"         TEXT,
        "huoltajan 2 lähiosoite"       TEXT,
        "huoltajan 2 postinumero"      TEXT,
        "huoltajan 2 puhelinnumero"    TEXT,
        "huoltajan 2 sähköpostiosoite" TEXT
    )
AS
$$
WITH preschoolers AS (
    SELECT
        child.id,
        child.social_security_number,
        child.last_name,
        child.first_name,
        child.street_address,
        child.postal_code,
        child.post_office
    FROM person child
    JOIN placement p ON (
        child.id = p.child_id AND
        p.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_CLUB') AND
        $2 BETWEEN p.start_date AND p.end_date
    )
    WHERE
        (
            date_part('year', child.date_of_birth) = $1 AND
            NOT EXISTS (
                SELECT FROM preschool_assistance
                WHERE
                    child.id = child_id AND
                    between_start_and_end(valid_during, $2) AND
                    level IN (
                        'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
                        'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2',
                        'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION',
                        'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION',
                        'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'
                    )
            )
        ) OR (
            date_part('year', child.date_of_birth) = $1 - 1
        )
),
guardians AS (
    SELECT DISTINCT g1.child_id, g1.guardian_id AS guardian1_id, g2.guardian_id AS guardian2_id
    FROM guardian g1
    LEFT JOIN guardian g2 ON g1.child_id = g2.child_id AND g1.guardian_id <> g2.guardian_id
    WHERE g2.guardian_id IS NULL OR g1.guardian_id < g2.guardian_id
)
SELECT
    child.social_security_number AS "esioppilaan henkilötunnus",
    child.last_name AS "esioppilaan sukunimi",
    child.first_name AS "esioppilaan etunimet",
    child.street_address AS "esioppilaan lähiosoite",
    child.postal_code || ' ' || child.post_office AS "esioppilaan postinumero",
    guardian1.social_security_number AS "huoltajan 1 henkilötunnus",
    guardian1.last_name AS "huoltajan 1 sukunimi",
    guardian1.first_name AS "huoltajan 1 etunimet",
    guardian1.street_address AS "huoltajan 1 lähiosoite",
    guardian1.postal_code || ' ' || guardian1.post_office AS "huoltajan 1 postinumero",
    guardian1.phone AS "huoltajan 1 puhelinnumero",
    guardian1.email AS "huoltajan 1 sähköpostiosoite",
    guardian2.social_security_number AS "huoltajan 2 henkilötunnus",
    guardian2.last_name AS "huoltajan 2 sukunimi",
    guardian2.first_name AS "huoltajan 2 etunimet",
    guardian2.street_address AS "huoltajan 2 lähiosoite",
    guardian2.postal_code || ' ' || guardian2.post_office AS "huoltajan 2 postinumero",
    guardian2.phone AS "huoltajan 2 puhelinnumero",
    guardian2.email AS "huoltajan 2 sähköpostiosoite"
FROM preschoolers child
LEFT JOIN guardians ON child.id = guardians.child_id
LEFT JOIN person guardian1 ON guardians.guardian1_id = guardian1.id
LEFT JOIN person guardian2 ON guardians.guardian2_id = guardian2.id
$$ LANGUAGE SQL;
