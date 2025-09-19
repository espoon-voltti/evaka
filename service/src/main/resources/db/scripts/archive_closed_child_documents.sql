-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Schedules async jobs to archive child documents from closed templates
-- marked for external archiving that haven't been archived yet.
--
-- Archiving strategy: Distributes the job limit evenly across all eligible document types,
-- selecting the oldest unarchived documents first from each type to ensure balanced processing.

-- Follow the progress via Kibana:
-- {
--   "bool": {
--     "minimum_should_match": 1,
--     "should": [
--       {
--         "match_phrase": {
--           "loggerName": "fi.espoo.evaka.shared.async.AsyncJobPool.AsyncJob.bulk"
--         }
--       },
--       {
--         "match_phrase": {
--           "loggerName": "fi.espoo.evaka.document.archival.ArchiveChildDocumentService"
--         }
--       }
--     ]
--   }
-- }

-- A configurable limit for how many archive jobs to schedule in one run.
\set job_limit 100

INSERT INTO async_job(type, payload, retry_count, retry_interval)
SELECT 'ArchiveChildDocument',
       jsonb_build_object(
               'user', NULL,
               'documentId', document_id
       ),
       3,
       interval '5 minutes'
FROM (
    WITH
    -- Select all templates which validity period has ended and are marked as externally archived.
    eligible_templates AS (
        SELECT id, validity FROM document_template
        WHERE upper(validity) <= current_date AND archive_externally = true
    ),
    -- Calculate how many documents to fetch per template.
    docs_per_template AS (
        SELECT coalesce(ceil(:job_limit::numeric / count(*)), 0) FROM eligible_templates
    ),
    -- For each eligible template, select document instances to schedule the archive jobs for.
    ranked_documents AS (
        SELECT
            cd.id as document_id,
            dt.validity as template_validity,
            row_number() OVER (PARTITION BY cd.template_id ORDER BY cd.created) as rn
        FROM
            child_document cd
        JOIN
            eligible_templates dt ON cd.template_id = dt.id
        WHERE
            cd.archived_at IS NULL
    )
    SELECT document_id
    FROM ranked_documents
    WHERE rn <= (SELECT * FROM docs_per_template)
    ORDER BY rn, template_validity
    LIMIT :job_limit
) documents;
