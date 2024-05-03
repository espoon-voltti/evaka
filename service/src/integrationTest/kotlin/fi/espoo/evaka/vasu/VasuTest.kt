// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.domain.OfficialLanguage
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class VasuTest {

    @Test
    fun `matchesStructurally returns true for equal objects`() {
        val content = getDefaultTemplateContent(CurriculumType.DAYCARE, OfficialLanguage.FI)
        assertTrue { content.matchesStructurally(content) }
    }

    @Test
    fun `matchesStructurally returns true with equal sections`() {
        assertTrue {
            getSampleContent().matchesStructurally(VasuContent(sections = getSampleSections()))
        }
    }

    @Test
    fun `matchesStructurally returns false if there are extra sections`() {
        assertFalse {
            getSampleContent()
                .matchesStructurally(
                    VasuContent(sections = getSampleSections() + getSampleSections())
                )
        }
    }

    @Test
    fun `matchesStructurally returns true for structurally equal objects where value does not match`() {
        assertTrue { getSampleContent().matchesStructurally(getSampleContent(listOf("1", "2"))) }
    }

    @Test
    fun `matchesStructurally returns false for non-equal content`() {
        assertFalse {
            getSampleContent()
                .matchesStructurally(
                    VasuContent(
                        sections =
                            listOf(
                                VasuSection(
                                    name = "foo",
                                    questions = listOf(getSampleQuestion().copy(name = "babar"))
                                )
                            )
                    )
                )
        }
    }

    @Test
    fun `matchesStructurally validates the new content`() {
        assertFalse(getSampleContent().matchesStructurally(getSampleContent(listOf("3"))))
    }

    private fun getSampleContent(value: List<String> = listOf()) =
        VasuContent(sections = getSampleSections(value))

    private fun getSampleSections(value: List<String> = listOf()) =
        listOf(VasuSection(name = "foo", questions = listOf(getSampleQuestion(value))))

    private fun getSampleQuestion(value: List<String> = listOf()) =
        VasuQuestion.MultiSelectQuestion(
            ophKey = OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS,
            name = "Q1",
            options =
                listOf(
                    QuestionOption(key = "1", name = "Kyllä"),
                    QuestionOption(key = "2", name = "Ei")
                ),
            minSelections = 1,
            maxSelections = null,
            value = value,
            textValue = emptyMap()
        )
}
