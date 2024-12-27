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
SELECT id, daterange(start_date, end_date, '[]') AS period
FROM out_of_office
WHERE employee_id = ${bind(employeeId)}
AND end_date >= ${bind(today)}
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
INSERT INTO out_of_office (employee_id, start_date, end_date)
VALUES (${bind(employeeId)}, ${bind(period.period.start)}, ${bind(period.period.end)})
"""
                )
            }
            .execute()
    } else {
        createUpdate {
                sql(
                    """
UPDATE out_of_office
SET start_date = ${bind(period.period.start)}, end_date = ${bind(period.period.end)}
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
