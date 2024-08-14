package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycareGroup
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import kotlin.test.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PlacementToolServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var service: PlacementToolService
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val clock = MockEvakaClock(2020, 12, 1, 12, 0)
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    val placementStart = LocalDate.of(2020, 11, 1)
    val placementEnd = LocalDate.of(2020, 12, 31)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycareGroup)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
            tx.insertPlacement(
                PlacementType.DAYCARE,
                testChild_1.id,
                testDaycare.id,
                placementStart,
                placementEnd,
                false
            )
            MockPersonDetailsService.addPersons(testAdult_1, testChild_1)
            MockPersonDetailsService.addDependants(testAdult_1, testChild_1)
        }
    }

    @Test fun `parse csv`() {}

    @Test fun `parse csv with faulty child id`() {}

    @Test fun `parse csv with faulty group id`() {}

    @Test fun `create applications`() {}

    @Test fun `create application without proper child`() {}

    @Test fun `create application without proper guardian`() {}
}
