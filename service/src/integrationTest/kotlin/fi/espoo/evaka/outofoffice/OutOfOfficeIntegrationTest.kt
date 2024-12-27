package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class OutOfOfficeIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var outOfOfficeController: OutOfOfficeController

    private val clock = RealEvakaClock()

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.UNIT_SUPERVISOR),
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(DevEmployee(id = employee.id))
            tx.insertDaycareAclRow(
                daycareId = daycare.id,
                employeeId = employee.id,
                UserRole.UNIT_SUPERVISOR,
            )
        }
    }

    @Test
    fun `get out of office periods`() {
        val ranges = outOfOfficeController.getOutOfOfficePeriods(dbInstance(), employee, clock)
        assertEquals(0, ranges.size)
    }
}
