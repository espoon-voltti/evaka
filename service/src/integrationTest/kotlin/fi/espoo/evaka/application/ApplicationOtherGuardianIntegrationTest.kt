// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.pis.Modifier
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.pis.updateFosterParentRelationshipValidity
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class ApplicationOtherGuardianIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val guardian = testAdult_5
    private val otherVtjGuardian = testAdult_6
    private val fosterParent = DevPerson(ssn = "010106A981M")
    private val child = testChild_6
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    private val application: ApplicationId = ApplicationId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.clearMessages()

        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(otherVtjGuardian, DevPersonType.ADULT)
            tx.insert(fosterParent, DevPersonType.ADULT)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(serviceWorker)
        }
        MockPersonDetailsService.addPersons(guardian, child, otherVtjGuardian)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherVtjGuardian, child)
    }

    @Test
    fun `sfi messages are sent to guardians and foster parents`() {
        val expectedOtherGuardians = setOf(otherVtjGuardian.id, fosterParent.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)
        insertFosterParent(clock)

        executeTestApplicationProcess(clock) {
            assertEquals(expectedOtherGuardians, getOtherGuardians())
        }
        assertEquals(expectedOtherGuardians + guardian.id, getSfiMessageRecipients())
    }

    @Test
    fun `legacy sensitive decisions are sent only to the application guardian`() {
        val expectedOtherGuardians = setOf(otherVtjGuardian.id, fosterParent.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)
        insertFosterParent(clock)

        val preferredStartDate = LocalDate.of(2024, 2, 1)
        insertTestApplication(preferredStartDate)

        sendApplication(clock)
        moveToWaitingPlacement(clock)
        createPlacementPlan(
            clock,
            period = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1)),
        )
        sendDecisionsWithoutProposal(clock)
        asyncJobRunner.runPendingJobsSync(clock, maxCount = 1) // create, but don't send yet
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "UPDATE decision SET document_contains_contact_info = TRUE WHERE application_id = ${bind(application)}"
                    )
                }
                .execute()
        }
        asyncJobRunner.runPendingJobsSync(clock) // send
        acceptDecisions(clock)

        assertEquals(expectedOtherGuardians, getOtherGuardians())
        assertEquals(setOf(guardian.id), getSfiMessageRecipients())
    }

    @ParameterizedTest(
        name =
            "sfi messages are sent to all guardians and foster parents - foster parent added during the process {0}"
    )
    @EnumSource(names = ["SENT", "WAITING_PLACEMENT", "WAITING_DECISION"])
    fun `sfi messages are sent to guardians and foster parents - foster parent added during the process`(
        addDuring: ApplicationStatus
    ) {
        val expectedOtherGuardians = mutableSetOf(otherVtjGuardian.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)

        executeTestApplicationProcess(clock) { status ->
            assertEquals(expectedOtherGuardians, getOtherGuardians())
            if (status == addDuring) {
                insertFosterParent(clock)
                expectedOtherGuardians += fosterParent.id
            }
        }

        assertEquals(expectedOtherGuardians + guardian.id, getSfiMessageRecipients())
    }

    @Test
    fun `sfi messages are sent to guardians and foster parents - foster parent added after decisions sent is not affected`() {
        val expectedOtherGuardians = setOf(otherVtjGuardian.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)
        executeTestApplicationProcess(clock) { status ->
            assertEquals(expectedOtherGuardians, getOtherGuardians())
            if (status == ApplicationStatus.WAITING_CONFIRMATION) {
                insertFosterParent(clock)
            }
        }

        assertEquals(expectedOtherGuardians + guardian.id, getSfiMessageRecipients())
    }

    @ParameterizedTest(
        name =
            "sfi messages are sent to all guardians and foster parents - guardian blocked during the process {0}"
    )
    @EnumSource(names = ["SENT", "WAITING_PLACEMENT", "WAITING_DECISION"])
    fun `sfi messages are sent to guardians - guardian blocked during the process`(
        blockDuring: ApplicationStatus
    ) {
        val expectedOtherGuardians = mutableSetOf(otherVtjGuardian.id, fosterParent.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)

        insertFosterParent(clock)
        executeTestApplicationProcess(clock) { status ->
            assertEquals(expectedOtherGuardians, getOtherGuardians())
            if (status == blockDuring) {
                db.transaction { it.blockGuardian(child.id, otherVtjGuardian.id) }
                expectedOtherGuardians -= otherVtjGuardian.id
            }
        }

        assertEquals(expectedOtherGuardians + guardian.id, getSfiMessageRecipients())
    }

    @ParameterizedTest(
        name =
            "sfi messages are sent to all guardians and foster parents - foster parent relationship expired during the process {0}"
    )
    @EnumSource(names = ["SENT", "WAITING_PLACEMENT", "WAITING_DECISION"])
    fun `sfi messages are sent to guardians - foster parent relationship expired during the process`(
        expireDuring: ApplicationStatus
    ) {
        val expectedOtherGuardians = mutableSetOf(otherVtjGuardian.id, fosterParent.id)
        val clock = MockEvakaClock(2024, 1, 1, 12, 0)

        val fosterParentRelationship = insertFosterParent(clock)
        executeTestApplicationProcess(clock) { status ->
            assertEquals(expectedOtherGuardians, getOtherGuardians())
            if (status == expireDuring) {
                db.transaction {
                    it.updateFosterParentRelationshipValidity(
                        fosterParentRelationship,
                        DateRange(clock.today(), clock.today()),
                        clock.now(),
                        Modifier.User(serviceWorker.evakaUserId),
                    )
                }
                clock.tick(Duration.ofDays(1))
                expectedOtherGuardians -= fosterParent.id
            }
        }

        assertEquals(expectedOtherGuardians + guardian.id, getSfiMessageRecipients())
    }

    private fun insertTestApplication(preferredStartDate: LocalDate): ApplicationId =
        db.transaction { tx ->
            tx.insertTestApplication(
                id = application,
                type = ApplicationType.DAYCARE,
                status = ApplicationStatus.CREATED,
                guardianId = guardian.id,
                childId = child.id,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        guardian = guardian.toDaycareFormAdult(),
                        child = child.toDaycareFormChild(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                        preferredStartDate = preferredStartDate,
                    ),
            )
        }

    private fun insertFosterParent(clock: EvakaClock) =
        db.transaction {
            it.insert(
                DevFosterParent(
                    childId = child.id,
                    parentId = fosterParent.id,
                    validDuring = DateRange(clock.today(), null),
                    modifiedAt = clock.now(),
                    modifiedBy = serviceWorker.evakaUserId,
                )
            )
        }

    private fun getOtherGuardians(): Set<PersonId> =
        db.read { it.getApplicationOtherGuardians(application) }

    private fun sendApplication(clock: EvakaClock) =
        db.transaction { tx ->
            applicationStateService.sendApplication(
                tx,
                guardian.user(CitizenAuthLevel.STRONG),
                clock,
                application,
            )
        }

    private fun moveToWaitingPlacement(clock: EvakaClock) =
        db.transaction { tx ->
            applicationStateService.moveToWaitingPlacement(
                tx,
                serviceWorker.user,
                clock,
                application,
            )
        }

    private fun createPlacementPlan(clock: EvakaClock, period: FiniteDateRange) =
        db.transaction { tx ->
            applicationStateService.createPlacementPlan(
                tx,
                serviceWorker.user,
                clock,
                application,
                DaycarePlacementPlan(daycare.id, period),
            )
        }

    private fun sendDecisionsWithoutProposal(clock: EvakaClock) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx,
                serviceWorker.user,
                clock,
                application,
            )
        }
    }

    private fun acceptDecisions(clock: EvakaClock) {
        db.transaction { tx ->
            tx.getDecisionsByApplication(application, AccessControlFilter.PermitAll).forEach {
                decision ->
                applicationStateService.acceptDecision(
                    tx,
                    guardian.user(CitizenAuthLevel.STRONG),
                    clock,
                    application,
                    decision.id,
                    requestedStartDate = decision.startDate,
                )
            }
        }
    }

    private fun getSfiMessageRecipients(): Set<PersonId> =
        MockSfiMessagesClient.getMessages()
            .map {
                when (it.ssn) {
                    guardian.ssn -> guardian.id
                    otherVtjGuardian.ssn -> otherVtjGuardian.id
                    fosterParent.ssn -> fosterParent.id
                    else -> error("Unknown Suomi.fi message recipient SSN $it.ssn")
                }
            }
            .toSet()

    // Creates a test application and performs the following state transitions on it:
    // CREATED -> SENT -> WAITING_PLACEMENT -> WAITING_DECISION -> WAITING_CONFIRMATION -> ACTIVE
    // The given callback is called after every transition
    private fun executeTestApplicationProcess(
        clock: EvakaClock,
        callback: (ApplicationStatus) -> Unit,
    ) {
        val preferredStartDate = LocalDate.of(2024, 2, 1)
        insertTestApplication(preferredStartDate)

        fun checkpoint() = callback(db.read { it.getApplicationStatus(application) })

        sendApplication(clock)
        checkpoint()
        moveToWaitingPlacement(clock)
        checkpoint()
        createPlacementPlan(
            clock,
            period = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1)),
        )
        checkpoint()
        sendDecisionsWithoutProposal(clock)
        asyncJobRunner.runPendingJobsSync(clock)
        checkpoint()
        acceptDecisions(clock)
        checkpoint()
    }
}
