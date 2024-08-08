// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.document.childdocument.getChildDocuments
import fi.espoo.evaka.document.getTemplateSummaries
import fi.espoo.evaka.process.getArchiveProcessByChildDocumentId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_CLOSED
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_REVIEWED
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class DocumentMigratorTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val mockToday: LocalDate = LocalDate.of(2023, 5, 22)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0)))

    private lateinit var vasuTemplate: VasuTemplate

    @Test
    fun `migration smoke test`() {
        db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee())
            val childId =
                tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
            val templateId =
                tx.insertVasuTemplate(
                    name = "vasu",
                    valid = FiniteDateRange(mockToday.minusMonths(13), mockToday.minusMonths(1)),
                    type = CurriculumType.DAYCARE,
                    language = OfficialLanguage.FI,
                    content =
                        VasuContent(
                            hasDynamicFirstSection = true,
                            sections =
                                listOf(
                                    VasuSection(
                                        name = "foo",
                                        questions =
                                            listOf(
                                                VasuQuestion.TextQuestion(
                                                    name = "bar",
                                                    multiline = false,
                                                    value = ""
                                                )
                                            )
                                    )
                                )
                        )
                )
            val vasuDocumentId =
                tx.insertVasuDocument(
                    now = clock.now(),
                    childId = childId,
                    template = tx.getVasuTemplate(templateId)!!
                )
            tx.publishVasuDocument(clock.now(), vasuDocumentId)
            tx.freezeVasuPlacements(clock.today(), vasuDocumentId)
            tx.insertVasuDocumentEvent(vasuDocumentId, MOVED_TO_READY, EvakaUserId(employeeId.raw))
            tx.insertVasuDocumentEvent(
                vasuDocumentId,
                MOVED_TO_REVIEWED,
                EvakaUserId(employeeId.raw)
            )
            tx.insertVasuDocumentEvent(vasuDocumentId, MOVED_TO_CLOSED, EvakaUserId(employeeId.raw))

            val processDefinitionNumber = "1.1.1"
            val archiveMetadataOrganization = "Espoo"
            migrateVasu(
                tx = tx,
                today = mockToday,
                id = vasuDocumentId,
                processDefinitionNumber = processDefinitionNumber,
                archiveMetadataOrganization = archiveMetadataOrganization
            )

            assertEquals(1, tx.getTemplateSummaries().size)
            assertEquals(1, tx.getChildDocuments(childId).size)
            val process = tx.getArchiveProcessByChildDocumentId(ChildDocumentId(vasuDocumentId.raw))
            assertNotNull(process)
            assertEquals(processDefinitionNumber, process.processDefinitionNumber)
            assertEquals(2, process.history.size)
        }
    }
}
