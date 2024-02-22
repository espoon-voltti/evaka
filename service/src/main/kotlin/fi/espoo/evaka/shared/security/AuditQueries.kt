// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.upsertCitizenUser(id: PersonId) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO evaka_user (id, type, citizen_id, name)
SELECT id, 'CITIZEN', id, last_name || ' ' || first_name
FROM person
WHERE id = :id
ON CONFLICT (id) DO UPDATE SET name = excluded.name
"""
        )
        .bind("id", id)
        .execute()

fun Database.Transaction.upsertEmployeeUser(id: EmployeeId) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, last_name || ' ' || coalesce(preferred_first_name, first_name)
FROM employee
WHERE id = :id
ON CONFLICT (id) DO UPDATE SET name = excluded.name
"""
        )
        .bind("id", id)
        .execute()

fun Database.Transaction.upsertMobileDeviceUser(id: MobileDeviceId) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO evaka_user (id, type, mobile_device_id, name)
SELECT id, 'MOBILE_DEVICE', id, name
FROM mobile_device
WHERE id = :id
ON CONFLICT (id) DO UPDATE SET name = excluded.name
"""
        )
        .bind("id", id)
        .execute()
