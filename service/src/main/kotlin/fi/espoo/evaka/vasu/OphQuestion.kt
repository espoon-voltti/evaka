// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

enum class OphQuestionKey {
    PEDAGOGIC_ACTIVITY_GOALS,
    SKILL_IMPROVEMENT_GOALS,
    PEDAGOGIC_GOALS_DESCRIPTION,
}

data class OphQuestion(
    val name: Map<VasuLanguage, String>,
    val options: List<OphQuestionOption>
)

data class OphQuestionOption(
    val key: String,
    val name: Map<VasuLanguage, String>
)

fun getDefaultTemplateContent(lang: VasuLanguage) = VasuContent(
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tavoitteet henkilöstön pedagogiselle toiminnalle"
                VasuLanguage.SV -> "Mål för personalens pedagogiska aktiviteter"
            },
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    ophKey = OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS,
                    name = ophQuestionMap[OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS]!!.name[lang]!!,
                    options = ophQuestionMap[OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS]!!.options.map {
                        QuestionOption(
                            key = it.key,
                            name = it.name[lang]!!
                        )
                    },
                    minSelections = 1,
                    maxSelections = null,
                    value = emptyList()
                )
            )
        )
    )
)

private val ophQuestionMap = mapOf(
    OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS to OphQuestion(
        name = mapOf(
            VasuLanguage.FI to "Tavoitteet henkilöstön pedagogiselle toiminnalle",
            VasuLanguage.SV to "Mål för personalens pedagogiska aktiviteter"
        ),
        options = listOf(
            OphQuestionOption(
                key = "foo",
                name = mapOf(
                    VasuLanguage.FI to "tavoite",
                    VasuLanguage.SV to "mål"
                )
            )
        )
    )
)

fun copyTemplateContentWithCurrentlyValidOphSections(template: VasuTemplate): VasuContent {
    val copyableSections = template.content.sections.filter { section -> section.questions.all { question -> question.ophKey == null } }
    val defaultSections = getDefaultTemplateContent(template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
