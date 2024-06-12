// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.syncApplicationOtherGuardians
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionType
import fi.espoo.evaka.incomestatement.IncomeStatementType
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.insertMessage
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.messaging.insertRecipients
import fi.espoo.evaka.messaging.insertThread
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.pis.service.deleteGuardianRelationship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InactivePeopleCleanupIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testDate = LocalDate.of(2020, 3, 1)
    private val testUnit = testDaycare

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(testArea)
            it.insert(DevDaycare(id = testUnit.id, name = testUnit.name, areaId = testArea.id))
        }
    }

    @Test
    fun `adult with no family is cleaned up`() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id))
    }

    @Test
    fun `guardian and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.RAW_ROW)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testChild_1.id))
    }

    @Test
    fun `blocked guardian is not cleaned up without any archived data`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.RAW_ROW)
            tx.blockGuardian(testChild_1.id, testAdult_1.id)
        }

        assertCleanedUpPeople(testDate, setOf(testChild_1.id))
    }

    @Test
    fun `head of family and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.RAW_ROW)
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testChild_1.id))
    }

    @Test
    fun `head of family and their partner are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            tx.insertTestPartnership(adult1 = testAdult_1.id, adult2 = testAdult_2.id)
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testAdult_2.id))
    }

    @Test
    fun `adult with no family that has logged in recently is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusMonths(2))} WHERE id = ${bind(testAdult_1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `guardian and their child are not cleaned up when guardian has logged in recently`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.RAW_ROW)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusDays(14))} WHERE id = ${bind(testAdult_1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult who is saved in the application other guardian table is not cleaned up`() {
        lateinit var otherGuardian: PersonId
        db.transaction { tx ->
            val applicationOwner = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            otherGuardian = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val child = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = applicationOwner, childId = child))
            tx.insert(DevGuardian(guardianId = otherGuardian, childId = child))
            val application =
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = applicationOwner,
                    childId = child,
                    document = DaycareFormV0.fromApplication2(validDaycareApplication)
                )
            tx.syncApplicationOtherGuardians(application, testDate)
            tx.deleteGuardianRelationship(childId = child, guardianId = otherGuardian)
        }
        assertCleanedUpPeople(testDate, setOf())

        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "DELETE FROM application_other_guardian WHERE guardian_id = ${bind(otherGuardian)}"
                    )
                }
                .execute()
        }
        assertCleanedUpPeople(testDate, setOf(otherGuardian))
    }

    @Test
    fun `head of family and their child are not cleaned up when child has a placement`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(DevPlacement(childId = testChild_1.id, unitId = testUnit.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testChild_2, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(DevPlacement(childId = testChild_1.id, unitId = testUnit.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children and two adults is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testChild_2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = testAdult_1.id, adult2 = testAdult_2.id)
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(DevPlacement(childId = testChild_1.id, unitId = testUnit.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children with separate heads of family is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testChild_2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = testAdult_1.id, adult2 = testAdult_2.id)
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )
            )
            tx.insert(DevPlacement(childId = testChild_1.id, unitId = testUnit.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with income statement is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(
                DevIncomeStatement(
                    IncomeStatementId(UUID.randomUUID()),
                    testAdult_1.id,
                    LocalDate.now(),
                    IncomeStatementType.INCOME,
                    42
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with received messages is cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisorId = EmployeeId(UUID.randomUUID())
            tx.insert(
                DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor")
            )
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisorId)

            tx.insert(testAdult_1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(testAdult_1.id)

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = employeeAccount,
                    sentAt = now,
                    recipientNames = listOf("recipient name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus"
                )
            tx.insertRecipients(listOf(messageId to setOf(personAccount)))
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id))
    }

    @Test
    fun `adult with sent messages is not cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisorId = EmployeeId(UUID.randomUUID())
            tx.insert(
                DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor")
            )
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisorId)

            tx.insert(testAdult_1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(testAdult_1.id)

            val contentId = tx.insertMessageContent("content", personAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = personAccount,
                    sentAt = now,
                    recipientNames = listOf("employee name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus"
                )
            tx.insertRecipients(listOf(messageId to setOf(employeeAccount)))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with assistance need decision is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = testAdult_1.id, childId = testChild_1.id))
            tx.insertTestAssistanceNeedDecision(
                childId = testChild_1.id,
                DevAssistanceNeedDecision(
                    decisionNumber = 999,
                    childId = testChild_1.id,
                    validityPeriod = DateRange.ofMonth(2019, Month.JANUARY),
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    language = OfficialLanguage.FI,
                    decisionMade = null,
                    sentForDecision = null,
                    selectedUnit = null,
                    preparedBy1 = null,
                    preparedBy2 = null,
                    decisionMaker = null,
                    pedagogicalMotivation = null,
                    structuralMotivationOptions =
                        StructuralMotivationOptions(false, false, false, false, false, false),
                    structuralMotivationDescription = null,
                    careMotivation = null,
                    serviceOptions = ServiceOptions(false, false, false, false, false),
                    servicesMotivation = null,
                    expertResponsibilities = null,
                    guardiansHeardOn = null,
                    guardianInfo = emptySet(),
                    viewOfGuardians = null,
                    otherRepresentativeHeard = false,
                    otherRepresentativeDetails = null,
                    assistanceLevels = emptySet(),
                    motivationForDecision = null,
                    unreadGuardianIds = null,
                    annulmentReason = "",
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with assistance need preschool decision is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = testAdult_1.id, childId = testChild_1.id))
            val employeeId = EmployeeId(UUID.randomUUID())
            tx.insert(
                DevEmployee(id = employeeId, firstName = "Firstname", lastName = "Supervisor")
            )
            tx.insertTestAssistanceNeedPreschoolDecision(
                DevAssistanceNeedPreschoolDecision(
                    decisionNumber = 999,
                    childId = testChild_1.id,
                    form =
                        AssistanceNeedPreschoolDecisionForm(
                            language = OfficialLanguage.FI,
                            type = AssistanceNeedPreschoolDecisionType.NEW,
                            validFrom = LocalDate.of(2019, 1, 1),
                            validTo = null,
                            extendedCompulsoryEducation = false,
                            extendedCompulsoryEducationInfo = "",
                            grantedAssistanceService = false,
                            grantedInterpretationService = false,
                            grantedAssistiveDevices = false,
                            grantedServicesBasis = "",
                            selectedUnit = testDaycare.id,
                            primaryGroup = "",
                            decisionBasis = "",
                            basisDocumentPedagogicalReport = false,
                            basisDocumentPsychologistStatement = false,
                            basisDocumentSocialReport = false,
                            basisDocumentDoctorStatement = false,
                            basisDocumentPedagogicalReportDate = null,
                            basisDocumentPsychologistStatementDate = null,
                            basisDocumentSocialReportDate = null,
                            basisDocumentDoctorStatementDate = null,
                            basisDocumentOtherOrMissing = false,
                            basisDocumentOtherOrMissingInfo = "",
                            basisDocumentsInfo = "",
                            guardiansHeardOn = null,
                            guardianInfo = emptySet(),
                            otherRepresentativeHeard = false,
                            otherRepresentativeDetails = "",
                            viewOfGuardians = "",
                            preparer1EmployeeId = employeeId,
                            preparer1Title = "",
                            preparer1PhoneNumber = "",
                            preparer2EmployeeId = null,
                            preparer2Title = "",
                            preparer2PhoneNumber = "",
                            decisionMakerEmployeeId = employeeId,
                            decisionMakerTitle = "",
                        ),
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    annulmentReason = "",
                    sentForDecision = null,
                    decisionMade = LocalDate.of(2019, 5, 1),
                    unreadGuardianIds = emptySet(),
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    private fun assertCleanedUpPeople(queryDate: LocalDate, cleanedUpPeople: Set<PersonId>) {
        val result = db.transaction { cleanUpInactivePeople(it, queryDate) }

        assertEquals(cleanedUpPeople, result)
    }
}
