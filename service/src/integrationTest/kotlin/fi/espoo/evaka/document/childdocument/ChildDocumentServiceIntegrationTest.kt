// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestChildDocument
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDocumentTemplate
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestPerson
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

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(testDaycare.copy(language = Language.sv))
            tx.insertTestPerson(testAdult_2)
            tx.insertTestPerson(testChild_1)
            tx.insertTestChild(DevChild(testChild_1.id))
            tx.insertTestGuardian(
                DevGuardian(guardianId = testAdult_2.id, childId = testChild_1.id)
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = clock.today(),
                endDate = clock.today().plusDays(5)
            )
            tx.insertTestDocumentTemplate(
                DevDocumentTemplate(
                    id = activeTemplateId,
                    type = DocumentType.PEDAGOGICAL_ASSESSMENT,
                    name = "Arvio",
                    validity = DateRange(clock.today().minusYears(1), clock.today()),
                    content = DocumentTemplateContent(emptyList())
                )
            )
            tx.insertTestDocumentTemplate(
                DevDocumentTemplate(
                    id = expiredTemplateId,
                    type = DocumentType.HOJKS,
                    name = "HOJKS",
                    validity = DateRange(clock.today().minusYears(1), clock.today().minusDays(1)),
                    content = DocumentTemplateContent(emptyList())
                )
            )
        }
    }

    @Test
    fun `creating new document and fetching it`() {
        // given
        db.transaction { tx ->
            tx.insertTestChildDocument(
                DevChildDocument(
                    id = activeDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = activeTemplateId,
                    content = DocumentContent(emptyList()),
                    publishedContent = null,
                    publishedAt = null
                )
            )
            tx.insertTestChildDocument(
                DevChildDocument(
                    id = expiredDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = expiredTemplateId,
                    content = DocumentContent(emptyList()),
                    publishedContent = DocumentContent(emptyList()),
                    publishedAt =
                        HelsinkiDateTime.Companion.of(
                            clock.today().minusMonths(1),
                            LocalTime.of(8, 0)
                        )
                )
            )
            tx.insertTestChildDocument(
                DevChildDocument(
                    id = alreadyCompletedDocumentId,
                    status = DocumentStatus.COMPLETED,
                    childId = testChild_1.id,
                    templateId = expiredTemplateId,
                    content = DocumentContent(emptyList()),
                    publishedContent = DocumentContent(emptyList()),
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
}
