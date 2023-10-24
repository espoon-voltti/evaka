// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var service: ChildDocumentService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    final val clock = MockEvakaClock(2022, 1, 1, 15, 0)

    final val activeTemplateId = DocumentTemplateId(UUID.randomUUID())
    final val expiredTemplateId = DocumentTemplateId(UUID.randomUUID())

    final val activeDocumentId = ChildDocumentId(UUID.randomUUID())
    final val expiredDocumentId = ChildDocumentId(UUID.randomUUID())
    final val alreadyCompletedDocumentId = ChildDocumentId(UUID.randomUUID())

    val templateContent =
        DocumentTemplateContent(
            listOf(
                Section(
                    id = "s1",
                    label = "s1",
                    questions = listOf(Question.TextQuestion(id = "q1", label = "q1"))
                )
            )
        )

    val content =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "answer"))
        )

    val updatedContent =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "updated"))
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare.copy(language = Language.sv))
            tx.insert(testAdult_2)
            tx.insert(testChild_1)
            tx.insert(DevChild(testChild_1.id))
            tx.insertTestGuardian(
                DevGuardian(guardianId = testAdult_2.id, childId = testChild_1.id)
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = clock.today(),
                endDate = clock.today().plusDays(5)
            )
            tx.insert(
                DevDocumentTemplate(
                    id = activeTemplateId,
                    type = DocumentType.PEDAGOGICAL_ASSESSMENT,
                    name = "Arvio",
                    validity = DateRange(clock.today().minusYears(1), clock.today()),
                    content = templateContent
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    id = expiredTemplateId,
                    type = DocumentType.HOJKS,
                    name = "HOJKS",
                    validity = DateRange(clock.today().minusYears(1), clock.today().minusDays(1)),
                    content = templateContent
                )
            )
        }
    }

    @Test
    fun `expired documents are completed and published`() {
        // given
        db.transaction { tx ->
            tx.insert(
                DevChildDocument(
                    id = activeDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = activeTemplateId,
                    content = content,
                    publishedContent = null,
                    modifiedAt = clock.now(),
                    publishedAt = null
                )
            )
            tx.insert(
                DevChildDocument(
                    id = expiredDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = expiredTemplateId,
                    content = content,
                    publishedContent = updatedContent,
                    modifiedAt = clock.now(),
                    publishedAt =
                        HelsinkiDateTime.Companion.of(
                            clock.today().minusMonths(1),
                            LocalTime.of(8, 0)
                        )
                )
            )
            tx.insert(
                DevChildDocument(
                    id = alreadyCompletedDocumentId,
                    status = DocumentStatus.COMPLETED,
                    childId = testChild_1.id,
                    templateId = expiredTemplateId,
                    content = content,
                    publishedContent = updatedContent,
                    modifiedAt = clock.now().minusMonths(1),
                    publishedAt = clock.now().minusMonths(1)
                )
            )
        }

        // when
        db.transaction { service.completeAndPublishChildDocumentsAtEndOfTerm(it, clock.now()) }
        asyncJobRunner.runPendingJobsSync(clock)

        // then
        db.read { tx ->
            with(tx.getChildDocument(activeDocumentId)!!) {
                assertEquals(DocumentStatus.DRAFT, status)
                assertNull(publishedAt)
            }

            with(tx.getChildDocument(expiredDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
                assertEquals(clock.now(), publishedAt)
            }

            with(tx.getChildDocument(alreadyCompletedDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
                assertEquals(clock.now().minusMonths(1), publishedAt)
            }
        }
        assertEquals(1, MockEmailClient.emails.size)
        assertEquals(testAdult_2.email, MockEmailClient.emails.first().toAddress)
    }

    @Test
    fun `email is not sent on publish if content was already up to date`() {
        // given
        db.transaction { tx ->
            tx.insert(
                DevChildDocument(
                    id = expiredDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = expiredTemplateId,
                    content = content,
                    publishedContent = content,
                    modifiedAt = clock.now(),
                    publishedAt =
                        HelsinkiDateTime.Companion.of(
                            clock.today().minusMonths(1),
                            LocalTime.of(8, 0)
                        )
                )
            )
        }

        // when
        db.transaction { service.completeAndPublishChildDocumentsAtEndOfTerm(it, clock.now()) }
        asyncJobRunner.runPendingJobsSync(clock)

        // then
        db.read { tx ->
            with(tx.getChildDocument(expiredDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
            }
        }
        assertEquals(0, MockEmailClient.emails.size)
    }
}
