// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.ChildDocumentDecisionStatus
import fi.espoo.evaka.document.childdocument.ChildDocumentOrDecisionStatus
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevChildDocumentDecision
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
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
            type = DocumentType.OTHER_DECISION,
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
            publishedContent = null,
            publishedAt = null,
            modifiedAt = clock.now(),
            contentModifiedAt = clock.now(),
            contentModifiedBy = supervisor2.id,
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
                    publishedAt = clock.now(),
                    publishedContent = child2Document.content,
                    contentModifiedBy = supervisor1.id,
                    decision =
                        DevChildDocumentDecision(
                            createdBy = decisionMaker.id,
                            modifiedBy = decisionMaker.id,
                            status = ChildDocumentDecisionStatus.ACCEPTED,
                            validity = DateRange(clock.today(), null),
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
                            publishedAt = clock.now(),
                            publishedContent = child2Document.content,
                            contentModifiedBy = supervisor1.id,
                            decision =
                                DevChildDocumentDecision(
                                    createdBy = decisionMaker.id,
                                    modifiedBy = decisionMaker.id,
                                    status = ChildDocumentDecisionStatus.ACCEPTED,
                                    validity = validity,
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
