// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.AcceptDecisionRequest
import fi.espoo.evaka.application.ApplicationControllerCitizen
import fi.espoo.evaka.application.ApplicationControllerV2
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.SimpleApplicationAction
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployee
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevVoucherValueDecision
import fi.espoo.evaka.shared.dev.emptyAssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MigrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var applicationController: ApplicationControllerV2
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen

    @Test
    fun `non-sent application is not migrated`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)

        val adult = DevPerson()
        val child = DevPerson()

        val applicationId =
            db.transaction { tx ->
                tx.insert(employee)
                tx.insert(area)
                tx.insert(unit)
                tx.insert(adult, DevPersonType.ADULT)
                tx.insert(child, DevPersonType.CHILD)

                tx.insertTestApplication(
                    status = ApplicationStatus.CREATED,
                    sentDate = null,
                    dueDate = null,
                    guardianId = adult.id,
                    childId = child.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            guardian = adult.toDaycareFormAdult(),
                            child = child.toDaycareFormChild(),
                            apply = Apply(preferredUnits = listOf(unit.id)),
                            preferredStartDate = today.plusMonths(4),
                        ),
                )
            }

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByApplicationId(applicationId) }
        assertNull(process)
    }

    @Test
    fun `sent application is migrated to INITIAL state`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)

        val adult = DevPerson()
        val child = DevPerson()

        val applicationId =
            db.transaction { tx ->
                tx.insert(employee)
                tx.insert(area)
                tx.insert(unit)
                tx.insert(adult, DevPersonType.ADULT)
                tx.insert(child, DevPersonType.CHILD)

                tx.insertTestApplication(
                    status = ApplicationStatus.CREATED,
                    sentDate = today,
                    dueDate = null,
                    guardianId = adult.id,
                    childId = child.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            guardian = adult.toDaycareFormAdult(),
                            child = child.toDaycareFormChild(),
                            apply = Apply(preferredUnits = listOf(unit.id)),
                            preferredStartDate = today.plusMonths(4),
                        ),
                )
            }

        applicationController.sendApplication(dbInstance(), employee.user, clock, applicationId)
        clearApplicationMetadata()

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByApplicationId(applicationId) }!!
        assertEquals("123.123.a", process.processDefinitionNumber)
        assertEquals(today.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)

        assertEquals(1, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(HelsinkiDateTime.of(today, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `accepted application is migrated to COMPLETED state`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)

        val adult = DevPerson()
        val child = DevPerson()

        val applicationId =
            db.transaction { tx ->
                tx.insert(employee)
                tx.insert(area)
                tx.insert(unit)
                tx.insert(adult, DevPersonType.ADULT)
                tx.insert(child, DevPersonType.CHILD)

                tx.insertTestApplication(
                    status = ApplicationStatus.CREATED,
                    sentDate = today,
                    dueDate = null,
                    guardianId = adult.id,
                    childId = child.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            guardian = adult.toDaycareFormAdult(),
                            child = child.toDaycareFormChild(),
                            apply = Apply(preferredUnits = listOf(unit.id)),
                            preferredStartDate = today.plusMonths(4),
                        ),
                )
            }

        applicationController.sendApplication(dbInstance(), employee.user, clock, applicationId)
        clock.tick(Duration.ofDays(1))

        applicationController.simpleApplicationAction(
            dbInstance(),
            employee.user,
            clock,
            applicationId,
            SimpleApplicationAction.MOVE_TO_WAITING_PLACEMENT,
        )
        applicationController.createPlacementPlan(
            dbInstance(),
            employee.user,
            clock,
            applicationId,
            DaycarePlacementPlan(unit.id, FiniteDateRange(today.plusMonths(4), today.plusMonths(5))),
        )
        applicationController.simpleApplicationAction(
            dbInstance(),
            employee.user,
            clock,
            applicationId,
            SimpleApplicationAction.SEND_DECISIONS_WITHOUT_PROPOSAL,
        )
        applicationController.simpleApplicationAction(
            dbInstance(),
            employee.user,
            clock,
            applicationId,
            SimpleApplicationAction.CONFIRM_DECISION_MAILED,
        )
        val decision =
            db.read {
                it.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll).single()
            }
        applicationControllerCitizen.acceptDecision(
            dbInstance(),
            adult.user(CitizenAuthLevel.STRONG),
            clock,
            applicationId,
            AcceptDecisionRequest(
                decisionId = decision.id,
                requestedStartDate = today.plusMonths(4),
            ),
        )
        clearApplicationMetadata()

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByApplicationId(applicationId) }!!
        assertEquals("123.123.a", process.processDefinitionNumber)
        assertEquals(today.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)

        assertEquals(2, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(HelsinkiDateTime.of(today, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history.last().also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(clock.now(), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `draft fee decision is not migrated`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val adult = DevPerson()
        val feeDecision =
            DevFeeDecision(
                status = FeeDecisionStatus.DRAFT,
                validDuring = FiniteDateRange(today, today.plusMonths(1)),
                headOfFamilyId = adult.id,
            )

        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(feeDecision)
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByFeeDecisionId(feeDecision.id) }
        assertNull(process)
    }

    @Test
    fun `approved fee decision is migrated to DECIDING`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val approved = HelsinkiDateTime.of(today, LocalTime.of(9, 30))

        val adult = DevPerson()
        val feeDecision =
            DevFeeDecision(
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedAt = approved,
                validDuring = FiniteDateRange(today, today.plusMonths(1)),
                headOfFamilyId = adult.id,
            )

        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(feeDecision)
            tx.execute { sql("UPDATE fee_decision SET created = ${bind(created)}") }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByFeeDecisionId(feeDecision.id) }!!
        assertEquals("123.789.a", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(2, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history.last().also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(approved, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `sent fee decision is migrated to COMPLETED`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val approved = HelsinkiDateTime.of(today, LocalTime.of(9, 30))
        val approvedBy = DevEmployee()
        val sent = HelsinkiDateTime.of(today, LocalTime.of(10, 0))

        val adult = DevPerson()
        val feeDecision =
            DevFeeDecision(
                status = FeeDecisionStatus.SENT,
                approvedAt = approved,
                approvedById = approvedBy.evakaUserId,
                sentAt = sent,
                validDuring = FiniteDateRange(today, today.plusMonths(1)),
                headOfFamilyId = adult.id,
            )

        db.transaction { tx ->
            tx.insert(approvedBy)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(feeDecision)
            tx.execute { sql("UPDATE fee_decision SET created = ${bind(created)}") }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process = db.read { it.getArchiveProcessByFeeDecisionId(feeDecision.id) }!!
        assertEquals("123.789.a", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(3, process.history.size)
        process.history[0].also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[1].also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(approved, it.enteredAt)
            assertEquals(approvedBy.evakaUserId, it.enteredBy.id)
        }
        process.history[2].also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(sent, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `draft voucher value decision is not migrated`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val adult = DevPerson()
        val child = DevPerson()
        val voucherValueDecision =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.DRAFT,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child.id,
                placementUnitId = daycare.id,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(voucherValueDecision)
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision.id) }
        assertNull(process)
    }

    @Test
    fun `approved voucher value decision decision is migrated to DECIDING`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val approved = HelsinkiDateTime.of(today, LocalTime.of(9, 30))

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val adult = DevPerson()
        val child = DevPerson()
        val voucherValueDecision =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedAt = approved,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child.id,
                placementUnitId = daycare.id,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(voucherValueDecision)
            tx.execute { sql("UPDATE voucher_value_decision SET created = ${bind(created)}") }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision.id) }!!
        assertEquals("123.789.b", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(2, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history.last().also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(approved, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `sent voucher value decision is migrated to COMPLETED`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val approved = HelsinkiDateTime.of(today, LocalTime.of(9, 30))
        val approvedBy = DevEmployee()
        val sent = HelsinkiDateTime.of(today, LocalTime.of(10, 0))

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val adult = DevPerson()
        val child = DevPerson()
        val voucherValueDecision =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.SENT,
                approvedAt = approved,
                approvedBy = approvedBy.evakaUserId,
                sentAt = sent,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child.id,
                placementUnitId = daycare.id,
            )

        db.transaction { tx ->
            tx.insert(approvedBy)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(voucherValueDecision)
            tx.execute { sql("UPDATE voucher_value_decision SET created = ${bind(created)}") }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision.id) }!!
        assertEquals("123.789.b", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(120, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(3, process.history.size)
        process.history[0].also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[1].also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(approved, it.enteredAt)
            assertEquals(approvedBy.evakaUserId, it.enteredBy.id)
        }
        process.history[2].also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(sent, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `draft assistance need daycare decision is migrated to INITIAL`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))

        val child = DevPerson()
        val assistanceNeedDecision =
            DevAssistanceNeedDecision(childId = child.id, validityPeriod = DateRange(today, null))

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(assistanceNeedDecision)
            tx.execute { sql("UPDATE assistance_need_decision SET created_at = ${bind(created)}") }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read { it.getArchiveProcessByAssistanceNeedDecisionId(assistanceNeedDecision.id) }!!
        assertEquals("123.456.a", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(1440, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(1, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `sent assistance need daycare decision is migrated to COMPLETED`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val sentForDecision = today.plusDays(1)
        val decisionMade = today.plusDays(2)

        val employee = DevEmployee()
        val child = DevPerson()
        val assistanceNeedDecision =
            DevAssistanceNeedDecision(
                childId = child.id,
                status = AssistanceNeedDecisionStatus.ACCEPTED,
                validityPeriod = DateRange(today, null),
                decisionMaker =
                    AssistanceNeedDecisionEmployee(employeeId = employee.id, null, null, null),
                sentForDecision = sentForDecision,
                decisionMade = decisionMade,
            )

        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(assistanceNeedDecision)
            tx.execute { sql("UPDATE assistance_need_decision SET created_at = ${bind(created)}") }
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.CreateAssistanceNeedDecisionPdf(assistanceNeedDecision.id)),
                runAt = now,
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read { it.getArchiveProcessByAssistanceNeedDecisionId(assistanceNeedDecision.id) }!!
        assertEquals("123.456.a", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(1440, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(4, process.history.size)
        process.history[0].also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[1].also {
            assertEquals(ArchivedProcessState.PREPARATION, it.state)
            assertEquals(HelsinkiDateTime.of(sentForDecision, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[2].also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(HelsinkiDateTime.of(decisionMade, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(employee.evakaUserId, it.enteredBy.id)
        }
        process.history[3].also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(HelsinkiDateTime.of(decisionMade, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `draft assistance need preschool decision is migrated to INITIAL`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))

        val child = DevPerson()
        val assistanceNeedPreschoolDecision =
            DevAssistanceNeedPreschoolDecision(
                childId = child.id,
                form = emptyAssistanceNeedPreschoolDecisionForm,
            )

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(assistanceNeedPreschoolDecision)
            tx.execute {
                sql("UPDATE assistance_need_preschool_decision SET created_at = ${bind(created)}")
            }
        }

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read {
                it.getArchiveProcessByAssistanceNeedPreschoolDecisionId(
                    assistanceNeedPreschoolDecision.id
                )
            }!!
        assertEquals("123.456.b", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(1440, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(1, process.history.size)
        process.history.first().also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `sent assistance need preschool decision is migrated to COMPLETED`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val created = HelsinkiDateTime.of(today, LocalTime.of(9, 0))
        val sentForDecision = today.plusDays(1)
        val decisionMade = today.plusDays(2)

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val employee = DevEmployee()
        val child = DevPerson()
        val assistanceNeedPreschoolDecision =
            DevAssistanceNeedPreschoolDecision(
                childId = child.id,
                status = AssistanceNeedDecisionStatus.ACCEPTED,
                form =
                    emptyAssistanceNeedPreschoolDecisionForm.copy(
                        validFrom = today,
                        selectedUnit = daycare.id,
                        preparer1EmployeeId = employee.id,
                        decisionMakerEmployeeId = employee.id,
                    ),
                sentForDecision = sentForDecision,
                decisionMade = decisionMade,
                unreadGuardianIds = setOf(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(assistanceNeedPreschoolDecision)
            tx.execute {
                sql("UPDATE assistance_need_preschool_decision SET created_at = ${bind(created)}")
            }
            asyncJobRunner.plan(
                tx,
                listOf(
                    AsyncJob.CreateAssistanceNeedPreschoolDecisionPdf(
                        assistanceNeedPreschoolDecision.id
                    )
                ),
                runAt = now,
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        migrateProcessMetadata(db, clock, featureConfig)

        val process =
            db.read {
                it.getArchiveProcessByAssistanceNeedPreschoolDecisionId(
                    assistanceNeedPreschoolDecision.id
                )
            }!!
        assertEquals("123.456.b", process.processDefinitionNumber)
        assertEquals(created.year, process.year)
        assertEquals(1, process.number)
        assertEquals(featureConfig.archiveMetadataOrganization, process.organization)
        assertEquals(1440, process.archiveDurationMonths)
        assertTrue(process.migrated)
        assertEquals(4, process.history.size)
        process.history[0].also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(created, it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[1].also {
            assertEquals(ArchivedProcessState.PREPARATION, it.state)
            assertEquals(HelsinkiDateTime.of(sentForDecision, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
        process.history[2].also {
            assertEquals(ArchivedProcessState.DECIDING, it.state)
            assertEquals(HelsinkiDateTime.of(decisionMade, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(employee.evakaUserId, it.enteredBy.id)
        }
        process.history[3].also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(HelsinkiDateTime.of(decisionMade, LocalTime.MIDNIGHT), it.enteredAt)
            assertEquals(AuthenticatedUser.SystemInternalUser.evakaUserId, it.enteredBy.id)
        }
    }

    @Test
    fun `migration works in batches`() {
        val today = LocalDate.of(2023, 1, 1)
        val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        val clock = MockEvakaClock(now)

        val approved = HelsinkiDateTime.of(today, LocalTime.of(9, 30))
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val adult = DevPerson()
        val child1 = DevPerson()
        val child2 = DevPerson()
        val child3 = DevPerson()
        val voucherValueDecision1 =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedAt = approved,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child1.id,
                placementUnitId = daycare.id,
            )
        val voucherValueDecision2 =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedAt = approved,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child2.id,
                placementUnitId = daycare.id,
            )
        val voucherValueDecision3 =
            DevVoucherValueDecision(
                status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedAt = approved,
                validFrom = today,
                validTo = today.plusMonths(1),
                headOfFamilyId = adult.id,
                childId = child3.id,
                placementUnitId = daycare.id,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(child3, DevPersonType.CHILD)
            tx.insert(voucherValueDecision1)
            tx.insert(voucherValueDecision2)
            tx.insert(voucherValueDecision3)
        }

        migrateProcessMetadata(db, clock, featureConfig, batchSize = 2)

        assertNotNull(
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision1.id) }
        )
        assertNotNull(
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision2.id) }
        )
        assertNotNull(
            db.read { it.getArchiveProcessByVoucherValueDecisionId(voucherValueDecision3.id) }
        )
    }

    private fun clearApplicationMetadata() {
        // Test setup creates metadata, so we need to clear it before running the migration
        db.transaction { tx ->
            tx.execute { sql("UPDATE application SET process_id = NULL") }
            tx.execute { sql("DELETE FROM archived_process_history") }
            tx.execute { sql("DELETE FROM archived_process") }
        }
    }
}
