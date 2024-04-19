// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.test.validPreschoolApplication
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

data class ManualDuplicationReportTestData(
    val areaId: AreaId,
    val daycareId: DaycareId,
    val preschoolId: DaycareId,
)

internal class ManualDuplicationReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var manualDuplicationReportController: ManualDuplicationReportController

    private val testRootTime = HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15))
    private val mockToday = MockEvakaClock(testRootTime)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)
    private val testChild = DevPerson(dateOfBirth = testRootTime.toLocalDate().minusYears(5))
    private val testGuardian = DevPerson()

    fun initTestData(): ManualDuplicationReportTestData {
        return db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)
            tx.insert(testChild, DevPersonType.RAW_ROW)
            tx.insert(testGuardian, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(testGuardian.id, testChild.id))
            val areaId = tx.insert(DevCareArea())
            val preschoolId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        type = setOf(CareType.PRESCHOOL),
                        openingDate = mockToday.today()
                    )
                )
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        type = setOf(CareType.CENTRE),
                        openingDate = mockToday.today()
                    )
                )

            addAcceptedPreschoolApplicationWithDecisions(
                tx = tx,
                child = testChild,
                guardian = testGuardian,
                preschoolId = preschoolId,
                daycareId = daycareId,
                sentDate = testRootTime.toLocalDate(),
                transferDecisionType = DecisionType.PRESCHOOL_DAYCARE,
            )

            ManualDuplicationReportTestData(areaId, daycareId, preschoolId)
        }
    }

    fun addAcceptedPreschoolApplicationWithDecisions(
        tx: Database.Transaction,
        child: DevPerson,
        guardian: DevPerson,
        sentDate: LocalDate,
        transferDecisionType: DecisionType,
        preschoolId: DaycareId,
        daycareId: DaycareId = preschoolId,
    ) {
        val applicationId = ApplicationId(UUID.randomUUID())

        tx.insertTestApplication(
            id = applicationId,
            type = ApplicationType.PRESCHOOL,
            status = ApplicationStatus.SENT,
            guardianId = guardian.id,
            childId = child.id,
            hideFromGuardian = false,
            transferApplication = false,
            allowOtherGuardianAccess = true,
            sentDate = sentDate,
            dueDate = null
        )
        tx.insertTestApplicationForm(
            applicationId = applicationId,
            document =
                DaycareFormV0.fromApplication2(validPreschoolApplication)
                    .copy(child = Child(dateOfBirth = child.dateOfBirth))
        )

        tx.insertTestDecision(
            TestDecision(
                createdBy = EvakaUserId(admin.id.raw),
                unitId = preschoolId,
                applicationId = applicationId,
                type = DecisionType.PRESCHOOL,
                startDate = testRootTime.toLocalDate(),
                endDate = testRootTime.toLocalDate().plusMonths(5),
                status = DecisionStatus.ACCEPTED,
                sentDate = sentDate
            )
        )
        tx.insertTestDecision(
            TestDecision(
                createdBy = EvakaUserId(admin.id.raw),
                unitId = daycareId,
                applicationId = applicationId,
                type = transferDecisionType,
                startDate = testRootTime.toLocalDate(),
                endDate = testRootTime.toLocalDate().plusMonths(5),
                status = DecisionStatus.ACCEPTED,
                sentDate = sentDate
            )
        )
    }

    @Test
    fun `Report finds duplication need for connected daycare decision`() {
        val testData = initTestData()

        val duplicationNeeds =
            manualDuplicationReportController.getManualDuplicationReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                ManualDuplicationReportViewMode.NONDUPLICATED
            )

        assertEquals(1, duplicationNeeds.size)
        assertEquals(testChild.id, duplicationNeeds[0].childId)
        assertEquals(testData.daycareId, duplicationNeeds[0].connectedDaycareId)
        assertEquals(testData.preschoolId, duplicationNeeds[0].preschoolDaycareId)
    }

    @Test
    fun `Report finds duplicated case for connected daycare decision`() {
        val testData = initTestData()
        db.transaction { tx ->
            tx.insert(DevPerson(duplicateOf = testChild.id), DevPersonType.RAW_ROW)
        }

        val duplicatedCases =
            manualDuplicationReportController.getManualDuplicationReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                ManualDuplicationReportViewMode.DUPLICATED
            )

        assertEquals(1, duplicatedCases.size)
        assertEquals(testChild.id, duplicatedCases[0].childId)
        assertEquals(testData.daycareId, duplicatedCases[0].connectedDaycareId)
        assertEquals(testData.preschoolId, duplicatedCases[0].preschoolDaycareId)
    }

    @Test
    fun `Report does not show a case when a new decision invalidates duplication need`() {
        val testData = initTestData()
        val laterDecisionDate = testRootTime.toLocalDate().plusDays(7)

        // add another overlapping application + decisions that do not constitute a duplication need
        db.transaction { tx ->
            addAcceptedPreschoolApplicationWithDecisions(
                tx = tx,
                child = testChild,
                guardian = testGuardian,
                preschoolId = testData.preschoolId,
                sentDate = laterDecisionDate,
                transferDecisionType = DecisionType.PRESCHOOL_CLUB,
            )
        }

        val duplicationNeeds =
            manualDuplicationReportController.getManualDuplicationReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                ManualDuplicationReportViewMode.NONDUPLICATED
            )

        assertEquals(0, duplicationNeeds.size)
    }

    @Test
    fun `Report still shows a duplication case even when a new decision invalidates original duplication need`() {
        val testData = initTestData()
        val laterDecisionDate = testRootTime.toLocalDate().plusDays(7)

        db.transaction { tx ->
            // add another overlapping application + decisions that do not constitute a duplication
            // need...
            addAcceptedPreschoolApplicationWithDecisions(
                tx = tx,
                child = testChild,
                guardian = testGuardian,
                preschoolId = testData.preschoolId,
                sentDate = laterDecisionDate,
                transferDecisionType = DecisionType.PRESCHOOL_CLUB,
            )
            // ...however the case has already been duplicated
            tx.insert(DevPerson(duplicateOf = testChild.id), DevPersonType.RAW_ROW)
        }

        val duplicatedCases =
            manualDuplicationReportController.getManualDuplicationReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                ManualDuplicationReportViewMode.DUPLICATED
            )

        assertEquals(1, duplicatedCases.size)
        assertEquals(testChild.id, duplicatedCases[0].childId)
        assertEquals(testData.daycareId, duplicatedCases[0].connectedDaycareId)
        assertEquals(testData.preschoolId, duplicatedCases[0].preschoolDaycareId)
    }

    @Test
    fun `Report only shows the latest duplication need in case of transfer`() {
        val testData = initTestData()
        val laterDecisionDate = testRootTime.toLocalDate().plusDays(7)

        val transferDaycareId = DaycareId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDaycare(
                    id = transferDaycareId,
                    areaId = testData.areaId,
                    type = setOf(CareType.CENTRE),
                    openingDate = mockToday.today()
                )
            )
            // add another overlapping application + decisions that do constitute a duplication need
            addAcceptedPreschoolApplicationWithDecisions(
                tx = tx,
                child = testChild,
                guardian = testGuardian,
                preschoolId = testData.preschoolId,
                daycareId = transferDaycareId,
                sentDate = laterDecisionDate,
                transferDecisionType = DecisionType.PRESCHOOL_DAYCARE,
            )
        }

        val duplicationNeeds =
            manualDuplicationReportController.getManualDuplicationReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                ManualDuplicationReportViewMode.NONDUPLICATED
            )

        assertEquals(1, duplicationNeeds.size)
        assertEquals(testChild.id, duplicationNeeds[0].childId)
        assertEquals(transferDaycareId, duplicationNeeds[0].connectedDaycareId)
        assertEquals(testData.preschoolId, duplicationNeeds[0].preschoolDaycareId)
    }
}
