-- SPDX-FileCopyrightText: 2017-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Change fee_alteration amount from integer to numeric to support euros and cents
ALTER TABLE fee_alteration ALTER COLUMN amount TYPE numeric(10, 2);
