-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO special_diet (id, abbreviation)
VALUES (0, 'erityisruokavalio')
ON CONFLICT (id) DO UPDATE SET abbreviation = EXCLUDED.abbreviation
WHERE special_diet.abbreviation <> EXCLUDED.abbreviation;
