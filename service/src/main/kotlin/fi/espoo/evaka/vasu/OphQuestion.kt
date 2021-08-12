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
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Näkemyksien huomioiminen"
                VasuLanguage.SV -> "Näkemyksien huomioiminen"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten lapsen näkökulma ja mielipiteet on otettu huomioon"
                        VasuLanguage.SV -> "Miten lapsen näkökulma ja mielipiteet on otettu huomioon"
                    },
                    info = "Lorem ipsum",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten huoltajien näkemykset otetaan huomioon ja miten yhteistyö on järjestetty"
                        VasuLanguage.SV -> "Miten huoltajien näkemykset otetaan huomioon ja miten yhteistyö on järjestetty"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Aiemman varhaiskasvatussuunnitelman tavoitteet ja toimenpiteet"
                VasuLanguage.SV -> "Aiemman varhaiskasvatussuunnitelman tavoitteet ja toimenpiteet"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden toteutuminen"
                        VasuLanguage.SV -> "Tavoitteiden toteutuminen"
                    },
                    info = "Lorem ipsum",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapsen edellisestä vasusta"
                        VasuLanguage.SV -> "Muut havainnot lapsen edellisestä vasusta"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
                VasuLanguage.SV -> "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                        VasuLanguage.SV -> "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                    },
                    info = "Lorem ipsum",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteet henkilöstön pedagogiselle toiminnalle sekä toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                        VasuLanguage.SV -> "Tavoitteet henkilöstön pedagogiselle toiminnalle sekä toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                    },
                    info = "Lorem ipsum",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Mahdolliset muut kehityksen ja oppimisen tukeen liittyvät tarpeet sekä tuen toteuttamiseen liittyvät tavoitteet ja sovitut järjestelyt"
                        VasuLanguage.SV -> "Mahdolliset muut kehityksen ja oppimisen tukeen liittyvät tarpeet sekä tuen toteuttamiseen liittyvät tavoitteet ja sovitut järjestelyt"
                    },
                    info = "Lorem ipsum",
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen hyvinvoinnin tukemiseen liittyvät muut huomioitavat asiat"
                VasuLanguage.SV -> "Lapsen hyvinvoinnin tukemiseen liittyvät muut huomioitavat asiat"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Esimerkiksi päiväuniin, ruokailuun tai ulkoiluun liittyvät asiat"
                        VasuLanguage.SV -> "Esimerkiksi päiväuniin, ruokailuun tai ulkoiluun liittyvät asiat"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                VasuLanguage.SV -> "Muut asiakirjat ja suunnitelmat"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatussuunitelman laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                        VasuLanguage.SV -> "Varhaiskasvatussuunitelman laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tiedonsaajatahot"
                VasuLanguage.SV -> "Tiedonsaajatahot"
            },
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla:"
                        VasuLanguage.SV -> "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla:"
                    },
                    options = listOf(
                        QuestionOption(
                            key = "Tulevaan esiopetusryhmään",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tulevaan esiopetusryhmään"
                                VasuLanguage.SV -> "Tulevaan esiopetusryhmään"
                            }
                        ),
                        QuestionOption(
                            key = "Neuvolaan",
                            name = when (lang) {
                                VasuLanguage.FI -> "Neuvolaan"
                                VasuLanguage.SV -> "Neuvolaan"
                            }
                        ),
                        QuestionOption(
                            key = "Lasten terapiapalveluihin",
                            name = when (lang) {
                                VasuLanguage.FI -> "Lasten terapiapalveluihin"
                                VasuLanguage.SV -> "Lasten terapiapalveluihin"
                            }
                        ),
                        QuestionOption(
                            key = "Erikoissairaanhoitoon",
                            name = when (lang) {
                                VasuLanguage.FI -> "Erikoissairaanhoitoon"
                                VasuLanguage.SV -> "Erikoissairaanhoitoon"
                            }
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = emptyList()
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muualle, minne?"
                        VasuLanguage.SV -> "Muualle, minne?"
                    },
                    multiline = false,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lisätietoja"
                VasuLanguage.SV -> "Lisätietoja"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lisätietoja suunnitelmaan, sen laatimiseen tai keskusteluihin liittyen"
                        VasuLanguage.SV -> "Lisätietoja suunnitelmaan, sen laatimiseen tai keskusteluihin liittyen"
                    },
                    multiline = true,
                    value = ""
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

// TODO: this won't currently work
fun copyTemplateContentWithCurrentlyValidOphSections(template: VasuTemplate): VasuContent {
    val copyableSections = template.content.sections.filter { section -> section.questions.all { question -> question.ophKey == null } }
    val defaultSections = getDefaultTemplateContent(template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
