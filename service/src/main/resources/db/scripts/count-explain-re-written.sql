-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

SELECT
    acc.id as account_id,
    coalesce(mt.is_copy, false) as is_copy,
    mtp.folder_id,
    count(mt.id) AS count
FROM message_account acc
         LEFT JOIN (
    SELECT
        dga.employee_id,
        daycare_group_id,
        (dga.created - interval '1 week')::date AS access_limit
    FROM daycare_group_acl dga
) dga ON dga.employee_id = '847a28c8-53ae-11ee-857e-53042a4a7e0f' AND dga.daycare_group_id = acc.daycare_group_id
         LEFT JOIN daycare_acl da ON da.employee_id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
         LEFT JOIN message_recipients mr ON mr.recipient_id = acc.id AND mr.read_at IS NULL
         LEFT JOIN message m ON mr.message_id = m.id AND m.sent_at IS NOT NULL
         LEFT JOIN message_thread_participant mtp ON m.thread_id = mtp.thread_id AND mtp.participant_id = acc.id AND (da.role = 'UNIT_SUPERVISOR' OR (mtp.folder_id IS NOT NULL OR mtp.last_message_timestamp >= dga.access_limit))
         LEFT JOIN message_thread mt ON m.thread_id = mt.id AND (da.role = 'UNIT_SUPERVISOR' OR (mtp.folder_id IS NOT NULL OR mt.is_copy = false OR m.sent_at >= dga.access_limit))
         LEFT JOIN message_thread_folder mtf ON mtp.folder_id = mtf.id
WHERE acc.id IN (
    -- Employee's own message account
    SELECT acc.id
    FROM message_account acc
             JOIN employee ON acc.employee_id = employee.id
    WHERE employee.id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND acc.active = TRUE

    UNION ALL

    -- Group-based access (daycare group ACL)
    SELECT acc.id
    FROM message_account acc
             JOIN daycare_group_acl gacl ON gacl.daycare_group_id = acc.daycare_group_id
    WHERE gacl.employee_id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND acc.active = TRUE

    UNION ALL

    -- Unit supervisor access to all group accounts in their units
    SELECT acc.id
    FROM message_account acc
             JOIN daycare_group dg ON acc.daycare_group_id = dg.id
             JOIN daycare_acl da ON da.daycare_id = dg.daycare_id
    WHERE da.employee_id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND da.role = 'UNIT_SUPERVISOR'
      AND acc.active = TRUE

    UNION ALL

    -- Municipal account for admins/messaging roles
    SELECT acc.id
    FROM employee e
             JOIN message_account acc ON acc.type = 'MUNICIPAL'
    WHERE e.id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND e.roles && '{ADMIN, MESSAGING}'::user_role[]

    UNION ALL

    -- Service worker account
    SELECT acc.id
    FROM employee e
             JOIN message_account acc ON acc.type = 'SERVICE_WORKER'
    WHERE e.id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND e.roles && '{SERVICE_WORKER}'::user_role[]

    UNION ALL

    -- Finance account
    SELECT acc.id
    FROM employee e
             JOIN message_account acc ON acc.type = 'FINANCE'
    WHERE e.id = '847a28c8-53ae-11ee-857e-53042a4a7e0f'
      AND e.roles && '{FINANCE_ADMIN}'::user_role[]
)
  AND (mtp.folder_id IS NULL OR mtf.name != 'ARCHIVE')
GROUP BY acc.id, mt.is_copy, mtp.folder_id