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
                VasuLanguage.FI -> "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat "
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
                    info = "Lomake 2 tehdään kaikille kaksi- ja monikielisille lapsille. Lomake 3 tehdään kaksi- ja monikielisille lapsille, joiden äidinkieli ei ole suomi tai jotka vasta harjoittelevat suomen kieltä.",
                    options = listOf(
                        QuestionOption(
                            key = "finnish_form2",
                            name = "Lapselle laaditaan KieliPeda-työvälineen lomake 2: monikielisen lapsen kielimaailma"
                        ),
                        QuestionOption(
                            key = "finnish_form3",
                            name = "Lapselle laaditaan KieliPeda-työvälineen lomake 3: lapsen suomenkielen taitotason seuranta"
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    value = listOf(),
                    textValue = emptyMap()
                ),
                VasuQuestion.RadioGroupQuestion(
                    name = "Suomen kielen taitotaso",
                    options = listOf(
                        QuestionOption(
                            key = "pre_a1",
                            name = "Esi-A1"
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
                    value = listOf(),
                    textValue = emptyMap()
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
                    value = listOf(),
                    textValue = emptyMap()
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
                    value = listOf(),
                    textValue = emptyMap()
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
                    info = "Allekirjoittamalla annan suostumuksen tämän oppimissuunnitelman siirtämiseen yllämainituille tahoille",
                    keys = listOf(Field("Päiväys"), Field("Huoltajan allekirjoitus"), Field("Nimenselvennys")),
                    value = listOf(listOf("", "", ""))
                )
            ),
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
                    paragraph = "Lähtökohtaisesti pyydämme huoltajilta luvan lapsen esiopetuksen oppimissuunnitelman luovuttamiselle toiselle varhaiskasvatuksen, esiopetuksen tai perusopetuksen järjestäjälle. Suunnitelma voidaan kuitenkin luovuttaa edellä mainituille tahoille myös ilman huoltajien lupaa, jos se on välttämätöntä lapsen varhaiskasvatuksen, esi- tai perusopetuksen järjestämiseksi (varhaiskasvatuslaki 41 §, perusopetuslaki 40 § ja 41 §)."
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
