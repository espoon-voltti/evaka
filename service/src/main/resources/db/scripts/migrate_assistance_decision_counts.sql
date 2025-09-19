-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Fetch counts of
-- - migrated assistance decisions (preschool / daycare, finnish / swedish)
-- - assistance decisions in the old format (includes both migrated and not migrated)

SELECT
    -- Daycare assistance decisions (old format and migrated)
    (SELECT COUNT(*) FROM assistance_need_decision
     WHERE status IN ('ACCEPTED', 'REJECTED', 'ANNULLED')
       AND document_key IS NOT NULL
       AND decision_number IS NOT NULL
       AND decision_maker_employee_id IS NOT NULL
       AND decision_made IS NOT NULL
       AND language = 'FI'
    ) AS old_daycare_fi,
    (SELECT COUNT(*) FROM child_document
     WHERE template_id IN (
         SELECT id FROM document_template
         WHERE type = 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION' AND language = 'FI'
     )
    ) AS migrated_daycare_fi,
    (SELECT COUNT(*) FROM assistance_need_decision
     WHERE status IN ('ACCEPTED', 'REJECTED', 'ANNULLED')
       AND document_key IS NOT NULL
       AND decision_number IS NOT NULL
       AND decision_maker_employee_id IS NOT NULL
       AND decision_made IS NOT NULL
       AND language = 'SV'
    ) AS old_daycare_sv,
    (SELECT COUNT(*) FROM child_document
     WHERE template_id IN (
         SELECT id FROM document_template
         WHERE type = 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION' AND language = 'SV'
     )
    ) AS migrated_daycare_sv,

    -- Preschool assistance decisions (old format and migrated)
    (SELECT COUNT(*) FROM assistance_need_preschool_decision
     WHERE status IN ('ACCEPTED', 'REJECTED', 'ANNULLED')
       AND document_key IS NOT NULL
       AND decision_number IS NOT NULL
       AND decision_maker_employee_id IS NOT NULL
       AND decision_made IS NOT NULL
       AND language = 'FI'
    ) AS old_preschool_fi,
    (SELECT COUNT(*) FROM child_document
     WHERE template_id IN (
         SELECT id FROM document_template
         WHERE type = 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION' AND language = 'FI'
     )
    ) AS migrated_preschool_fi,
    (SELECT COUNT(*) FROM assistance_need_preschool_decision
     WHERE status IN ('ACCEPTED', 'REJECTED', 'ANNULLED')
       AND document_key IS NOT NULL
       AND decision_number IS NOT NULL
       AND decision_maker_employee_id IS NOT NULL
       AND decision_made IS NOT NULL
       AND language = 'SV'
    ) AS old_preschool_sv,
    (SELECT COUNT(*) FROM child_document
     WHERE template_id IN (
         SELECT id FROM document_template
         WHERE type = 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION' AND language = 'SV'
     )
    ) AS migrated_preschool_sv;
