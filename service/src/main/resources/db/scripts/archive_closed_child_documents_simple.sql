-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Schedules async jobs to archive child documents from closed templates
-- marked for external archiving that haven't been archived yet.


-- A configurable limit for how many archive jobs to schedule in one run.
\set job_limit 100

INSERT INTO async_job(type, payload, retry_count, retry_interval)
SELECT 'ArchiveChildDocument',
       jsonb_build_object(
               'user', NULL,
               'documentId', cd.id
       ),
       3,
       interval '5 minutes'
FROM child_document cd
JOIN document_template dt ON cd.template_id = dt.id
WHERE upper(dt.validity) <= current_date
  AND dt.archive_externally = true
  AND cd.archived_at IS NULL
ORDER BY cd.created
LIMIT :job_limit;
