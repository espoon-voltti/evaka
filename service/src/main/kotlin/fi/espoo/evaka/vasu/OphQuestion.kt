// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.vasu.CurriculumType.DAYCARE
import fi.espoo.evaka.vasu.CurriculumType.PRESCHOOL

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

fun getDefaultTemplateContent(type: CurriculumType, lang: VasuLanguage) = when (type) {
    DAYCARE -> getDefaultVasuContent(lang)
    PRESCHOOL -> getDefaultLeopsContent()
}

fun getDefaultVasuContent(lang: VasuLanguage) = VasuContent(
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman laatijat"
                VasuLanguage.SV -> "Uppgörande av barnets plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Laatimisesta vastaava henkilö"
                        VasuLanguage.SV -> "Person som ansvarat för uppgörande av planen"
                    },
                    keys = when (lang) {
                        VasuLanguage.FI ->
                            listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike"), Field("Puhelinnumero"))
                        VasuLanguage.SV ->
                            listOf(Field("Förnamn"), Field("Efternamn"), Field("Titel"), Field("Telefonnummer"))
                    },
                    value = listOf("", "", "", "")
                ),
                VasuQuestion.MultiFieldList(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muu laatimiseen osallistunut henkilöstö/asiantuntijat"
                        VasuLanguage.SV -> "Övrig personal/sakkunniga som deltagit i uppgörandet av planen"
                    },
                    keys = when (lang) {
                        VasuLanguage.FI ->
                            listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike"), Field("Puhelinnumero"))
                        VasuLanguage.SV ->
                            listOf(Field("Förnamn"), Field("Efternamn"), Field("Titel"), Field("Telefonnummer"))
                    },
                    value = listOf()
                )
            )
        ),
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
                    },
                    value = emptyList()
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
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelmakeskustelu"
                VasuLanguage.SV -> "Samtal om barnets plan för småbarnspedagogik"
            },
            questions = listOfNotNull(
                VasuQuestion.Paragraph(
                    title = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatussuunnitelma on käyty läpi yhteistyössä huoltajien kanssa"
                        VasuLanguage.SV -> ""
                    },
                    paragraph = ""
                ),
                VasuQuestion.DateQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatuskeskustelun päivämäärä"
                        VasuLanguage.SV -> "Datum för samtalet om barnets plan för småbarnspedagogik"
                    },
                    trackedInEvents = true,
                    nameInEvents = "Varhaiskasvatussuunnitelmakeskustelu",
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Keskusteluun osallistuneet huoltajat"
                        VasuLanguage.SV -> "Vårdnadshavare som deltog i samtalet"
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä"
                        VasuLanguage.SV -> "Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan"
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Toteutumisen arviointi"
                VasuLanguage.SV -> "Utvärdering av genomförandet"
            },
            hideBeforeReady = true,
            questions = listOfNotNull(
                VasuQuestion.Paragraph(
                    title = when (lang) {
                        VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman arviointikeskustelu huoltajien kanssa"
                        VasuLanguage.SV -> ""
                    },
                    paragraph = ""
                ),
                VasuQuestion.DateQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Arviointikeskustelun päivämäärä"
                        VasuLanguage.SV -> "Datum för utvärderingssamtalet"
                    },
                    trackedInEvents = true,
                    nameInEvents = "Arviointikeskustelu",
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Arviointikeskusteluun osallistuneet huoltajat"
                        VasuLanguage.SV -> "Vårdnadshavare som deltog i utvärderingssamtalet"
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä"
                        VasuLanguage.SV -> "Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan"
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                        VasuLanguage.SV -> "Utvärdering av hur målen och åtgärderna har genomförts"
                    },
                    multiline = true,
                    value = ""
                )
            )
        )
    )
)

fun getDefaultLeopsContent() = VasuContent(
    sections = listOf(
        VasuSection(
            name = "Lapsen esiopetuksen oppimissuunnitelman laatiminen",
            questions = listOf(
                VasuQuestion.MultiField(
                    name = "Laatimisesta vastaava henkilö",
                    keys = listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike"), Field("Puhelinnumero")),
                    value = listOf("", "", "", "")
                ),
                VasuQuestion.MultiFieldList(
                    name = "Muu laatimiseen osallistunut henkilöstö/asiantuntijat",
                    keys = listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike"), Field("Puhelinnumero")),
                    value = listOf(listOf("", "", "", ""))
                ),
                VasuQuestion.MultiFieldList(
                    name = "Esiopetuksen ulkopuoliset yhteistyötahot, joista huoltajien kanssa on sovittu",
                    keys = listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike"), Field("Puhelinnumero")),
                    value = listOf(listOf("", "", "", ""))
                )
            )
        ),
        VasuSection(
            name = "Lapsen ja huoltajien osallisuus suunnitelman laadinnassa",
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Miten lapsen näkökulma ja mielipiteet on otettu huomioon",
                    info = "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi voi osallistua oman esiopetusvuotensa suunnitteluun ja arviointiin sekä omien oppimistavoitteidensa asettamiseen ja arviointiin.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Miten huoltajien näkemykset otetaan huomioon ja miten yhteistyö on järjestetty",
                    info = "Huoltajien kanssa keskustellaan lapsen esiopetusvuodesta, oppimisesta, kasvusta ja hyvinvoinnista suunnitelmallisissa keskustelutilanteissa lukuvuoden aikana sekä päivittäisissä arjen kohtaamisissa. Huoltajien on mahdollista pohtia oman lapsensa esiopetukseen liittyviä toiveita ja odotuksia etukäteen huoltajan oma osio -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista yhteisissä tilaisuuksissa.",
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = "Suunnitelma esiopetuksen toteuttamiselle",
            questions = listOf(
                VasuQuestion.Paragraph(
                    title = "",
                    paragraph = "Täytetään yhdessä huoltajien kanssa ja sisällytetään sekä huoltajien että ryhmän henkilöstön näkökulmat. Sisältää myös mahdollisen lapsen tarvitseman kasvun ja oppimisen tuen toteuttamisen suunnitelman."
                ),
                VasuQuestion.TextQuestion(
                    name = "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen",
                    info = "Tähän kohtaan kirjataan lapsen vahvuuksia, kiinnostuksen kohteita ja tarpeita, jotka otetaan huomioon esiopetusryhmän toiminnan suunnittelussa ja toteuttamisessa.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Tavoitteet henkilöstön pedagogiselle toiminnalle lapsen oppimisen ja kehityksen tukemiseksi",
                    info = "Tähän kohtaan kirjataan lapsen kasvun, oppimisen ja hyvinvoinnin kannalta oleelliset tavoitteet henkilöstön pedagogiselle toiminnalle. Henkilöstön toiminnalle asetetut tavoitteet voivat liittyä lapsen oppimiseen, työskentely- ja vuorovaikutustaitoihin. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus. Tavoitteita asetetaan enintään kolme.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Toimenpiteet, menetelmät ja oppimisympäristöön liittyvät järjestelyt tavoitteiden saavuttamiseksi",
                    info = "Tähän kirjataan, millä toimenpiteillä, menetelmillä ja oppimisympäristöön liittyvillä järjestelyillä edellisessä kohdassa asetettuihin tavoitteisiin pyritään arjen toiminnassa.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Muut mahdolliset lapsen kasvun ja oppimisen tukeen liittyvät tarpeet sekä tuen toteuttamiseen liittyvät tavoitteet ja sovitut järjestelyt",
                    info = "Jos lapsi tarvitsee kasvulleen tai oppimiselleen tukea, tähän kohtaan kirjataan tarvittaessa muut tuen näkökulmasta esiopetukseen vaikuttavat asiat. Pedagogiset ratkaisut, kuten oppimisympäristön muokkaamiseen sekä lapsen tukeen liittyvät ratkaisut: joustavat ryhmittelyt, samanaikaisopetus, opetusmenetelmät, työskentely- ja kommunikointitavat tms. Lapsen osa-aikaisen erityisopetuksen järjestäminen. Tukeen liittyvät asiat otetaan huomioon myös muita tämän osion kohtia kirjattaessa.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Seuranta ja arviointi",
                    info = "Sovitaan esiopetuksen henkilöstön ja huoltajien yhteisistä seurannan ja arvioinnin käytänteistä ja sovitaan, kuinka pedagogista dokumentointia hyödynnetään lapsen oppimisen, kasvun ja hyvinvoinnin seurannassa esiopetuksen pedagogisten tavoitteiden saavuttamiseksi.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.Followup(
                    title = "Täydennykset ja jatkuva arviointi toimintakauden aikana",
                    name = "Tavoitteiden ja toimenpiteiden toteutumisen arviointia ja tarkennuksia toimintakauden aikana lapsen tarpeiden mukaan sekä mahdollinen huoltajien kanssa tehty yhteistyö",
                    info = "Täydennä aina myös kunkin kirjauksen päivämäärä.",
                    value = emptyList()
                )
            )
        ),
        VasuSection(
            name = "Suomi toisena kielenä -opetus",
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = "Lapsen kielimaailma ja suomen kielen taitotason seuranta",
                    options = listOf(
                        QuestionOption(
                            key = "finnish_form2",
                            name = "Lapselle laaditaan kielipeda-työvälineen lomake 2: monikielisen lapsen kielimaailma"
                        ),
                        QuestionOption(
                            key = "finnish_form3",
                            name = "Lapselle laaditaan kielipeda-työvälineen lomake 3: lapsen suomenkielen taitotason seuranta"
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = listOf()
                ),
                VasuQuestion.RadioGroupQuestion(
                    name = "Suomen kielen taitotaso",
                    options = listOf(
                        QuestionOption(
                            key = "pre_a1",
                            name = "Esi A1"
                        ),
                        QuestionOption(
                            key = "a1",
                            name = "A1"
                        ),
                        QuestionOption(
                            key = "a2",
                            name = "A2"
                        ),
                        QuestionOption(
                            key = "b1",
                            name = "B1"
                        )
                    ),
                    value = null
                ),
                VasuQuestion.DateQuestion(
                    name = "Viimeisin seuranta",
                    info = "Päivitä tähän viimeisin taitotason seurannan ajankohta.",
                    trackedInEvents = false,
                    value = null
                ),
                VasuQuestion.MultiSelectQuestion(
                    name = "Perusopetukseen valmistavan opetuksen toteuttaminen",
                    options = listOf(
                        QuestionOption(
                            key = "preparatory",
                            name = "Tätä oppimissuunnitelmaa käytetään perusopetukseen valmistavan opetuksen toteuttamiseksi esiopetuksessa"
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = listOf()
                )
            )
        ),
        VasuSection(
            name = "Kasvun ja oppimisen tuki",
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = "Lapsen kasvuun ja oppimiseen liittyvä tuen tarve",
                    info = "Lapsen kasvuun ja oppimiseen liittyvä tuen tarve, asiakirjat ja käsittely. Täytä tämä kohta jos lapsi on yleisen, tehostetun tai erityisen tuen tarpeessa. Jos lapsella ilmenee vaikeuksia oppimisessaan, on hänellä oikeus saada osa-aikaista erityisopetusta muun esiopetuksen ohessa kaikilla tuen tasoilla. Vaikeudet voivat liittyä kielellisiin, matemaattisiin tai motorisiin taitoihin tai oman toiminnan ohjaukseen, tarkkaavaisuuteen tai vuorovaikutukseen. Osa-aikaisen erityisopetuksen tavoitteena on vahvistaa lapsen oppimisen edellytyksiä, ehkäistä kehityksen ja oppimisen vaikeuksia. Osa-aikaisen erityisopetuksen tarve arvioidaan ja suunnitellaan yhteistyössä esiopettajien ja varhaiskasvatuksessa toimivien erityisopettajien kanssa. Osa-aikaista erityisopetusta annetaan joustavin järjestelyin samanaikaisopetuksena, pienryhmissä tai yksilöllisesti. Tavoitteet sisällytetään lapsen saamaan muuhun opetukseen ja ne kuvataan kohdassa 4. Vaikutuksia arvioidaan opettajien yhteistyönä sekä lapsen että huoltajien kanssa. Huoltajille tiedotetaan yksikön toimintatavoista.",
                    options = listOf(
                        QuestionOption(
                            key = "tuki1",
                            name = "Lapsi saa osa-aikaista erityisopetusta"
                        ),
                        QuestionOption(
                            key = "tuki2",
                            name = "Pedagoginen arvio on tehty tehostetun tuen käynnistämiseksi"
                        ),
                        QuestionOption(
                            key = "tuki3",
                            name = "Tätä oppimissuunnitelmaa käytetään tehostetun tuen toteuttamiseksi"
                        ),
                        QuestionOption(
                            key = "tuki4",
                            name = "Pedagoginen selvitys on tehty erityisen tuen tarpeen arvioimiseksi"
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = listOf()
                ),
                VasuQuestion.TextQuestion(
                    name = "Tuen edellyttämä yhteistyö ja palvelut",
                    info = "Opiskeluhuollon ammattihenkilöiden ja muiden asiantuntijoiden antama tuki ja yhdessä sovittu vastuunjako. Esiopetukseen osallistumisen edellyttämät perusopetuslain mukaiset tulkitsemis- ja avustajapalvelut, muut opetuspalvelut, erityiset apuvälineet sekä eri toimijoiden vastuunjako. Lapsen osallistuminen muuhun varhaiskasvatukseen ja kuvaus yhteistyöstä toiminnan järjestäjän kanssa.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.MultiFieldList(
                    name = "Lapsen oppimissuunnitelman laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat",
                    info = "Esimerkiksi lapsen tilanteeseen liittyvät asiantuntijalausunnot. Huom, älä mainitse tässä mahdollisia diagnooseja.",
                    keys = listOf(Field("Asiakirja"), Field("Asiakirjan pvm")),
                    value = listOf()
                ),
                VasuQuestion.MultiSelectQuestion(
                    name = "Opiskeluhuolto",
                    info = "Esiopetuksen yksilökohtainen opiskeluhuolto sisältää opiskeluhuollon psykologin ja kuraattorin palvelut sekä tapauskohtaisesti esioppilaan tueksi koottavan monialaisen asiantuntijaryhmän. Myös neuvolan terveydenhoitaja voi osallistua tarpeen mukaan. Yksilökohtainen opiskeluhuolto perustuu vapaaehtoisuuteen ja sen toteuttaminen edellyttää huoltajien suostumuksen.",
                    options = listOf(
                        QuestionOption(
                            key = "opiskeluhuolto",
                            name = "Lapsi on ohjattu yksilökohtaisen opiskeluhuollon piiriin"
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = listOf()
                )
            )
        ),
        VasuSection(
            name = "Lapsen esiopetuksen oppimissuunnitelmakeskustelut",
            questions = listOf(
                VasuQuestion.Paragraph(
                    title = "Oppimissuunnitelma on laadittu yhteistyössä huoltajien kanssa",
                    paragraph = ""
                ),
                VasuQuestion.DateQuestion(
                    name = "Keskustelun päivämäärä",
                    trackedInEvents = true,
                    nameInEvents = "Oppimissuunnitelmakeskustelu",
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = "Keskusteluun osallistuneet huoltajat",
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = "Lisätietoja",
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Lisätietoja suunnitelmaan, sen laatimiseen tai keskusteluihin liittyen",
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = "Perusopetukseen siirtyminen",
            hideBeforeReady = true,
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Tavoitteiden ja toimenpiteiden toteutumisen arviointi",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.DateQuestion(
                    name = "Huoltajien kanssa käydyn keskustelun päivämäärä",
                    trackedInEvents = true,
                    nameInEvents = "Perusopetukseen siirtymiskeskustelu",
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = "Keskusteluun osallistuneet huoltajat",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.Paragraph(
                    title = "Lapsen esiopetuksen oppimissuunnitelman tiedonsiirtokeskustelu koulun kanssa",
                    paragraph = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Lapsen kasvun ja oppimisen kannalta oleellisimmat huomiot tiedoksi tulevalle koululle",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.DateQuestion(
                    name = "Tiedonsiirtokeskustelun päivämäärä",
                    trackedInEvents = false,
                    value = null
                ),
                VasuQuestion.MultiFieldList(
                    name = "Tiedonsiirtokeskustelun osallistujat",
                    keys = listOf(Field("Etunimi"), Field("Sukunimi"), Field("Nimike")),
                    value = listOf()
                )
            )
        ),
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
    val defaultSections = getDefaultTemplateContent(template.type, template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
