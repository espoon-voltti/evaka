// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

enum class OphQuestionKey {
    PEDAGOGIC_ACTIVITY_GOALS,
    PEDAGOGIC_GOALS_DESCRIPTION
}

data class OphQuestion(
    val name: Map<VasuLanguage, String>,
    val options: List<OphQuestionOption>
)

data class OphQuestionOption(
    val key: String,
    val name: Map<VasuLanguage, String>
)

fun getDefaultTemplateContent(@Suppress("UNUSED_PARAMETER") lang: VasuLanguage) = VasuContent(
    sections = emptyList()

/*    listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
                VasuLanguage.SV -> "Mål för pedagogisk verksamhet och åtgärder för att uppnå målen"
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
                ),
                VasuQuestion.TextQuestion(
                    ophKey = OphQuestionKey.PEDAGOGIC_GOALS_DESCRIPTION,
                    name = ophQuestionMap[OphQuestionKey.PEDAGOGIC_GOALS_DESCRIPTION]!!.name[lang]!!,
                    value = "",
                    multiline = true
                )
            )
        )
    )*/
)

private val ophQuestionMap = mapOf(
    OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS to OphQuestion(
        name = mapOf(
            VasuLanguage.FI to "Tavoitteet henkilöstön pedagogiselle toiminnalle",
            VasuLanguage.SV to "Mål för personalens pedagogiska aktiviteter"
        ),
        options = listOf(
            OphQuestionOption(
                key = "goal1",
                name = mapOf(
                    VasuLanguage.FI to "Joku tavoite",
                    VasuLanguage.SV to "mål 1"
                )
            ),
            OphQuestionOption(
                key = "goal2",
                name = mapOf(
                    VasuLanguage.FI to "Toinen tavoite",
                    VasuLanguage.SV to "mål 2"
                )
            ),
            OphQuestionOption(
                key = "goal3",
                name = mapOf(
                    VasuLanguage.FI to "Kolmas vaihtoehto",
                    VasuLanguage.SV to "mål 3"
                )
            )
        )
    ),
    OphQuestionKey.PEDAGOGIC_GOALS_DESCRIPTION to OphQuestion(
        name = mapOf(
            VasuLanguage.FI to "Kuvaile tavoitteita tarkemmin",
            VasuLanguage.SV to "Beskriv målen mer i detalj"
        ),
        options = emptyList()
    )
)

fun copyTemplateContentWithCurrentlyValidOphSections(template: VasuTemplate): VasuContent {
    val copyableSections = template.content.sections.filter { section -> section.questions.all { question -> question.ophKey == null } }
    val defaultSections = getDefaultTemplateContent(template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
