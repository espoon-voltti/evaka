// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
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
                                Question.MultiselectQuestion(
                                    id = "q2",
                                    label = "kysymys 2",
                                    options = listOf("a", "b", "c")
                                )
                            )
                    )
                )
        )

    val testCreationRequest =
        DocumentTemplateCreateRequest(
            name = "test",
            validity = DateRange(LocalDate.of(2022, 7, 1), null)
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                tx.insertTestEmployee(DevEmployee()).let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                }
        }
    }

    @Test
    fun `test basic crud`() {
        val created =
            controller.createTemplate(dbInstance(), employeeUser, now, testCreationRequest)

        assertEquals(testCreationRequest.name, created.name)
        assertEquals(testCreationRequest.validity, created.validity)
        assertEquals(DocumentTemplateContent(sections = emptyList()), created.content)

        val summaries = controller.getTemplates(dbInstance(), employeeUser, now)
        assertEquals(
            listOf(
                DocumentTemplateSummary(
                    id = created.id,
                    name = created.name,
                    validity = created.validity,
                    published = false
                )
            ),
            summaries
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
        assertEquals(created.copy(content = testContent, validity = newValidity), fetched)

        controller.deleteDraftTemplate(dbInstance(), employeeUser, now, created.id)

        val summaries2 = controller.getTemplates(dbInstance(), employeeUser, now)
        assertEquals(emptyList(), summaries2)
        assertThrows<NotFound> {
            controller.getTemplate(dbInstance(), employeeUser, now, created.id)
        }
    }

    @Test
    fun `test publishing, after which content cannot be updated or template deleted`() {
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
                DocumentTemplateCreateRequest(name = "another", validity = newValidity)
            )

        assertNotEquals(created.id, copy.id)
        assertEquals("another", copy.name)
        assertEquals(newValidity, copy.validity)
        assertEquals(false, copy.published)
        assertEquals(testContent, copy.content)
        assertEquals(copy, controller.getTemplate(dbInstance(), employeeUser, now, copy.id))
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
                                        Question.MultiselectQuestion(
                                            id = "q1",
                                            label = "bar",
                                            options = listOf("a", "b")
                                        ),
                                    )
                            )
                        )
                )
            )
        }
    }
}
