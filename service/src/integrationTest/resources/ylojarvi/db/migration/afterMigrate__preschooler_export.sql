-- SPDX-FileCopyrightText: 2023-2025 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- \copy (SELECT * FROM preschooler_export(2019, current_date)) TO 'evaka_preschooler_export.csv' CSV DELIMITER ';'

DROP FUNCTION IF EXISTS preschooler_export;

CREATE FUNCTION preschooler_export(year_of_birth int, date date)
    RETURNS TABLE
            (
                "esioppilaan henkilötunnus"            TEXT,
                "esioppilaan sukunimi"                 TEXT,
                "esioppilaan etunimet"                 TEXT,
                "esioppilaan lähiosoite"               TEXT,
                "esioppilaan postinumero"              TEXT,
                "esioppilaan esiopetuspaikka"          TEXT,
                "esioppilaan esiopetusryhmä"           TEXT,
                "esioppilaan voimassa oleva tuen taso" TEXT,
                "huoltajan 1 henkilötunnus"            TEXT,
                "huoltajan 1 sukunimi"                 TEXT,
                "huoltajan 1 etunimet"                 TEXT,
                "huoltajan 1 lähiosoite"               TEXT,
                "huoltajan 1 postinumero"              TEXT,
                "huoltajan 1 sähköpostiosoite"         TEXT,
                "huoltajan 2 henkilötunnus"            TEXT,
                "huoltajan 2 sukunimi"                 TEXT,
                "huoltajan 2 etunimet"                 TEXT,
                "huoltajan 2 lähiosoite"               TEXT,
                "huoltajan 2 postinumero"              TEXT,
                "huoltajan 2 sähköpostiosoite"         TEXT
            )
AS
$$
WITH preschoolers AS (SELECT coalesce(duplicate_of.id, child.id)                         AS id,
                             child.id                                                    AS id_in_preschool,
                             coalesce(duplicate_of.social_security_number,
                                      child.social_security_number)                      AS social_security_number,
                             coalesce(duplicate_of.last_name, child.last_name)           AS last_name,
                             coalesce(duplicate_of.first_name, child.first_name)         AS first_name,
                             coalesce(duplicate_of.street_address, child.street_address) AS street_address,
                             coalesce(duplicate_of.postal_code, child.postal_code)       AS postal_code,
                             coalesce(duplicate_of.post_office, child.post_office)       AS post_office,
                             u.name                                                      AS unit_name,
                             dg.name                                                     AS group_name
                      FROM person child
                               JOIN placement p ON child.id = p.child_id AND
                                                   p.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_CLUB') AND
                                                   $2 BETWEEN p.start_date AND p.end_date
                               JOIN daycare u ON p.unit_id = u.id
                               LEFT JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id AND
                                                                        $2 BETWEEN dgp.start_date AND dgp.end_date
                               LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
                               LEFT JOIN person duplicate_of ON child.duplicate_of = duplicate_of.id
                      WHERE (
                                (date_part('year', child.date_of_birth) = $1 AND NOT EXISTS (SELECT
                                                                                             FROM preschool_assistance
                                                                                             WHERE child.id = child_id
                                                                                               AND between_start_and_end(valid_during, $2)
                                                                                               AND level IN
                                                                                                   ('SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
                                                                                                    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2',
                                                                                                    'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION',
                                                                                                    'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION',
                                                                                                    'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION')))
                                    OR
                                date_part('year', child.date_of_birth) = $1 - 1
                                )),
     guardians AS (SELECT DISTINCT g1.child_id, g1.guardian_id AS guardian1_id, g2.guardian_id AS guardian2_id
                   FROM guardian g1
                            LEFT JOIN guardian g2 ON g1.child_id = g2.child_id AND g1.guardian_id <> g2.guardian_id
                   WHERE g2.guardian_id IS NULL
                      OR g1.guardian_id < g2.guardian_id),
     assistances AS (SELECT child_id,
                            string_agg(CASE level
                                           WHEN 'INTENSIFIED_SUPPORT'
                                               THEN 'Tehostettu tuki'
                                           WHEN 'SPECIAL_SUPPORT'
                                               THEN 'Erityinen tuki ilman pidennettyä oppivelvollisuutta'
                                           WHEN 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1'
                                               THEN 'Erityinen tuki ja pidennetty oppivelvollisuus - muu (Koskeen)'
                                           WHEN 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'
                                               THEN 'Erityinen tuki ja pidennetty oppivelvollisuus - kehitysvamma 2 (Koskeen)'
                                           WHEN 'CHILD_SUPPORT'
                                               THEN 'Lapsikohtainen tuki ilman varhennettua oppivelvollisuutta (Koskeen)'
                                           WHEN 'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION'
                                               THEN 'Lapsikohtainen tuki ja varhennettu oppivelvollisuus (Koskeen)'
                                           WHEN 'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'
                                               THEN 'Lapsikohtainen tuki ja vanhan mallinen pidennetty ov - muu (Koskeen, käytössä siirtymäkautena 1.8.2025 - 31.7.2026)'
                                           WHEN 'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'
                                               THEN 'Lapsikohtainen tuki ja vanhan mallinen pidennetty ov - kehitysvamma 2 (Koskeen, käytössä siirtymäkautena 1.8.2025 - 31.7.2026)'
                                           WHEN 'GROUP_SUPPORT'
                                               THEN 'Ryhmäkohtaiset tukimuodot'
                                           END, ',') AS levels
                     FROM preschool_assistance
                     WHERE between_start_and_end(valid_during, $2)
                     GROUP BY child_id)
SELECT child.social_security_number                          AS "esioppilaan henkilötunnus",
       child.last_name                                       AS "esioppilaan sukunimi",
       child.first_name                                      AS "esioppilaan etunimet",
       child.street_address                                  AS "esioppilaan lähiosoite",
       child.postal_code || ' ' || child.post_office         AS "esioppilaan postinumero",
       child.unit_name                                       AS "esioppilaan esiopetuspaikka",
       child.group_name                                      AS "esioppilaan esiopetusryhmä",
       assistances.levels                                    AS "esioppilaan voimassa oleva tuen taso",
       guardian1.social_security_number                      AS "huoltajan 1 henkilötunnus",
       guardian1.last_name                                   AS "huoltajan 1 sukunimi",
       guardian1.first_name                                  AS "huoltajan 1 etunimet",
       guardian1.street_address                              AS "huoltajan 1 lähiosoite",
       guardian1.postal_code || ' ' || guardian2.post_office AS "huoltajan 1 postinumero",
       guardian1.email                                       AS "huoltajan 1 sähköpostiosoite",
       guardian2.social_security_number                      AS "huoltajan 2 henkilötunnus",
       guardian2.last_name                                   AS "huoltajan 2 sukunimi",
       guardian2.first_name                                  AS "huoltajan 2 etunimet",
       guardian2.street_address                              AS "huoltajan 2 lähiosoite",
       guardian2.postal_code || ' ' || guardian2.post_office AS "huoltajan 2 postinumero",
       guardian2.email                                       AS "huoltajan 2 sähköpostiosoite"
FROM preschoolers child
         LEFT JOIN assistances ON child.id_in_preschool = assistances.child_id
         LEFT JOIN guardians ON child.id = guardians.child_id
         LEFT JOIN person guardian1 ON guardians.guardian1_id = guardian1.id
         LEFT JOIN person guardian2 ON guardians.guardian2_id = guardian2.id
$$
    LANGUAGE SQL;
