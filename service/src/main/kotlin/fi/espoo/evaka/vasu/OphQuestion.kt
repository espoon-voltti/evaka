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

fun getDefaultTemplateContent(lang: VasuLanguage) = VasuContent(
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Näkemyksien huomioiminen"
                VasuLanguage.SV -> "Barnets och vårdnadshavarnas delaktighet i uppgörandet av planen"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten lapsen näkökulma ja mielipiteet on otettu huomioon"
                        VasuLanguage.SV -> "Hur har barnets perspektiv och synpunkter beaktats"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Lapsen näkökulma on läsnä keskustelussa koko ajan. Lapsi voi myös osallistua keskusteluun osan ajasta: ikätason mukaan lapsen osallisuutta vasukeskusteluissa lisätään. Lapsi voi esimerkiksi esitellä mielipaikkojaan, muuta oppimisympäristöä tai lempilelujaan sisällä tai ulkona. Keskustelkaa tiimissä otsikossa mainituista asioista niin, että kaikki tiimin jäsenet tuovat ilmi oman näkemyksensä havaintojen ja pedagogisen dokumentoinnin perusteella. Kirjaa lyhyt yhteenveto tähän. Jos et löydä juuri sopivaa ”lokeroa” mielestänne tärkeän asian kirjaamiselle, palatkaa vasutekstin äärelle tai jutelkaa esimiehenne/työkavereittenne kanssa."
                        VasuLanguage.SV -> "Barnets perspektiv är ständigt närvarande i samtalet. Barnet kan också delta i samtalet en del av tiden: beroende på ålder ökas barnets deltagande i samtalen i anslutning till planen för småbarnspedagogik. Barnet kan till exempel visa sina favoritplatser, annan inlärningsmiljö eller sina favoritleksaker inomhus eller utomhus. Diskutera barnets perspektiv så att alla teammedlemmar uttrycker sina synpunkter baserade på observationer och pedagogisk dokumentation. Skriv en kort sammanfattning här. Om du inte hittar ett lämpligt ställe för ett ärende som ni tycker är viktigt, gå tillbaka till texten för planen för småbarnspedagogik eller prata med er chef/era medarbetare."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten huoltajien näkemykset otetaan huomioon ja miten yhteistyö on järjestetty"
                        VasuLanguage.SV -> "Hur har vårdnadshavarnas synpunkter beaktats och hur samarbetet med vårdnadshavarna ordnats"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Aiemman varhaiskasvatussuunnitelman tavoitteet ja toimenpiteet"
                VasuLanguage.SV -> "Mål och åtgärder i den föregående barnets plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden toteutuminen"
                        VasuLanguage.SV -> "Genomförande av målen"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Kirjoita tähän lyhyesti niistä asioista, joita edellisessä vasukeskustelussa, yhdessä huoltajien kanssa asetitte pedagogisen toiminnan tavoitteiksi. Jos jatkat toisen tekemää suunnitelmaa, mainitse siitä ja muista laittaa omat nimikirjaimesi tekstin perään."
                        VasuLanguage.SV -> "Skriv här kortfattat om vad ni tillsammans med vårdnadshavarna satte som mål för den pedagogiska verksamheten i det föregående samtalet för barnets plan. Om du fortsätter en plan som utarbetats av någon annan, ange det och kom ihåg att skriva dina egna initialer efter texten."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapsen edellisestä vasusta"
                        VasuLanguage.SV -> "Andra iakttagelser om föregående barnets plan för småbarnspedagogik"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
                VasuLanguage.SV -> "Mål för den pedagogiska verksamheten och åtgärder för att uppnå målen"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                        VasuLanguage.SV -> "Barnets styrkor, intressen och behov samt hur man beaktar dem"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Keskustelkaa tiimissä otsikossa mainituista asioista niin, että kaikki tiimin jäsenet tuovat ilmi oman näkemyksensä havaintojen ja pedagogisen dokumentoinnin perusteella. Kirjaa lyhyt yhteenveto tähän. Jos et löydä juuri sopivaa \"lokeroa\" mielestänne tärkeän asian kirjaamiselle, palatkaa vasutekstin äärelle tai jutelkaa esimiehenne/työkavereittenne kanssa. Luo huoltajien kanssa käytävään keskusteluun miellyttävä ilmapiiri. Valmistaudu niin, että myös hankalista asioista on mahdollista puhua."
                        VasuLanguage.SV -> "Diskutera de frågor som nämns i rubriken så att alla teammedlemmar uttrycker sina synpunkter baserade på observationer och pedagogisk dokumentation. Skriv en kort sammanfattning här. Om du inte hittar ett lämpligt ställe för ett ärende som ni tycker är viktigt, gå tillbaka till texten för planen för småbarnspedagogik eller prata med er chef/era medarbetare. Skapa en trevlig atmosfär i samtalet med vårdnadshavarna. Förbered dig så att även svåra saker kan diskuteras."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteet henkilöstön pedagogiselle toiminnalle sekä toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                        VasuLanguage.SV -> "Mål för personalens pedagogiska verksamhet samt åtgärder och metoder för att uppnå målen"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Valitkaa yhdessä huoltajien kanssa 1-3 tavoitetta tulevalle pedagogiselle toiminnalle. Jotain sellaista, jolla on merkitystä juuri tämän lapsen kohdalla."
                        VasuLanguage.SV -> "Välj tillsammans med vårdnadshavarna 1–3 mål för framtida pedagogisk verksamhet. Målen för den pedagogiska verksamheten ska ha betydelse för just detta barn."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Mahdolliset muut kehityksen ja oppimisen tukeen liittyvät tarpeet sekä tuen toteuttamiseen liittyvät tavoitteet ja sovitut järjestelyt"
                        VasuLanguage.SV -> "Eventuella andra behov som rör stöd för utveckling och lärande samt mål och överenskomna arrangemang för genomförandet av stödet"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kirjoitetaan rakenteelliset tukitoimet ja muu sellainen lapsen hyvinvointiin liittyvä tuki, joka ei käy ilmi pedagogisen toiminnan kuvauksessa."
                        VasuLanguage.SV -> "Strukturella stödåtgärder och annat stöd som rör barnets välbefinnande och som inte framgår av beskrivningen av den pedagogiska verksamheten skrivs här."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.Followup(
                    title = when (lang) {
                        VasuLanguage.FI -> "Täydennykset ja jatkuva arviointi toimintakauden aikana"
                        VasuLanguage.SV -> ""
                    },
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden ja toimenpiteiden toteutumisen arviointia ja tarkennuksia toimintakauden aikana lapsen tarpeiden mukaan sekä mahdollinen huoltajien kanssa tehty yhteistyö"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Laadittu-tilassa olevaa varhaiskasvatussuunnitelmaa päivitetään pääasiassa lisäämällä uutta tekstiä Täydennykset ja jatkuva arviointi -osioon. Sinne voidaan mm. lisätä huomioita koskien lapsen kehitystä, arjen tapahtumia sekä huoltajien kanssa käytyjä keskusteluja."
                        VasuLanguage.SV -> ""
                    }
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen hyvinvoinnin tukemiseen liittyvät muut huomioitavat asiat"
                VasuLanguage.SV -> "Andra frågor som ska beaktas i samband med stöd för barnets välbefinnande"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Esimerkiksi päiväuniin, ruokailuun tai ulkoiluun liittyvät asiat"
                        VasuLanguage.SV -> "Frågor som rör bland annat vila, måltider eller utevistelse"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                VasuLanguage.SV -> "Övriga handlingar och planer"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatussuunnitelman laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                        VasuLanguage.SV -> "Eventuella övriga handlingar och planer som använts vid uppgörande av barnets plan för småbarnspedagogik"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tiedonsaajatahot"
                VasuLanguage.SV -> "Barnets plan för småbarnspedagogik delges till följande:"
            },
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla:"
                        VasuLanguage.SV -> "Barnets plan överlämnas med vårdnadshavarnas tillstånd till:"
                    },
                    options = listOf(
                        QuestionOption(
                            key = "Tulevaan esiopetusryhmään",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tulevaan esiopetusryhmään"
                                VasuLanguage.SV -> "den blivande förskolegruppen"
                            }
                        ),
                        QuestionOption(
                            key = "Neuvolaan",
                            name = when (lang) {
                                VasuLanguage.FI -> "Neuvolaan"
                                VasuLanguage.SV -> "barnrådgivningen"
                            }
                        ),
                        QuestionOption(
                            key = "Lasten terapiapalveluihin",
                            name = when (lang) {
                                VasuLanguage.FI -> "Lasten terapiapalveluihin"
                                VasuLanguage.SV -> "terapitjänsterna för barn"
                            }
                        ),
                        QuestionOption(
                            key = "Erikoissairaanhoitoon",
                            name = when (lang) {
                                VasuLanguage.FI -> "Erikoissairaanhoitoon"
                                VasuLanguage.SV -> "specialsjukvården"
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
                        VasuLanguage.SV -> "Annanstans, vart?"
                    },
                    multiline = false,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lisätietoja"
                VasuLanguage.SV -> "Ytterligare information"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lisätietoja suunnitelmaan, sen laatimiseen tai keskusteluihin liittyen"
                        VasuLanguage.SV -> "Ytterligare information om planen, dess uppgörandet eller samtalen"
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
