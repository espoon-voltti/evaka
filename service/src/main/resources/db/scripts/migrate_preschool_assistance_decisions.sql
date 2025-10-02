-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Schedules async jobs to migrate preschool assistance decisions
-- into child documents / decisions

INSERT INTO async_job(type, payload, retry_count, retry_interval)
SELECT 'MigratePreschoolAssistanceDecision',
  jsonb_build_object(
    'user', NULL,
    'decisionId', id),
    1,
    interval '5 seconds'
FROM (
  SELECT id FROM assistance_need_preschool_decision
) decisions_to_migrate;
