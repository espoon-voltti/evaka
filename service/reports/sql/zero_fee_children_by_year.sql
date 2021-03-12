-- SPDX-FileCopyrightText: 2017-2021 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

SELECT DATE_PART('year', fee_decision_part.date_of_birth)::int AS birth_year, COUNT(fee_decision_part.child) AS count
FROM fee_decision
JOIN fee_decision_part ON fee_decision.id = fee_decision_part.fee_decision_id
LEFT JOIN LATERAL (
 SELECT SUM(effects.effect)
 FROM (
   SELECT id, (jsonb_array_elements(fee_alterations)->>'effect')::integer effect
   FROM fee_decision_part
 ) effects
 WHERE effects.id = fee_decision_part.id
) fee_alteration_effects ON true
WHERE fee_decision.status IN ('SENT', 'WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING')
AND (fee_decision_part.fee + COALESCE(fee_alteration_effects.sum, 0)) = 0
AND daterange(fee_decision.valid_from, fee_decision.valid_to, '[]') @> '2020-09-15'::date
GROUP BY DATE_PART('year', fee_decision_part.date_of_birth)
