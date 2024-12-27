package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.OutOfOfficeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class OutOfOfficePeriod(val id: OutOfOfficeId, val period: FiniteDateRange)

fun Database.Read.getOutOfOfficePeriods(
    employeeId: EmployeeId,
    today: LocalDate,
): List<OutOfOfficePeriod> {
    return createQuery {
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
}
