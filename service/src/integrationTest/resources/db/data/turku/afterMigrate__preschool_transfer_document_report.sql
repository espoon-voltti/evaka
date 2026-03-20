-- SPDX-FileCopyrightText: 2026 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- \copy (SELECT * FROM preschool_transfer_document_report(current_date)) TO 'preschool_transfer_document_report.csv' CSV DELIMITER ';' HEADER

DROP FUNCTION IF EXISTS preschool_transfer_document_report;

CREATE FUNCTION preschool_transfer_document_report(inspection_date date)
    RETURNS TABLE (
        "henkilötunnus"             TEXT,
        "nimi"                      TEXT,
        "yksikkö"                   TEXT,
        "tuen taso esiopetuksessa"  TEXT
    )
AS
$$
WITH children_with_document AS (
    SELECT DISTINCT cd.child_id
    FROM child_document cd
    JOIN document_template dt ON cd.template_id = dt.id
    WHERE
        dt.name ILIKE '%tiedonsiirto esiopetuksesta perusopetukseen%' OR
        dt.name ILIKE '%överföring av uppgifter från förskoleundervisningen%'
), assistances AS (
    SELECT child_id,
           string_agg(
               CASE level
                   WHEN 'INTENSIFIED_SUPPORT' THEN 'Tehostettu tuki'
                   WHEN 'SPECIAL_SUPPORT' THEN 'Erityinen tuki'
                   WHEN 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1' THEN 'Erityinen tuki ja pidennetty oppivelvollisuus - taso 1'
                   WHEN 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2' THEN 'Erityinen tuki ja pidennetty oppivelvollisuus - taso 2'
                   WHEN 'CHILD_SUPPORT' THEN 'Lapsikohtainen tuki'
                   WHEN 'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION' THEN 'Lapsikohtainen tuki ja varhennettu oppivelvollisuus'
                   WHEN 'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION' THEN 'Lapsikohtainen tuki ja pidennetty oppivelvollisuus - muu'
                   WHEN 'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION' THEN 'Lapsikohtainen tuki ja pidennetty oppivelvollisuus - kehitysvamma 2'
                   WHEN 'GROUP_SUPPORT' THEN 'Ryhmäkohtaiset tukimuodot'
               END,
               ', '
           ) AS levels
    FROM preschool_assistance
    GROUP BY child_id
)
SELECT
    p.social_security_number AS "henkilötunnus",
    p.last_name || ' ' || p.first_name AS "nimi",
    u.name AS "yksikkö",
    a.levels AS "tuen taso esiopetuksessa"
FROM children_with_document cwd
JOIN placement pl ON (
    cwd.child_id = pl.child_id AND
    pl.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PREPARATORY', 'PREPARATORY_DAYCARE') AND
    $1 BETWEEN pl.start_date AND pl.end_date
)
JOIN person p ON cwd.child_id = p.id
JOIN daycare u ON pl.unit_id = u.id
LEFT JOIN assistances a ON cwd.child_id = a.child_id
$$
LANGUAGE SQL;
