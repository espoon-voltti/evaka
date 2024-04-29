// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DocumentTemplateIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: DocumentTemplateController

    lateinit var employeeUser: AuthenticatedUser

    val now = MockEvakaClock(2022, 1, 1, 15, 0)

    val testContent =
        DocumentTemplateContent(
            sections =
                listOf(
                    Section(
                        id = "s1",
                        label = "Eka",
                        questions =
                            listOf(
                                Question.TextQuestion(id = "q1", label = "kysymys 1"),
                                Question.CheckboxQuestion(id = "q2", label = "kysymys 2")
                            )
                    )
                )
        )

    val testCreationRequest =
        DocumentTemplateBasicsRequest(
            name = "test",
            type = DocumentType.PEDAGOGICAL_ASSESSMENT,
            language = OfficialLanguage.FI,
            confidential = true,
            legalBasis = "§42",
            validity = DateRange(LocalDate.of(2022, 7, 1), null)
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                tx.insert(DevEmployee()).let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                }
            tx.insert(testArea)
            tx.insert(testDaycare.copy(language = Language.sv))
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = now.today(),
                endDate = now.today().plusDays(5)
            )
        }
    }

    @Test
    fun `test basic crud`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)

        assertEquals(testCreationRequest.name, created.name)
        assertEquals(testCreationRequest.type, created.type)
        assertEquals(testCreationRequest.language, created.language)
        assertEquals(testCreationRequest.confidential, created.confidential)
        assertEquals(testCreationRequest.legalBasis, created.legalBasis)
        assertEquals(testCreationRequest.validity, created.validity)
        assertEquals(DocumentTemplateContent(sections = emptyList()), created.content)

        val summaries = controller.getTemplates(dbInstance(), employeeUser, now)
        assertEquals(
            listOf(
                DocumentTemplateSummary(
                    id = created.id,
                    name = created.name,
                    type = created.type,
                    language = created.language,
                    validity = created.validity,
                    published = false
                )
            ),
            summaries
        )

        controller.updateDraftTemplateBasics(
            dbInstance(),
            employeeUser,
            now,
            created.id,
            testCreationRequest.copy(
                name = "name2",
                language = OfficialLanguage.SV,
                type = DocumentType.PEDAGOGICAL_REPORT,
                confidential = false,
                legalBasis = "$42b"
            )
        )

        controller.updateDraftTemplateContent(
            dbInstance(),
            employeeUser,
            now,
            created.id,
            testContent
        )
        val newValidity = DateRange(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 1))
        controller.updateTemplateValidity(dbInstance(), employeeUser, now, created.id, newValidity)

        val fetched = controller.getTemplate(dbInstance(), employeeUser, now, created.id)
        assertEquals(
            created.copy(
                name = "name2",
                language = OfficialLanguage.SV,
                type = DocumentType.PEDAGOGICAL_REPORT,
                confidential = false,
                legalBasis = "$42b",
                content = testContent,
                validity = newValidity
            ),
            fetched
        )

        controller.deleteDraftTemplate(dbInstance(), employeeUser, now, created.id)

        val summaries2 = controller.getTemplates(dbInstance(), employeeUser, now)
        assertEquals(emptyList(), summaries2)
        assertThrows<NotFound> {
            controller.getTemplate(dbInstance(), employeeUser, now, created.id)
        }
    }

    @Test
    fun `test publishing, after which basics and content cannot be updated or template deleted`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)
        controller.updateDraftTemplateContent(
            dbInstance(),
            employeeUser,
            now,
            created.id,
            testContent
        )
        controller.publishTemplate(dbInstance(), employeeUser, now, created.id)

        assertTrue(controller.getTemplate(dbInstance(), employeeUser, now, created.id).published)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateBasics(
                dbInstance(),
                employeeUser,
                now,
                created.id,
                testCreationRequest.copy(name = "changed")
            )
        }

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employeeUser,
                now,
                created.id,
                testContent
            )
        }

        assertThrows<BadRequest> {
            controller.deleteDraftTemplate(dbInstance(), employeeUser, now, created.id)
        }

        // validity period can still be updated
        controller.updateTemplateValidity(
            dbInstance(),
            employeeUser,
            now,
            created.id,
            DateRange(LocalDate.of(2000, 1, 1), null)
        )
    }

    @Test
    fun `template can be duplicated`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)
        controller.updateDraftTemplateContent(
            dbInstance(),
            employeeUser,
            now,
            created.id,
            testContent
        )
        controller.publishTemplate(dbInstance(), employeeUser, now, created.id)

        val newValidity = DateRange(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 1))
        val copy =
            controller.duplicateTemplate(
                dbInstance(),
                employeeUser,
                now,
                created.id,
                DocumentTemplateBasicsRequest(
                    name = "another",
                    type = DocumentType.PEDAGOGICAL_REPORT,
                    language = OfficialLanguage.SV,
                    confidential = false,
                    legalBasis = "",
                    validity = newValidity
                )
            )

        assertNotEquals(created.id, copy.id)
        assertEquals("another", copy.name)
        assertEquals(DocumentType.PEDAGOGICAL_REPORT, copy.type)
        assertEquals(OfficialLanguage.SV, copy.language)
        assertEquals(false, copy.confidential)
        assertEquals("", copy.legalBasis)
        assertEquals(newValidity, copy.validity)
        assertEquals(false, copy.published)
        assertEquals(testContent, copy.content)
        assertEquals(copy, controller.getTemplate(dbInstance(), employeeUser, now, copy.id))
    }

    @Test
    fun `active templates endpoint returns valid templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employeeUser,
                now,
                testCreationRequest.copy(
                    language = OfficialLanguage.SV,
                    validity = DateRange(now.today(), null)
                )
            )
        controller.publishTemplate(dbInstance(), employeeUser, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(1, active.size)
        assertEquals(template.id, active.first().id)
    }

    @Test
    fun `active templates endpoint does not return draft templates`() {
        controller.createTemplate(
            dbInstance(),
            employeeUser,
            now,
            testCreationRequest.copy(
                language = OfficialLanguage.SV,
                validity = DateRange(now.today(), null)
            )
        )

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, testChild_1.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return future templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employeeUser,
                now,
                testCreationRequest.copy(
                    language = OfficialLanguage.SV,
                    validity = DateRange(now.today().plusDays(1), null)
                )
            )
        controller.publishTemplate(dbInstance(), employeeUser, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, testChild_1.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return expired templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employeeUser,
                now,
                testCreationRequest.copy(
                    language = OfficialLanguage.SV,
                    validity = DateRange(now.today().minusDays(10), now.today().minusDays(1))
                )
            )
        controller.publishTemplate(dbInstance(), employeeUser, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, testChild_1.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return templates in finnish when placement unit is swedish`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employeeUser,
                now,
                testCreationRequest.copy(
                    language = OfficialLanguage.FI,
                    validity = DateRange(now.today(), null)
                )
            )
        controller.publishTemplate(dbInstance(), employeeUser, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, testChild_1.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return templates in swedish when placement unit is finnish`() {
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea(shortName = "area2"))
                val daycareId = tx.insert(DevDaycare(areaId = areaId, language = Language.fi))
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertTestPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = now.today(),
                    endDate = now.today().plusDays(5)
                )
                childId
            }
        val template =
            controller.createTemplate(
                dbInstance(),
                employeeUser,
                now,
                testCreationRequest.copy(
                    language = OfficialLanguage.SV,
                    validity = DateRange(now.today(), null)
                )
            )
        controller.publishTemplate(dbInstance(), employeeUser, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employeeUser, now, childId)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `section ids must be unique`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employeeUser,
                now,
                created.id,
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(id = "s1", label = "foo", questions = emptyList()),
                            Section(id = "s1", label = "bar", questions = emptyList())
                        )
                )
            )
        }
    }

    @Test
    fun `question ids must be unique`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employeeUser,
                now,
                created.id,
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(
                                id = "s1",
                                label = "hello",
                                questions =
                                    listOf(
                                        Question.TextQuestion(id = "q1", label = "foo"),
                                        Question.CheckboxQuestion(id = "q1", label = "bar"),
                                    )
                            )
                        )
                )
            )
        }
    }
}
