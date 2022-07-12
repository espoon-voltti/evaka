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
    PRESCHOOL -> getDefaultLeopsContent(lang)
}

fun getDefaultVasuContent(lang: VasuLanguage) = VasuContent(
    hasDynamicFirstSection = true,
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Perustiedot"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.StaticInfoSubSection(),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Yhteydenpitoon liittyviä lisätietoja"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Yhteydenpitoon liittyvät lisätiedot voivat esimerkiksi olla yhteishuoltajuuteen tai turvakieltoon liittyviä asioita."
                        VasuLanguage.SV -> ""
                    },
                    value = "",
                    multiline = true
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman laatiminen"
                VasuLanguage.SV -> ""
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
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten lapsen näkökulma ja mielipiteet otetaan huomioon"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi osallistuu oman varhaiskasvatuksensa suunnitteluun ja arviointiin. Lapsen varhaiskasvatussuunnitelmaa tehtäessä keskustellaan lapsen vahvuuksista, kiinnostuksen kohteista, osaamisesta ja yksilöllisistä tarpeista. Lapsen toiveita, mielipiteitä ja odotuksia selvitetään erilaisin tavoin lapsen ikä- ja kehitystaso huomioiden."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten huoltajien näkemykset otetaan huomioon ja yhteistyötä toteutetaan"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kuvataan, miten huoltajien kanssa keskustellaan lapsen oppimisesta, kasvusta ja hyvinvoinnista toimintavuoden aikana. Huoltajien on mahdollista pohtia oman lapsensa varhaiskasvatukseen liittyviä toiveita ja odotuksia ennen vasu-keskustelua Lapsi kotioloissa -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista erilaisissa varhaiskasvatusyksikön tilaisuuksissa.\nTähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Monialainen yhteistyö"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Yhteistyökumppanit ja yhteystiedot"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi lastenneuvolan tai lastensuojelun kanssa. Lisäksi kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Sovitut yhteistyötavat, vastuut ja palvelut"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kirjataan yhteisesti sovitut asiat. Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan\n\n• yhteistyö lapsen ja huoltajan kanssa\n• lapsen tuen toteuttamisen vastuut \n• erityisasiantuntijoiden palvelujen käyttö \n• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio\n• mahdollisten kuljetusten järjestelyt ja vastuut."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden ja toimenpiteiden toteutuminen"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Mitkä toiminnalle asetetut tavoitteet ja toimenpiteet ovat toteutuneet? Miten ne ovat toteutuneet? Mikä on edistänyt/estänyt tavoitteiden ja toimenpiteiden toteutumista? Arviointi kohdistuu toiminnan, järjestelyjen, oppimisympäristöjen ja pedagogiikan arviointiin, ei lapsen arviointiin. Arvioinnin yhteydessä henkilöstö sekä huoltaja ja lapsi pohtivat kuinka hyvin lapsen vasuun kirjatut kasvatukselle, opetukselle ja hoidolle asetetut tavoitteet ovat toteutuneet ja ovatko toimenpiteet olleet tarkoituksenmukaisia."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen tuen arviointi"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Ovatko lapselle annettu tuki ja tukitoimenpiteet olleet toimivia ja riittäviä? Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Miten sovitut yhteistyökäytännöt ovat toteutuneet? Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä lapsen vasua päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea lapsen vasua päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Lapsen vasu tulee arvioida ja tarkistaa vähintään kerran vuodessa tai useammin jos lapsen tarpeet sitä edellyttävät. Lapsen vasua arvioitaessa arviointi kohdistuu pedagogiikan toteutumiseen, oppimisympäristöihin ja toiminnan järjestelyihin sekä mahdolliseen tuen vaikuttavuuteen ja tukitoimien toteutumiseen. Lapsen vasun tarkistaminen perustuu lapsen vasun arviointiin yhdessä lapsen ja huoltajan kanssa. Tarkoituksena on varmistaa, että lapsen vasusta muodostuu jatkumo. Tässä osiossa tarkastellaan aiemmin laadittua lapsen vasua ja arvioidaan siihen kirjattujen tavoitteiden toteutumista. Mikäli lapsen vasua ollaan laatimassa ensimmäistä kertaa, tätä arviointia ei luonnollisesti tehdä. Lapsen vasun tavoitteita sekä niiden toteuttamista seurataan ja arvioidaan säännöllisesti."
                        VasuLanguage.SV -> ""
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
                        VasuLanguage.FI -> "Tähän kuvataan lapsen keskeiset vahvuudet ja kiinnostuksen kohteet sekä tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Kieleen ja kulttuuriin liittyviä tarkentavia näkökulmia"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kohdassa kirjataan, miten edistetään monipuolisesti vieraskielisten ja monikielisten lasten kielitaidon sekä kieli- ja kulttuuri-identiteettien ja itsetunnon kehittymistä. Huoltajien kanssa keskustellaan myös lapsen oman äidinkielen/äidinkielien tukemisesta."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Mahdolliset lapsen kehityksen, oppimisen ja hyvinvoinnin tukeen liittyvät tarpeet sekä lapsen tuen toteuttamiseen liittyvät tuen muodot (pedagogiset, rakenteelliset ja hoidolliset)"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Lapsen vasua hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu lapsen vasussa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Lapsen vasua päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi lapsen vasuun kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen varhaiskasvatuksen järjestämisen näkökulmasta."
                        VasuLanguage.SV -> ""
                    },
                    keys = when (lang) {
                        VasuLanguage.FI ->
                            listOf(
                                Field(
                                    name = "Pedagogiset tuen muodot",
                                    info = "• varhaiskasvatuspäivän rakenteeseen ja päivärytmiin liittyvät ratkaisut \n" +
                                        "• oppimisympäristöihin liittyvät ratkaisut \n" +
                                        "• tarvittavat erityispedagogiset menetelmät \n" +
                                        "• vuorovaikutus- ja kommunikointitavat, esimerkiksi viittomien ja kuvien käyttö \n" +
                                        "• käytännöt, miten lapsi pääsee osalliseksi vertaisryhmän toimintaa, esimerkiksi esteettömyyden huomiointi."
                                ),
                                Field(
                                    name = "Rakenteelliset tuen muodot",
                                    info = "• tuen toteuttamiseen liittyvän osaamisen ja erityispedagogisen osaamisen vahvistaminen \n" +
                                        "• henkilöstön mitoitukseen ja rakenteeseen liittyvät ratkaisut \n" +
                                        "• lapsiryhmän kokoon ja ryhmärakenteeseen liittyvät ratkaisut \n" +
                                        "• tulkitsemis- ja avustamispalvelut sekä apuvälineiden käyttö \n" +
                                        "• pien- tai erityisryhmä tai muu tarvittava ryhmämuoto \n" +
                                        "• varhaiskasvatuksen erityisopettajan osa- tai kokoaikainen opetus tai konsultaatio."
                                ),
                                Field(
                                    name = "Hoidolliset tuen muodot",
                                    info = "• perushoitoon, hoivaan ja avustamiseen liittyvät menetelmät \n" +
                                        "• terveydenhoidolliset tarpeet, esimerkiksi lapsen pitkäaikaissairauksien hoitoon, lääkitykseen, ruokavalioon ja liikkumiseen liittyvä avustaminen ja apuvälineet.\n"
                                )
                            )
                        VasuLanguage.SV ->
                            listOf(Field(""), Field(""), Field(""))
                    },
                    value = listOf("", "", ""),
                    separateRows = true
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteet henkilöstön pedagogiselle toiminnalle"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Olennaista on kirjata tavoitteet lapsen kasvatukselle, opetukselle ja hoidolle. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja oppimisen alueita. Lisäksi tähän kirjataan mahdolliset kehityksen, oppimisen ja hyvinvoinnin tuen kannalta merkitykselliset tavoitteet. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.RadioGroupQuestion(
                    name = "Lapsen tuen taso jatkossa",
                    options = listOf(
                        QuestionOption(
                            key = "general",
                            name = when (lang) {
                                VasuLanguage.FI -> "Yleinen tuki"
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "",
                            name = when (lang) {
                                VasuLanguage.FI -> "Lapsen tuen toteuttamista koskeva hallintopäätös"
                                VasuLanguage.SV -> ""
                            },
                            isIntervention = true,
                            info = when (lang) {
                                VasuLanguage.FI -> "Tämä kohta kirjataan, jos lapsen tuesta on annettu hallintopäätös. Lapsen vasuun kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "during_range",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tukipalvelut ajalla"
                                VasuLanguage.SV -> ""
                            },
                            dateRange = true
                        ),
                        QuestionOption(
                            key = "intensified",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tehostettu tuki"
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "special",
                            name = when (lang) {
                                VasuLanguage.FI -> "Erityinen tuki"
                                VasuLanguage.SV -> ""
                            }
                        )
                    ),
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muita huomioita"
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.Followup(
                    title = when (lang) {
                        VasuLanguage.FI -> "Tarkennuksia toimintavuoden aikana lapsen tarpeiden mukaan"
                        VasuLanguage.SV -> ""
                    },
                    name = when (lang) {
                        VasuLanguage.FI -> "Päivämäärä ja kirjaus"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tämän kohdan tarkoituksena on varmistaa lapsen vasuun kirjattujen tavoitteiden ja toimenpiteiden toteutumisen jatkuva arviointi. Jatkuvalla arvioinnilla tarkoitetaan havainnoinnin ja pedagogisen dokumentoinnin avulla tarkennettavia tavoitteita ja toimenpiteitä. Näistä keskustellaan huoltajien kanssa päivittäisissä kohtaamisissa. Jatkuvan arvioinnin avulla lapsen vasu pysyy ajan tasalla."
                        VasuLanguage.SV -> ""
                    },
                    value = emptyList(),
                    continuesNumbering = true
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Muut mahdolliset lapsen varhaiskasvatuksessa huomioitavat asiat"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muita huomioita"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Tähän osioon kirjataan muita huomioitavia asioita, kuten esimerkiksi lepoon, ruokailuun tai pukemiseen liittyvät asiat.\n" +
                                "Keskustele tarvittaessa huoltajien ajatuksista tyttöjen ympärileikkauksesta, ks. erillinen ohje Tyttöjen sukuelinten silpomisen estäminen. Tähän kirjataan huoltajien ajatukset asiasta. Jos huoli herää, toimi em. ohjeistuksen mukaan."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Lapsen vasun laatimisessa voidaan hyödyntää muita mahdollisia suunnitelmia kuten esimerkiksi lapsen vasun liitteenä oleva lääkehoitosuunnitelma ja Hyve4-lomakkeet."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tiedonsaajatahot"
                VasuLanguage.SV -> "Barnets plan för småbarnspedagogik delges till följande"
            },
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla"
                        VasuLanguage.SV -> "Barnets plan överlämnas med vårdnadshavarnas tillstånd till"
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
                    value = emptyList(),
                    textValue = emptyMap()
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
                    nameInEvents = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatussuunnitelmakeskustelu"
                        VasuLanguage.SV -> ""
                    },
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
                VasuLanguage.FI -> "Seuranta- ja arviointiajankohdat"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Ajankohdat ja kuvaus"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan huoltajan kanssa yhdessä sovittu jatkosuunnitelma ja milloin suunnitelmaa seuraavan kerran arvioidaan."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        )
    )
)

fun getDefaultLeopsContent(lang: VasuLanguage) = VasuContent(
    hasDynamicFirstSection = true,
    sections = listOf(
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Perustiedot"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.StaticInfoSubSection(),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Yhteydenpitoon liittyviä lisätietoja"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Yhteydenpitoon liittyvät lisätiedot voivat esimerkiksi olla yhteishuoltajuuteen tai turvakieltoon liittyviä asioita."
                        VasuLanguage.SV -> ""
                    },
                    value = "",
                    multiline = true
                )
            )
        ),
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
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten lapsen näkökulma ja mielipiteet otetaan huomioon ja lapsi on osallisena prosessissa"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi voi osallistua oman esiopetusvuotensa sekä omien oppimistavoitteidensa suunnitteluun ja arviointiin. Lapsen esiopetuksen oppimissuunnitelmaa (leops) tehtäessä keskustellaan lapsen vahvuuksista, kiinnostuksen kohteista, osaamisesta, yksilöllisistä tarpeista ja oppimisesta. Lapsen toiveita, mielipiteitä ja odotuksia selvitetään erilaisin tavoin."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten huoltajien näkemykset otetaan huomioon ja yhteistyötä toteutetaan"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Tässä tarkoituksena on varmistaa, että huoltajilla on mahdollisuus osallistua suunnitelman laadintaan ja arviointiin. Huoltajien kanssa keskustellaan lapsen esiopetusvuodesta, oppimisesta, kasvusta ja hyvinvoinnista suunnitelmallisissa keskustelutilanteissa lukuvuoden aikana sekä päivittäisissä arjen kohtaamisissa. Huoltajien on mahdollista pohtia oman lapsensa esiopetukseen liittyviä toiveita ja odotuksia etukäteen Lapsi kotioloissa -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista yhteisissä tilaisuuksissa.\n" +
                                "Tähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Monialainen yhteistyö"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Yhteistyökumppanit ja yhteystiedot"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi opiskeluhuollon, lastenneuvolan, koulujen ja lastensuojelun kanssa. Tähän kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Sovitut yhteistyötavat, vastuut ja palvelut"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Tähän kirjataan yhteisesti sovitut asiat. Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan \n" +
                                "• yhteistyö lapsen ja huoltajan kanssa \n" +
                                "• lapsen tuen toteuttamisen vastuut \n" +
                                "• erityisasiantuntijoiden palvelujen käyttö \n" +
                                "• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio \n" +
                                "• mahdollisten kuljetusten järjestelyt ja vastuut.\n"
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen esiopetuksen oppimissuunnitelman tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                VasuLanguage.SV -> ""
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden ja toimenpiteiden toteutuminen"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Mitkä toiminnalle asetetut tavoitteet ja toimenpiteet ovat toteutuneet? Miten ne ovat toteutuneet? Mikä on edistänyt/estänyt tavoitteiden ja toimenpiteiden toteutumista? Arviointi kohdistuu toiminnan, järjestelyjen, oppimisympäristöjen ja pedagogiikan arviointiin. Arvioinnin avulla seurataan lapsen oppimista, kasvua ja hyvinvointia ja kuinka pedagogiselle toiminnalle asetetut tavoitteet ovat niitä tukeneet."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen tuen arviointi"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Ovatko lapselle annettu tuki ja tukitoimenpiteet olleet toimivia ja riittäviä? Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Miten sovitut yhteistyökäytännöt ovat toteutuneet? \n" +
                                "Tässä kohdassa huomioidaan seuraavat asiat, jos lapsi on liittyvässä varhaiskasvatuksessa:\n" +
                                "Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä leopsia päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea leopsia päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta tai esiopetuksen oppimissuunitelmasta"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Lapsen esiopetuksen oppimissuunnitelma tulee tehdä, arvioida ja tarkistaa vähintään kaksi kertaa vuodessa tai useammin, jos lapsen tarpeet sitä edellyttävät. Leopsia arvioitaessa arviointi kohdistuu pedagogiikan toteutumiseen, oppimisympäristöihin ja toiminnan järjestelyihin sekä mahdolliseen tuen vaikuttavuuteen ja tukitoimien toteutumiseen. Lapsen tuki tulee tarkistaa esiopetuksen alkaessa. \n" +
                                "Leopsin tarkistaminen perustuu arviointiin yhdessä lapsen ja huoltajan kanssa. Tarkoituksena on varmistaa, että lapsen vasusta ja leopsista muodostuu jatkumo. Tässä osiossa voidaan tarkastella aiemmin laadittua lapsen vasua ja yhdessä huoltajan kanssa nostaa keskeisiä asioita leopsiin. Leopsin tavoitteita sekä niiden toteuttamista seurataan ja arvioidaan säännöllisesti."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi",
            questions = listOf(
                VasuQuestion.Paragraph(
                    title = "",
                    paragraph = "Tavoitteet ja toimenpiteet koskevat esiopetuksen ja liittyvän varhaiskasvatuksen kokonaisuutta sisältäen lapsen tuen."
                ),
                VasuQuestion.TextQuestion(
                    name = "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen",
                    info = "Tähän kuvataan lapsen keskeiset vahvuudet ja kiinnostuksen kohteet sekä tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = "Kieleen ja kulttuuriin liittyviä tarkentavia näkökulmia",
                    info = "Tässä kohdassa kirjataan, miten edistetään monipuolisesti vieraskielisten ja monikielisten lasten kielitaidon sekä kieli- ja kulttuuri-identiteettien ja itsetunnon kehittymistä. Huoltajien kanssa keskustellaan myös lapsen oman äidinkielen/äidinkielien tukemisesta.\n" +
                        "KieliPeda-työvälineen osat 2 ja 3 ovat leopsin liitteitä. Tähän kohtaan merkitään niistä nousevat keskeiset asiat, kuten lapsen kielimaailma ja suomen kielen taidon kehittyminen. Suomen kielen taitotaso merkitään kohtaan 5.3.\n" +
                        "Tähän kohtaan voi myös kirjata kieleen ja kulttuuriin liittyviä muita tarkentavia näkökulmia pedagogisiin tavoitteisiin ja toimenpiteisiin liittyen esimerkiksi saamen kieleen, romanikieleen ja viittomakieleen.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.CheckboxQuestion(
                    name = "Tätä oppimissuunnitelmaa käytetään perusopetukseen valmistavan opetuksen toteuttamiseksi",
                    value = false,
                    info = "Tähän kohtaan merkitään rasti, jos tätä oppimissuunnitelmaa käytetään perusopetukseen valmistavan opetuksen toteuttamiseksi.",
                    notNumbered = true
                ),
                VasuQuestion.Paragraph(
                    title = "Lapsen tuen tarve ja tuen toteuttaminen",
                    paragraph = "Tuen järjestämisen lähtökohtana on lapsen kokonaisen päivän huomioiminen."
                ),
                VasuQuestion.TextQuestion(
                    name = "Lapsen tukeen liittyvät tarpeet ja tuen järjestämiseen liittyvät asiat",
                    info = "Tähän kohtaan kirjataan lapsen tukeen liittyvät tarpeet, jotka heijastuvat toiminnalle asetettaviin tavoitteisiin sekä toimenpiteisiin ja menetelmiin tavoitteiden saavuttamiseksi.\n" +
                        "Lisäksi kirjataan esiopetukseen osallistumisen edellyttämät perusopetuslain mukaiset tulkitsemis- ja avustajapalvelut ja erityiset apuvälineet, joista on tehty päätös. \n" +
                        "Kirjaa tähän myös se, miten huoltajat tukevat lasta sovituissa asioissa.",
                    multiline = true,
                    value = "",
                ),
                VasuQuestion.MultiSelectQuestion(
                    name = "Esiopetusta koskeva tuen tarve ja pedagogiset asiakirjat",
                    info = "Jos lapsella ilmenee vaikeuksia oppimisessaan, on hänellä oikeus saada osa-aikaista erityisopetusta muun esiopetuksen ohessa kaikilla tuen tasoilla. Osa-aikaisen erityisopetuksen tavoitteena on vahvistaa lapsen oppimisen edellytyksiä, ehkäistä kehityksen ja oppimisen vaikeuksia. Osa-aikaisen erityisopetuksen tarve arvioidaan ja suunnitellaan yhteistyössä esiopettajan ja varhaiskasvatuksen erityisopettajan kanssa. Osa-aikaista erityisopetusta annetaan joustavin järjestelyin samanaikaisopetuksena, pienryhmissä tai yksilöllisesti. Tavoitteet sisällytetään lapsen saamaan muuhun opetukseen. Vaikutuksia arvioidaan opettajien yhteistyönä\n" +
                        "sekä lapsen että huoltajien kanssa. Huoltajille tiedotetaan yksikön toimintatavoista.\n" +
                        "\n" +
                        "Pedagoginen arvio\n" +
                        "• Laaditaan tehostettua tukea varten, kun ilmenee, ettei yleinen tuki esiopetuksessa ole lapselle riittävää.\n" +
                        "• Tehdään tehostettua tukea varten lapsen perusopetukseen siirtymistä valmisteltaessa.\n" +
                        "• Laatimisesta vastaa esiopettaja.\n" +
                        "\n" +
                        "Pedagoginen selvitys\n" +
                        "• Laaditaan erityistä tukea varten lapselle, jolle tehostettu tuki ei riitä.\n" +
                        "• Laaditaan tarvittaessa esiopetusvuoden aikana lapsen perusopetukseen siirtymistä valmisteltaessa.\n" +
                        "• Laatimisesta vastaa esiopettaja.\n" +
                        "Ks. erilliset ohjeet pedagogisen arvion ja selvityksen lomakkeista.",
                    value = emptyList(),
                    options = listOf(
                        QuestionOption(
                            key = "partTimeSpecialNeedsEducation",
                            name = "Lapsi saa osa-aikaista erityisopetusta."
                        ),
                        QuestionOption(
                            key = "pedagogicalEvaluationMade",
                            name = "Pedagoginen arvio on tehty tehostetun tuen käynnistämiseksi, pvm.",
                            date = true,
                            subText = "Tätä oppimissuunnitelmaa käytetään tehostetun tuen toteuttamiseksi."
                        ),
                        QuestionOption(
                            key = "pedagogicalStatementMade",
                            name = "Pedagoginen selvitys on tehty erityisen tuen tarpeen arvioimiseksi."
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    textValue = emptyMap()
                ),
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tuen muodot, jos lapsi on liittyvässä varhaiskasvatuksessa"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Leopsia hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu leopsissa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Leopsia päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi leopsiin kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen esiopetuksen ja liittyvän varhaiskasvatuksen järjestämisen näkökulmasta."
                        VasuLanguage.SV -> ""
                    },
                    keys = when (lang) {
                        VasuLanguage.FI ->
                            listOf(
                                Field(
                                    name = "Pedagogiset tuen muodot",
                                    info = "• päivän rakenteeseen ja päivärytmiin liittyvät ratkaisut \n" +
                                        "• oppimisympäristöihin liittyvät ratkaisut \n" +
                                        "• tarvittavat erityispedagogiset menetelmät \n" +
                                        "• vuorovaikutus- ja kommunikointitavat, esimerkiksi viittomien ja kuvien käyttö \n" +
                                        "• käytännöt, miten lapsi pääsee osalliseksi vertaisryhmän toimintaa, esimerkiksi esteettömyyden huomiointi."
                                ),
                                Field(
                                    name = "Rakenteelliset tuen muodot",
                                    info = "• tuen toteuttamiseen liittyvän osaamisen ja erityispedagogisen osaamisen vahvistaminen \n" +
                                        "• henkilöstön mitoitukseen ja rakenteeseen liittyvät ratkaisut \n" +
                                        "• lapsiryhmän kokoon ja ryhmärakenteeseen liittyvät ratkaisut \n" +
                                        "• tulkitsemis- ja avustamispalvelut sekä apuvälineiden käyttö \n" +
                                        "• pien- tai erityisryhmä tai muu tarvittava ryhmämuoto \n" +
                                        "• varhaiskasvatuksen erityisopettajan osa- tai kokoaikainen opetus tai konsultaatio."
                                ),
                                Field(
                                    name = "Hoidolliset tuen muodot",
                                    info = "• perushoitoon, hoivaan ja avustamiseen liittyvät menetelmät \n" +
                                        "• terveydenhoidolliset tarpeet, esimerkiksi lapsen pitkäaikaissairauksien hoitoon, lääkitykseen, ruokavalioon ja liikkumiseen liittyvä avustaminen ja apuvälineet."
                                )
                            )
                        VasuLanguage.SV ->
                            listOf(Field(""), Field(""), Field(""))
                    },
                    value = listOf("", "", ""),
                    separateRows = true
                ),
                VasuQuestion.TextQuestion(
                    name = "Tavoitteet henkilöstön pedagogiselle toiminnalle",
                    info = "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Henkilöstön toiminnalle asetetut tavoitteet voivat liittyä mm. lapsen oppimiseen, työskentely- ja vuorovaikutustaitoihin. Olennaista on kirjata tavoitteet lapsen kokonaiselle päivälle huomioiden mahdolliset lapsen tuen kannalta merkitykselliset asiat. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja opetuksen yhteisiä tavoitteita. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus.",
                    value = "",
                    multiline = true
                ),
                VasuQuestion.TextQuestion(
                    name = "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi",
                    info = "Tähän kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista.",
                    value = "",
                    multiline = true
                ),
                VasuQuestion.RadioGroupQuestion(
                    name = "Lapsen tuen taso jatkossa",
                    options = listOf(
                        QuestionOption(
                            key = "general",
                            name = when (lang) {
                                VasuLanguage.FI -> "Yleinen tuki"
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "",
                            name = when (lang) {
                                VasuLanguage.FI -> "Lapsen tuen toteuttamista koskeva hallintopäätös esiopetuksessa ja liittyvässä varhaiskasvatuksessa"
                                VasuLanguage.SV -> ""
                            },
                            isIntervention = true,
                            info = when (lang) {
                                VasuLanguage.FI -> "Tähän kohtaan kirjataan, jos lapsen tuesta esiopetuksessa ja/tai liittyvässä varhaiskasvatuksessa on annettu hallintopäätös. Leopsiin kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "during_range",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tukipalvelut ajalla"
                                VasuLanguage.SV -> ""
                            },
                            dateRange = true
                        ),
                        QuestionOption(
                            key = "intensified",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tehostettu tuki"
                                VasuLanguage.SV -> ""
                            }
                        ),
                        QuestionOption(
                            key = "special",
                            name = when (lang) {
                                VasuLanguage.FI -> "Erityinen tuki"
                                VasuLanguage.SV -> ""
                            }
                        )
                    ),
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = "Muita huomioita",
                    value = "",
                    multiline = true
                ),
                VasuQuestion.Followup(
                    title = when (lang) {
                        VasuLanguage.FI -> "Tarkennuksia esiopetusvuoden aikana lapsen tarpeiden mukaan"
                        VasuLanguage.SV -> ""
                    },
                    name = when (lang) {
                        VasuLanguage.FI -> "Päivämäärä ja kirjaus"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tämän kohdan tarkoituksena on varmistaa leopsiin kirjattujen tavoitteiden ja toimenpiteiden toteutumisen jatkuva arviointi. Jatkuvalla arvioinnilla tarkoitetaan havainnoinnin ja pedagogisen dokumentoinnin avulla tarkennettavia tavoitteita ja toimenpiteitä. Näistä keskustellaan huoltajien kanssa päivittäisissä kohtaamisissa. Jatkuvan arvioinnin avulla leops pysyy ajan tasalla. \n"
                        VasuLanguage.SV -> ""
                    },
                    value = emptyList(),
                    continuesNumbering = true
                )
            )
        ),
        VasuSection(
            name = "Opiskeluhuolto",
            questions = listOf(
                VasuQuestion.CheckboxQuestion(
                    label = "Opiskeluhuolto",
                    name = "Lapsi on ohjattu yksilökohtaisen opiskeluhuollon piiriin",
                    value = false,
                    info = "Tähän merkitään rasti, jos lapsi on ohjattu yksilökohtaisen opiskeluhuollon piiriin.\nKs. erilliset ohjeet opiskeluhuollosta."
                )
            )
        ),
        VasuSection(
            name = "Muut mahdolliset lapsen esiopetusvuonna huomioitavat asiat",
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Muita huomioita",
                    multiline = true,
                    value = "",
                    info = "Tähän osioon kirjataan muita huomioitavia asioita, kuten esimerkiksi lepoon, ruokailuun tai pukemiseen liittyvät asiat. \n" +
                        "Keskustellaan tarvittaessa huoltajien ajatuksista tyttöjen ympärileikkauksesta, ks. erillinen ohje Tyttöjen sukuelinten silpomisen estäminen. Tähän kirjataan huoltajien ajatukset asiasta. Jos huoli herää, toimi em. ohjeistuksen mukaan."
                )
            )
        ),
        VasuSection(
            name = "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat",
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Muut asiakirjat ja suunnitelmat",
                    multiline = true,
                    value = "",
                    info = "Lapsen esiopetuksen oppimissuunnitelman laatimisessa voidaan hyödyntää muita mahdollisia suunnitelmia kuten esimerkiksi lääkehoitosuunnitelmaa."
                )
            )
        ),
        VasuSection(
            name = "Lapsen esiopetuksen oppimissuunnitelmakeskustelu",
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
            name = "Tiedonsaajatahot",
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = "Tämä oppimissuunnitelma luovutetaan huoltajan/huoltajien luvalla",
                    options = listOf(
                        QuestionOption(
                            key = "tiedonsaajataho_tuleva_esiopetusryhma",
                            name = "Tulevaan esiopetusryhmään"
                        ),
                        QuestionOption(
                            key = "tiedonsaajataho_neuvola",
                            name = "Neuvolaan"
                        ),
                        QuestionOption(
                            key = "tiedonsaajataho_terapiapalveluihin",
                            name = "Lasten terapiapalveluihin"
                        ),
                        QuestionOption(
                            key = "tiedonsaajataho_erikoissairaanhoitoon",
                            name = "Erikoissairaanhoitoon"
                        ),
                        QuestionOption(
                            key = "tiedonsaajataho_muualle",
                            name = "Muualle, minne?",
                            textAnswer = true
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = emptyList(),
                    textValue = emptyMap()
                ),
                VasuQuestion.MultiFieldList(
                    name = "Päiväykset ja allekirjoitukset",
                    info = "Allekirjoittamalla annan suostumuksen tämän oppimissuunnitelman siirtämiseen yllämainituille tahoille.",
                    keys = listOf(Field("Päiväys"), Field("Huoltajan allekirjoitus"), Field("Nimenselvennys")),
                    value = listOf(listOf("", "", ""))
                )
            ),
        ),
        VasuSection(
            name = "Seuranta- ja arviointiajankohdat",
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = "Ajankohdat ja kuvaus",
                    multiline = true,
                    value = "",
                    info = "Tähän kohtaan kirjataan huoltajan kanssa yhdessä sovittu jatkosuunnitelma ja milloin suunnitelmaa seuraavan kerran arvioidaan."
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
    val defaultSections = getDefaultTemplateContent(template.type, template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
