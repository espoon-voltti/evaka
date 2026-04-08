// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.ChildDocumentDecisionStatus
import evaka.core.document.childdocument.ChildDocumentOrDecisionStatus
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentDecision
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentDecisionsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: ChildDocumentDecisionsReportController

    val clock = MockEvakaClock(2024, 3, 1, 15, 0)

    val area = DevCareArea()
    val daycare1 =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC))
    val daycare2 =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC))
    val supervisor1 = DevEmployee()
    val supervisor2 = DevEmployee()
    val decisionMaker = DevEmployee(roles = setOf(UserRole.DIRECTOR))
    val child1 = DevPerson()
    val child2 = DevPerson()
    val template =
        DevDocumentTemplate(
            type = ChildDocumentType.OTHER_DECISION,
            name = "Tuen päätös",
            validity = DateRange(clock.today(), null),
            content =
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(
                                id = "s1",
                                label = "s1",
                                questions =
                                    listOf(Question.CheckboxQuestion(id = "q1", label = "q1")),
                            )
                        )
                ),
            endDecisionWhenUnitChanges = true,
        )
    val child2Document =
        DevChildDocument(
            status = DocumentStatus.DECISION_PROPOSAL,
            childId = child2.id,
            templateId = template.id,
            decisionMaker = decisionMaker.id,
            content =
                DocumentContent(
                    answers =
                        listOf(AnsweredQuestion.CheckboxAnswer(questionId = "q1", answer = true))
                ),
            modifiedAt = clock.now(),
            modifiedBy = supervisor2.evakaUserId,
            contentLockedAt = clock.now(),
            contentLockedBy = supervisor2.id,
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(supervisor1, unitRoles = mapOf(daycare1.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(supervisor2, unitRoles = mapOf(daycare2.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(decisionMaker)
            tx.insert(child1, type = DevPersonType.CHILD)
            tx.insert(child2, type = DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = clock.today().minusMonths(1),
                    endDate = clock.today().plusMonths(1),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = clock.today().minusMonths(1),
                    endDate = clock.today().plusMonths(1),
                )
            )
            tx.insert(template)
            tx.insert(child2Document)
        }
    }

    @Test
    fun `child 2 document is visible to supervisor 2 and decision maker`() {
        assertEquals(0, getReport(supervisor1.user).size)
        assertEquals(1, getReport(supervisor2.user).size)
        assertEquals(1, getReport(decisionMaker.user).size)
    }

    @Test
    fun `notification is shown to decision maker in decision proposal status`() {
        db.transaction { tx ->
            tx.insert(
                child2Document.copy(
                    id = ChildDocumentId(UUID.randomUUID()),
                    childId = child1.id,
                    status = DocumentStatus.COMPLETED,
                    contentLockedBy = supervisor1.id,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = clock.now(),
                                createdBy = supervisor1.evakaUserId,
                                publishedContent = child2Document.content,
                            )
                        ),
                    decision =
                        DevChildDocumentDecision(
                            createdBy = decisionMaker.id,
                            modifiedBy = decisionMaker.id,
                            status = ChildDocumentDecisionStatus.ACCEPTED,
                            validity = DateRange(clock.today(), null),
                            daycareId = daycare1.id,
                        ),
                )
            )
        }
        assertEquals(0, getNotifications(supervisor1.user))
        assertEquals(0, getNotifications(supervisor2.user))
        assertEquals(1, getNotifications(decisionMaker.user))
    }

    @Test
    fun `status filter works`() {
        assertEquals(1, getReport(decisionMaker.user, statuses = emptySet()).size)
        assertEquals(
            1,
            getReport(
                    decisionMaker.user,
                    statuses = setOf(ChildDocumentOrDecisionStatus.DECISION_PROPOSAL),
                )
                .size,
        )
        assertEquals(
            1,
            getReport(
                    decisionMaker.user,
                    statuses =
                        setOf(
                            ChildDocumentOrDecisionStatus.DECISION_PROPOSAL,
                            ChildDocumentOrDecisionStatus.DRAFT,
                        ),
                )
                .size,
        )
        assertEquals(
            0,
            getReport(decisionMaker.user, statuses = setOf(ChildDocumentOrDecisionStatus.DRAFT))
                .size,
        )
    }

    @Test
    fun `include ended filter works`() {
        db.transaction { tx ->
            // child 1 has one upcoming, one current and one ended decision
            listOf(
                    DateRange(clock.today().plusMonths(1), null),
                    DateRange(clock.today().minusMonths(1), clock.today().plusDays(5)),
                    DateRange(clock.today().minusMonths(6), clock.today().minusMonths(4)),
                )
                .forEach { validity ->
                    tx.insert(
                        child2Document.copy(
                            id = ChildDocumentId(UUID.randomUUID()),
                            childId = child1.id,
                            status = DocumentStatus.COMPLETED,
                            contentLockedBy = supervisor1.id,
                            publishedVersions =
                                listOf(
                                    DevChildDocumentPublishedVersion(
                                        versionNumber = 1,
                                        createdAt = clock.now(),
                                        createdBy = supervisor1.evakaUserId,
                                        publishedContent = child2Document.content,
                                    )
                                ),
                            decision =
                                DevChildDocumentDecision(
                                    createdBy = decisionMaker.id,
                                    modifiedBy = decisionMaker.id,
                                    status = ChildDocumentDecisionStatus.ACCEPTED,
                                    validity = validity,
                                    daycareId = daycare1.id,
                                ),
                        )
                    )
                }
        }

        assertEquals(4, getReport(decisionMaker.user, includeEnded = true).size)
        assertEquals(3, getReport(decisionMaker.user, includeEnded = false).size)
    }

    private fun getReport(
        user: AuthenticatedUser.Employee,
        statuses: Set<ChildDocumentOrDecisionStatus> = emptySet(),
        includeEnded: Boolean = false,
    ) =
        controller.getChildDocumentDecisionsReport(
            db = dbInstance(),
            user = user,
            clock = clock,
            statuses = statuses,
            includeEnded = includeEnded,
        )

    private fun getNotifications(user: AuthenticatedUser.Employee) =
        controller.getChildDocumentDecisionsReportNotificationCount(
            db = dbInstance(),
            user = user,
            clock = clock,
        )
}
