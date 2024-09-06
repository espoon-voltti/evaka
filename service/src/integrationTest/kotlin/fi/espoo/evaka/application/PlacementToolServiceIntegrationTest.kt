// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.messaging.AccountType
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.getUnreadMessagesCounts
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.preschoolTerm2021
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.snDefaultPreschool
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDaycareGroup
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PlacementToolServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var service: PlacementToolService
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var scheduledJobs: ScheduledJobs
    @Autowired lateinit var accessCpontrol: AccessControl

    private val clock = MockEvakaClock(2021, 1, 10, 12, 0)
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val serviceworker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    val currentPlacementStart = LocalDate.of(2020, 11, 1)
    val currentPlacementEnd = LocalDate.of(2021, 5, 31)
    val serviceNeedOption =
        ServiceNeedOption(
            snDefaultPreschool.id,
            snDefaultPreschool.nameFi,
            snDefaultPreschool.nameSv,
            snDefaultPreschool.nameEn,
            PlacementType.PRESCHOOL_DAYCARE,
        )
    val partTimeServiceNeedOption =
        ServiceNeedOption(
            snDefaultPreschool.id,
            snDefaultPreschool.nameFi,
            snDefaultPreschool.nameSv,
            snDefaultPreschool.nameEn,
            PlacementType.PRESCHOOL,
        )
    val preschoolTerm =
        PreschoolTerm(
            preschoolTerm2021.id,
            preschoolTerm2021.finnishPreschool,
            preschoolTerm2021.swedishPreschool,
            preschoolTerm2021.extendedTerm,
            preschoolTerm2021.applicationPeriod,
            preschoolTerm2021.termBreaks,
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycareGroup)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(DevGuardian(testAdult_1.id, testChild_1.id))
            tx.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    headOfChild = testAdult_1.id,
                    startDate = testChild_1.dateOfBirth,
                    endDate = testChild_1.dateOfBirth.plusYears(18),
                )
            )
            tx.insertServiceNeedOptions()
            tx.insertPlacement(
                PlacementType.DAYCARE,
                testChild_1.id,
                testDaycare.id,
                currentPlacementStart,
                currentPlacementEnd,
                false,
            )
            tx.insert(preschoolTerm2021)
            MockPersonDetailsService.addPersons(testAdult_1, testChild_1)
            MockPersonDetailsService.addDependants(testAdult_1, testChild_1)
            tx.upsertEmployeeMessageAccount(serviceworker.id, AccountType.SERVICE_WORKER)
        }
    }

    @Test
    fun `parse csv`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${testChild_1.id}";"${testDaycare.id}"
        """
                .trimIndent()

        val data = service.parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(1, data.size)
        assertEquals(testDaycare.id, data[0].preschoolId)
        assertEquals(testChild_1.id, data[0].childId)
    }

    @Test
    fun `parse csv with faulty child id`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "";"${testDaycare.id}"
        """
                .trimIndent()

        val data = service.parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `parse csv with faulty group id`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${testChild_1.id}";""
        """
                .trimIndent()

        val data = service.parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `parse csv with missing group id field`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${testChild_1.id}";
        """
                .trimIndent()

        val data = service.parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `create application with one guardian`() {
        val data = PlacementToolData(childId = testChild_1.id, preschoolId = testDaycare.id)
        service.createApplication(
            db,
            admin,
            clock,
            data,
            partTimeServiceNeedOption,
            serviceNeedOption,
            preschoolTerm,
        )

        clock.tick()
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries =
            db.read { it.fetchApplicationSummariesForGuardian(testAdult_1.id) }
        assertEquals(1, applicationSummaries.size)

        val summary = applicationSummaries.first()
        assertEquals(summary.childId, testChild_1.id)
        assertEquals(summary.preferredUnitId, testDaycare.id)

        val messagingAccount = db.read { it.getCitizenMessageAccount(testAdult_1.id) }
        assertNotNull(messagingAccount)
        val messageCount =
            db.read {
                it.getUnreadMessagesCounts(
                        accessCpontrol.requireAuthorizationFilter(
                            it,
                            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .firstOrNull()
                    ?.unreadCount ?: 0
            }
        assertEquals(1, messageCount)
    }

    @Test
    fun `create application with two guardians`() {
        db.transaction { tx ->
            tx.insert(testAdult_2, DevPersonType.ADULT)
            tx.insert(DevGuardian(testAdult_2.id, testChild_1.id))
            MockPersonDetailsService.addPersons(testAdult_2)
            MockPersonDetailsService.addDependants(testAdult_2, testChild_1)
        }
        val data = PlacementToolData(childId = testChild_1.id, preschoolId = testDaycare.id)
        service.createApplication(
            db,
            admin,
            clock,
            data,
            partTimeServiceNeedOption,
            serviceNeedOption,
            preschoolTerm,
        )

        clock.tick()
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries =
            db.read { it.fetchApplicationSummariesForGuardian(testAdult_1.id) }
        assertEquals(1, applicationSummaries.size)

        val summary = applicationSummaries.first()
        assertEquals(summary.childId, testChild_1.id)
        assertEquals(summary.preferredUnitId, testDaycare.id)

        val application = db.read { it.fetchApplicationDetails(summary.applicationId) }
        assertNotNull(application)
        assert(application.allowOtherGuardianAccess)
    }

    @Test
    fun `create application without proper child`() {
        val data = PlacementToolData(childId = testChild_3.id, preschoolId = testDaycare.id)
        assertThrows<Exception> {
            service.createApplication(
                db,
                admin,
                clock,
                data,
                partTimeServiceNeedOption,
                serviceNeedOption,
                preschoolTerm,
            )
        }
    }

    @Test
    fun `create application without proper unit`() {
        val data = PlacementToolData(childId = testChild_1.id, preschoolId = testDaycare2.id)
        assertThrows<Exception> {
            service.createApplication(
                db,
                admin,
                clock,
                data,
                partTimeServiceNeedOption,
                serviceNeedOption,
                preschoolTerm,
            )
        }
    }
}
