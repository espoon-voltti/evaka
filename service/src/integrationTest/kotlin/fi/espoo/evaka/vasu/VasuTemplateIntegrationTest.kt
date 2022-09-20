// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VasuTemplateIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val adminUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `creating new template includes default questions`() {
        postVasuTemplate(
            VasuTemplateController.CreateTemplateRequest(
                name = "vasu",
                valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                type = CurriculumType.DAYCARE,
                language = VasuLanguage.FI
            )
        )

        val summaries = getVasuTemplates()
        assertEquals(1, summaries.size)
        with(summaries.first()) {
            assertEquals("vasu", name)
            assertEquals(FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)), valid)
            assertEquals(CurriculumType.DAYCARE, type)
            assertEquals(VasuLanguage.FI, language)
            assertEquals(0, documentCount)
        }

        val defaultQuestions = getDefaultTemplateContent(CurriculumType.DAYCARE, VasuLanguage.FI)

        val templateId = summaries.first().id
        val template = getVasuTemplate(templateId)
        assertEquals(defaultQuestions, template.content)

        // unused template may be deleted
        deleteVasuTemplate(templateId)
        assertTrue(getVasuTemplates().isEmpty())
    }

    @Test
    fun `editing template content`() {
        postVasuTemplate(
            VasuTemplateController.CreateTemplateRequest(
                name = "vasu",
                valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                type = CurriculumType.DAYCARE,
                language = VasuLanguage.FI
            )
        )

        val templateId = getVasuTemplates().first().id

        val template = getVasuTemplate(templateId)

        val updatedContent =
            VasuContent(
                sections =
                    template.content.sections +
                        listOf(
                            VasuSection(
                                name = "uusi osio",
                                questions =
                                    listOf(
                                        VasuQuestion.TextQuestion(
                                            name = "kysymys 1",
                                            multiline = false,
                                            value = ""
                                        ),
                                        VasuQuestion.CheckboxQuestion(
                                            name = "kysymys 2",
                                            value = false
                                        ),
                                        VasuQuestion.RadioGroupQuestion(
                                            name = "kysymys 3",
                                            options =
                                                listOf(
                                                    QuestionOption("vaihtoehto 1", "vaihtoehto 1"),
                                                    QuestionOption("vaihtoehto 2", "vaihtoehto 2")
                                                ),
                                            value = "vaihtoehto 1"
                                        ),
                                        VasuQuestion.MultiSelectQuestion(
                                            name = "kysymys 4",
                                            options =
                                                listOf(
                                                    QuestionOption("vaihtoehto 1", "vaihtoehto 1"),
                                                    QuestionOption("vaihtoehto 2", "vaihtoehto 2")
                                                ),
                                            minSelections = 1,
                                            maxSelections = 2,
                                            value = listOf("vaihtoehto 1"),
                                            textValue = emptyMap()
                                        )
                                    )
                            ),
                        )
            )

        putVasuTemplateContent(templateId, updatedContent)

        assertEquals(updatedContent, getVasuTemplate(templateId).content)
    }

    private fun postVasuTemplate(request: VasuTemplateController.CreateTemplateRequest) {
        val (_, res, _) =
            http
                .post("/vasu/templates")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(adminUser)
                .response()

        assertEquals(200, res.statusCode)
    }

    private fun getVasuTemplates(): List<VasuTemplateSummary> {
        val (_, res, result) =
            http
                .get("/vasu/templates")
                .asUser(adminUser)
                .responseObject<List<VasuTemplateSummary>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuTemplate(id: VasuTemplateId): VasuTemplate {
        val (_, res, result) =
            http
                .get("/vasu/templates/$id")
                .asUser(adminUser)
                .responseObject<VasuTemplate>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun putVasuTemplateContent(id: VasuTemplateId, request: VasuContent) {
        val (_, res, _) =
            http
                .put("/vasu/templates/$id/content")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(adminUser)
                .response()

        assertEquals(200, res.statusCode)
    }

    private fun deleteVasuTemplate(id: VasuTemplateId) {
        val (_, res, _) = http.delete("/vasu/templates/$id").asUser(adminUser).response()

        assertEquals(200, res.statusCode)
    }
}
