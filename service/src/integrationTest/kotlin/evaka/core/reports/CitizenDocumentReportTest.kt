// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.daycare.CareType
import evaka.core.document.CITIZEN_DOCUMENT_CREATION_DAYS_BEFORE_PLACEMENT
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplate
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.RadioButtonGroupQuestionOption
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevBackupCare
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertEmployeeToDaycareGroupAcl
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class CitizenDocumentReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var citizenDocumentResponseReport: CitizenDocumentResponseReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val unitSuper = DevEmployee(firstName = "Unit", lastName = "Super")
    private val staff = DevEmployee(firstName = "Staff", lastName = "Staff")
    private val otherUnitSuper = DevEmployee(firstName = "Unit", lastName = "Super")
    private val otherGroupStaff = DevEmployee(firstName = "Staff", lastName = "Staff")

    private final val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(12, 15)))

    private val startDate = LocalDate.of(2024, 9, 15)

    @Test
    fun `Admin can see citizen document report results`() {
        val templateData = initTemplates(startDate)
        val testUnitData = initTestUnitData(startDate)
        val unitAData = testUnitData.first()
        initTestDocumentData(
            startDate,
            unitAData,
            testUnitData[1],
            templateData.first().toDocumentTemplate(),
        )

        assertDoesNotThrow {
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                admin.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )
        }
    }

    @Test
    fun `Only same unit supervisor can see citizen document report results`() {
        val templateData = initTemplates(startDate)
        val testUnitData = initTestUnitData(startDate)
        val unitAData = testUnitData.first()
        initTestDocumentData(
            startDate,
            unitAData,
            testUnitData[1],
            templateData.first().toDocumentTemplate(),
        )

        assertDoesNotThrow {
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                unitSuper.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )
        }

        assertThrows<Forbidden> {
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                otherUnitSuper.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )
        }
    }

    @Test
    fun `Only same group staff can see citizen document report results`() {
        val templateData = initTemplates(startDate)
        val testUnitData = initTestUnitData(startDate)
        val unitAData = testUnitData.first()
        initTestDocumentData(
            startDate,
            unitAData,
            testUnitData[1],
            templateData.first().toDocumentTemplate(),
        )

        assertDoesNotThrow {
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                staff.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )
        }

        assertThrows<Forbidden> {
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                otherGroupStaff.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )
        }
    }

    @Test
    fun `Citizen document report results are correct`() {
        val templateData = initTemplates(startDate)
        val testUnitData = initTestUnitData(startDate)
        val unitAData = testUnitData.first()
        val testPersonData =
            initTestDocumentData(
                startDate,
                unitAData,
                testUnitData[1],
                templateData.first().toDocumentTemplate(),
            )

        val results =
            citizenDocumentResponseReport.getCitizenDocumentResponseReport(
                dbInstance(),
                mockClock,
                admin.user,
                unitId = unitAData.first.id,
                groupId = unitAData.second.first().id,
                documentTemplateId = templateData.first().id,
            )

        val expectedResults = testPersonData.map {
            CitizenDocumentResponseReportRow(
                childId = it.first.id,
                answeredAt = it.second?.answeredAt,
                firstName = it.first.firstName,
                lastName = it.first.lastName,
                documentStatus = it.second?.status,
                documentContent = it.second?.content,
                isBackup = it.first.email == "demetrius@notadomain",
            )
        }

        assertThat(results).containsExactlyInAnyOrderElementsOf(expectedResults)
    }

    private fun initTemplates(startDate: LocalDate): List<DevDocumentTemplate> {
        val citizenDocumentTemplate =
            DevDocumentTemplate(
                name = "CitizenDocument",
                type = ChildDocumentType.CITIZEN_BASIC,
                language = UiLanguage.FI,
                validity = DateRange(startDate, startDate.plusYears(1)),
                content =
                    DocumentTemplateContent(
                        sections =
                            listOf(
                                Section(
                                    id = "a",
                                    label = "Document",
                                    infoText = "Document infotext",
                                    questions =
                                        listOf(
                                            Question.CheckboxQuestion(
                                                id = "q1",
                                                label = "Question 1",
                                                infoText = "Question 1 text",
                                            ),
                                            Question.RadioButtonGroupQuestion(
                                                id = "q2",
                                                label = "Question 2",
                                                infoText = "Question 2 text",
                                                options =
                                                    listOf(
                                                        RadioButtonGroupQuestionOption(
                                                            id = "rbgqo1",
                                                            label = "Radio 1",
                                                        ),
                                                        RadioButtonGroupQuestionOption(
                                                            id = "rbgqo2",
                                                            label = "Radio 2",
                                                        ),
                                                    ),
                                            ),
                                        ),
                                )
                            )
                    ),
            )
        return db.transaction { tx ->
            tx.insert(citizenDocumentTemplate)
            listOf(citizenDocumentTemplate)
        }
    }

    private fun initTestUnitData(start: LocalDate): List<Pair<DevDaycare, List<DevDaycareGroup>>> {
        return db.transaction { tx ->
            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val daycare =
                DevDaycare(
                    name = "Daycare A",
                    areaId = areaAId,
                    openingDate = start,
                    type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
                    dailyPreschoolTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                    operationTimes =
                        List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                            List(2) { null },
                    enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
                )

            val shiftDaycare =
                DevDaycare(
                    name = "Shiftdaycare B",
                    areaId = areaAId,
                    openingDate = start,
                    type = setOf(CareType.CENTRE),
                    operationTimes =
                        List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                            List(2) { null },
                    shiftCareOperationTimes =
                        List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                    shiftCareOpenOnHolidays = true,
                    enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
                )

            tx.insert(daycare)
            tx.insert(shiftDaycare)

            val groupA1 =
                DevDaycareGroup(daycareId = daycare.id, name = "Ryhmä 1", startDate = start)

            val groupA2 =
                DevDaycareGroup(daycareId = daycare.id, name = "Ryhmä 2", startDate = start)

            val groupB1 =
                DevDaycareGroup(daycareId = shiftDaycare.id, name = "Ryhmä 1", startDate = start)

            listOf(groupA1, groupA2, groupB1).forEach { tx.insert(it) }

            listOf(admin, unitSuper, staff, otherUnitSuper, otherGroupStaff).forEach {
                tx.insert(it)
            }

            tx.insertDaycareAclRow(daycare.id, unitSuper.id, role = UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(daycare.id, staff.id, role = UserRole.STAFF)
            tx.insertEmployeeToDaycareGroupAcl(groupA1.id, staff.id)

            tx.insertDaycareAclRow(
                shiftDaycare.id,
                otherUnitSuper.id,
                role = UserRole.UNIT_SUPERVISOR,
            )
            tx.insertDaycareAclRow(daycare.id, otherGroupStaff.id, role = UserRole.STAFF)
            tx.insertEmployeeToDaycareGroupAcl(groupA2.id, staff.id)

            listOf(Pair(daycare, listOf(groupA1, groupA2)), Pair(shiftDaycare, listOf(groupB1)))
        }
    }

    private fun initTestDocumentData(
        start: LocalDate,
        daycareData: Pair<DevDaycare, List<DevDaycareGroup>>,
        secondaryDaycareData: Pair<DevDaycare, List<DevDaycareGroup>>,
        template: DocumentTemplate,
        defaultPlacementDuration: FiniteDateRange = FiniteDateRange(start, start.plusMonths(6)),
    ): List<Pair<DevPerson, DevChildDocument?>> {
        return db.transaction { tx ->
            val templateContent = template.content.sections.first()
            val affirmativeDocumentContent =
                DocumentContent(
                    answers =
                        listOf(
                            AnsweredQuestion.CheckboxAnswer(
                                questionId = templateContent.questions[0].id,
                                answer = true,
                            ),
                            AnsweredQuestion.RadioButtonGroupAnswer(
                                questionId = templateContent.questions[1].id,
                                answer = "rbgqo1",
                            ),
                        )
                )

            val negativeDocumentContent =
                DocumentContent(
                    answers =
                        listOf(
                            AnsweredQuestion.CheckboxAnswer(
                                questionId = templateContent.questions[0].id,
                                answer = false,
                            ),
                            AnsweredQuestion.RadioButtonGroupAnswer(
                                questionId = templateContent.questions[1].id,
                                answer = "rbgqo2",
                            ),
                        )
                )

            // Test case Aapo:
            // placed in group A1, 2 answers, latest (true, Radio 1)
            val testChildAapo =
                DevPerson(
                    dateOfBirth = start.minusYears(2),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val placementA =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementA)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementA.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementA.startDate,
                    endDate = placementA.endDate,
                )
            )

            val aapoLatestResponse =
                DevChildDocument(
                    templateId = template.id,
                    createdAt = mockClock.now(),
                    childId = testChildAapo.id,
                    status = DocumentStatus.COMPLETED,
                    content = affirmativeDocumentContent,
                    contentLockedAt = mockClock.now(),
                    modifiedAt = mockClock.now(),
                    modifiedBy = admin.evakaUserId,
                    statusModifiedAt = mockClock.now(),
                    answeredAt = mockClock.now(),
                    contentLockedBy = admin.id,
                    answeredBy = admin.evakaUserId,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = mockClock.now(),
                                createdBy = admin.evakaUserId,
                                publishedContent = affirmativeDocumentContent,
                            )
                        ),
                )
            val aapoEarlierResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    createdAt = mockClock.now().minusDays(1),
                    contentLockedAt = mockClock.now().minusDays(1),
                    statusModifiedAt = mockClock.now().minusDays(1),
                    answeredAt = mockClock.now().minusDays(1),
                    content = negativeDocumentContent,
                )

            // Test case Bertil:
            // placed in group A1, document sent but no answer
            val testChildBertil =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)
            val placementB =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE_ONLY,
                    childId = testChildBertil.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementB)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementB.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementB.startDate,
                    endDate = placementB.endDate,
                )
            )

            val bertilResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildBertil.id,
                    status = DocumentStatus.CITIZEN_DRAFT,
                    content = negativeDocumentContent.copy(answers = emptyList()),
                    answeredAt = null,
                    answeredBy = null,
                )

            // Test case Cecil:
            // placed in group A1, split placement, answered during the earlier row
            val testChildCecil =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)
            val placementC1 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = start.plusMonths(1),
                )
            tx.insert(placementC1)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC1.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementC1.startDate,
                    endDate = placementC1.endDate,
                )
            )

            val placementC2 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareData.first.id,
                    startDate = start.plusMonths(1).plusDays(1),
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementC2)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC2.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementC2.startDate,
                    endDate = placementC2.endDate,
                )
            )

            val cecilEarlierPlacementResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildCecil.id,
                    content = affirmativeDocumentContent,
                    createdAt =
                        HelsinkiDateTime.of(placementC1.startDate.plusDays(1).atStartOfDay()),
                    statusModifiedAt =
                        HelsinkiDateTime.of(placementC1.startDate.plusDays(1).atStartOfDay()),
                    contentLockedAt =
                        HelsinkiDateTime.of(placementC1.startDate.plusDays(1).atStartOfDay()),
                    answeredAt =
                        HelsinkiDateTime.of(placementC1.startDate.plusDays(1).atStartOfDay()),
                )

            // Test case Eemeli:
            // placed in group A1, sent at the edge of the creation window, no answer
            val testChildEemeli =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Eemeli",
                    lastName = "Eskola",
                )
            tx.insert(testChildEemeli, DevPersonType.CHILD)
            val placementE =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildEemeli.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementE)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementE.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementE.startDate,
                    endDate = placementE.endDate,
                )
            )

            val eemeliCreatedAt =
                HelsinkiDateTime.of(
                    placementE.startDate
                        .minusDays(CITIZEN_DOCUMENT_CREATION_DAYS_BEFORE_PLACEMENT.toLong())
                        .atStartOfDay()
                )
            val eemeliResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildEemeli.id,
                    status = DocumentStatus.CITIZEN_DRAFT,
                    content = negativeDocumentContent.copy(answers = emptyList()),
                    createdAt = eemeliCreatedAt,
                    statusModifiedAt = eemeliCreatedAt,
                    contentLockedAt = eemeliCreatedAt,
                    answeredAt = null,
                    answeredBy = null,
                )

            // Test case Fanni:
            // placed in group A1, answered one day before the creation window opened
            val testChildFanni =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Fanni",
                    lastName = "Forsberg",
                )
            tx.insert(testChildFanni, DevPersonType.CHILD)
            val placementF =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildFanni.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementF)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementF.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementF.startDate,
                    endDate = placementF.endDate,
                )
            )

            val fanniCreatedAt =
                HelsinkiDateTime.of(
                    placementF.startDate
                        .minusDays(CITIZEN_DOCUMENT_CREATION_DAYS_BEFORE_PLACEMENT + 1L)
                        .atStartOfDay()
                )
            val fanniResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildFanni.id,
                    content = affirmativeDocumentContent,
                    createdAt = fanniCreatedAt,
                    statusModifiedAt = fanniCreatedAt,
                    contentLockedAt = fanniCreatedAt,
                    answeredAt = fanniCreatedAt,
                )

            // Test case Greta:
            // placed in group A1, created before the creation window but sent inside it
            val testChildGreta =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Greta",
                    lastName = "Grönlund",
                )
            tx.insert(testChildGreta, DevPersonType.CHILD)
            val placementG =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildGreta.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementG)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementG.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementG.startDate,
                    endDate = placementG.endDate,
                )
            )

            val gretaCreatedAt =
                HelsinkiDateTime.of(
                    placementG.startDate
                        .minusDays(CITIZEN_DOCUMENT_CREATION_DAYS_BEFORE_PLACEMENT + 30L)
                        .atStartOfDay()
                )
            val gretaSentAt = HelsinkiDateTime.of(placementG.startDate.minusDays(30).atStartOfDay())
            val gretaResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildGreta.id,
                    status = DocumentStatus.CITIZEN_DRAFT,
                    content = negativeDocumentContent.copy(answers = emptyList()),
                    createdAt = gretaCreatedAt,
                    statusModifiedAt = gretaSentAt,
                    contentLockedAt = gretaCreatedAt,
                    answeredAt = null,
                    answeredBy = null,
                )

            // Test case Heidi:
            // placed in group A1, an older unanswered document and a newer answered one
            val testChildHeidi =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Heidi",
                    lastName = "Hakala",
                )
            tx.insert(testChildHeidi, DevPersonType.CHILD)
            val placementH =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildHeidi.id,
                    unitId = daycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementH)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementH.id,
                    daycareGroupId = daycareData.second.first().id,
                    startDate = placementH.startDate,
                    endDate = placementH.endDate,
                )
            )

            val heidiUnansweredAt = mockClock.now().minusDays(30)
            val heidiUnansweredResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildHeidi.id,
                    status = DocumentStatus.CITIZEN_DRAFT,
                    content = negativeDocumentContent.copy(answers = emptyList()),
                    createdAt = heidiUnansweredAt,
                    statusModifiedAt = heidiUnansweredAt,
                    contentLockedAt = heidiUnansweredAt,
                    answeredAt = null,
                    answeredBy = null,
                )

            val heidiAnsweredAt = mockClock.now().minusDays(5)
            val heidiAnsweredResponse =
                aapoLatestResponse.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = testChildHeidi.id,
                    content = affirmativeDocumentContent,
                    createdAt = heidiAnsweredAt,
                    statusModifiedAt = heidiAnsweredAt,
                    contentLockedAt = heidiAnsweredAt,
                    answeredAt = heidiAnsweredAt,
                )

            // Test case Demetrius
            // placed in group B1, backup placement to A1, no document sent
            val testChildDemetrius =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Demetrius",
                    lastName = "Dromedaris",
                    email = "demetrius@notadomain",
                )

            tx.insert(testChildDemetrius, DevPersonType.CHILD)

            val placementD =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildDemetrius.id,
                    unitId = secondaryDaycareData.first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementD)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementD.id,
                    daycareGroupId = secondaryDaycareData.second.first().id,
                    startDate = placementD.startDate,
                    endDate = placementD.endDate,
                )
            )
            tx.insert(
                DevBackupCare(
                    unitId = daycareData.first.id,
                    groupId = daycareData.second.first().id,
                    childId = testChildDemetrius.id,
                    period = FiniteDateRange(mockClock.today(), mockClock.today()),
                )
            )

            listOf(
                    aapoEarlierResponse,
                    aapoLatestResponse,
                    bertilResponse,
                    cecilEarlierPlacementResponse,
                    eemeliResponse,
                    fanniResponse,
                    gretaResponse,
                    heidiUnansweredResponse,
                    heidiAnsweredResponse,
                )
                .forEach { tx.insert(it) }

            listOf(
                Pair(testChildAapo, aapoLatestResponse),
                Pair(testChildBertil, bertilResponse),
                Pair(testChildCecil, cecilEarlierPlacementResponse),
                Pair(testChildEemeli, eemeliResponse),
                Pair(testChildFanni, null),
                Pair(testChildGreta, gretaResponse),
                Pair(testChildHeidi, heidiAnsweredResponse),
                Pair(testChildDemetrius, null),
            )
        }
    }
}
