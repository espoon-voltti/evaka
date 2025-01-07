// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.OutOfOfficeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class OutOfOfficePeriod(val id: OutOfOfficeId, val period: FiniteDateRange)

data class OutOfOfficePeriodUpsert(val id: OutOfOfficeId?, val period: FiniteDateRange)

fun Database.Read.getOutOfOfficePeriods(
    employeeId: EmployeeId,
    today: LocalDate,
): List<OutOfOfficePeriod> {
    val result =
        createQuery {
                sql(
                    """
SELECT id, period
FROM out_of_office
WHERE employee_id = ${bind(employeeId)}
AND period && DATERANGE(${bind(today)}, NULL)
"""
                )
            }
            .toList<OutOfOfficePeriod>()
    return result
}

fun Database.Transaction.upsertOutOfOfficePeriod(
    employeeId: EmployeeId,
    period: OutOfOfficePeriodUpsert,
) {
    if (period.id == null) {
        createUpdate {
                sql(
                    """
INSERT INTO out_of_office (employee_id, period)
VALUES (${bind(employeeId)}, DATERANGE(${bind(period.period.start)}, ${bind(period.period.end)}, '[]'))
"""
                )
            }
            .execute()
    } else {
        createUpdate {
                sql(
                    """
UPDATE out_of_office
SET period = DATERANGE(${bind(period.period.start)}, ${bind(period.period.end)}, '[]')
WHERE id = ${bind(period.id)}
"""
                )
            }
            .updateExactlyOne()
    }
}

fun Database.Transaction.deleteOutOfOfficePeriod(id: OutOfOfficeId) {
    createUpdate {
            sql(
                """
DELETE FROM out_of_office
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()
}
