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
                        VasuLanguage.SV -> "Ytterligare kontaktinformation"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Yhteydenpitoon liittyvät lisätiedot voivat esimerkiksi olla yhteishuoltajuuteen tai turvakieltoon liittyviä asioita."
                        VasuLanguage.SV -> "Ytterligare kontaktinformation kan gälla till exempel uppgifter om gemensam vårdnad eller säkerhetsförbud, t.ex. gällande spärrmarkering."
                    },
                    value = "",
                    multiline = true
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman laatiminen"
                VasuLanguage.SV -> "Uppgörande av barnets plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Laatimisesta vastaava henkilö"
                        VasuLanguage.SV -> "Person som ansvarar för uppgörandet"
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
                        VasuLanguage.SV -> "Övrig personal/sakkunniga som deltagit i uppgörandet"
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
                        VasuLanguage.SV -> "Hur har barnets perspektiv och åsikter beaktats och på vilket sätt är barnet delaktigt i uppgörandet av sin plan"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi osallistuu oman varhaiskasvatuksensa suunnitteluun ja arviointiin. Lapsen varhaiskasvatussuunnitelmaa tehtäessä keskustellaan lapsen vahvuuksista, kiinnostuksen kohteista, osaamisesta ja yksilöllisistä tarpeista. Lapsen toiveita, mielipiteitä ja odotuksia selvitetään erilaisin tavoin lapsen ikä- ja kehitystaso huomioiden."
                        VasuLanguage.SV -> "Här beskrivs de typer av samtal och aktiviteter genom vilka barnet deltar i planeringen och utvärderingen av sin småbarnspedagogik. Barnets styrkor, intressen, kunskaper och individuella behov diskuteras när barnets plan för småbarnspedagogik görs upp. Barnets önskemål, åsikter och förväntningar utreds på olika sätt med hänsyn till barnets ålder och utvecklingsnivå. Använd gärna Svenska bildningstjänsters stödmaterial."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Miten huoltajien näkemykset otetaan huomioon ja yhteistyötä toteutetaan"
                        VasuLanguage.SV -> "Vårdnadshavares synpunkter, samt hur samarbetet genomförs"
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Tässä kuvataan, miten huoltajien kanssa keskustellaan lapsen oppimisesta, kasvusta ja hyvinvoinnista toimintavuoden aikana. Huoltajien on mahdollista pohtia oman lapsensa varhaiskasvatukseen liittyviä toiveita ja odotuksia ennen vasu-keskustelua huoltajan oma osio -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista erilaisissa varhaiskasvatusyksikön tilaisuuksissa.\n" +
                                "Tähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan.\n" +
                                "Tarvittaessa mm. tulkkauspalveluihin liittyen lapsen pakolaistausta selvitetään osoitteesta pakolaiskorvaukset@espoo.fi , ks. erillinen ohje Tulkkauspalveluiden hankinta ja tilaaminen."
                        VasuLanguage.SV ->
                            "Här beskrivs hur barnets lärande, utveckling och välbefinnande diskuteras med vårdnadshavarna under verksamhetsåret. I sin egen del i barnets plan har vårdnadshavarna möjlighet att reflektera över sina önskemål och förväntningar på barnets småbarnspedagogik före samtalet om barnets plan för småbarnspedagogik.\n\n" +
                                "Här kan antecknas önskemål och överenskommelser gällande familjens språkliga, kulturella eller åskådningsmässiga bakgrund, såsom hemspråk, användning av tolkningstjänster eller hur frågor kring livsåskådning ska diskuteras. Vid behov, till exempel i anslutning till tolkningstjänster, utreds barnets flyktingstatus på adressen pakolaiskorvaukset@espoo.fi. Se anvisningen om beställning av tolktjänster."
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Monialainen yhteistyö"
                VasuLanguage.SV -> "Sektorsövergripande samarbete"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Yhteistyökumppanit ja yhteystiedot"
                        VasuLanguage.SV -> "Samarbetspartners och kontaktuppgifter"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi lastenneuvolan tai lastensuojelun kanssa. Lisäksi kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot."
                        VasuLanguage.SV -> "Här kan antecknas genomförandet av sektorsövergripande samarbete med till exempel barnrådgivningen eller barnskyddet. Här antecknas även organisationerna, namnen och kontaktuppgifterna till aktörerna inom det sektorsövergripande arbetet."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Sovitut yhteistyötavat, vastuut ja palvelut"
                        VasuLanguage.SV -> "Överenskomna samarbetsformer, ansvarsområden och tjänster"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kirjataan yhteisesti sovitut asiat. Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan\n\n• yhteistyö lapsen ja huoltajan kanssa\n• lapsen tuen toteuttamisen vastuut \n• erityisasiantuntijoiden palvelujen käyttö \n• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio\n• mahdollisten kuljetusten järjestelyt ja vastuut."
                        VasuLanguage.SV -> "Här antecknas gemensamma överenskommelser. Gällande det samarbete och de tjänster som det eventuella stödet kräver, beaktas\n\n• samarbetet med barnet och vårdnadshavaren\n• ansvarsfördelningen gällande genomförandet av barnets stöd\n• användningen av specialisttjänster\n• vägledningen av och konsultationer med sakkunniga inom social- och hälsovården samt övriga sakkunniga\n• ansvar för anordnandet av eventuella transporter."
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                VasuLanguage.SV -> "Utvärdering av hur målen uppnåtts och åtgärderna genomförts i barnets tidigare plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteiden ja toimenpiteiden toteutuminen"
                        VasuLanguage.SV -> "Beskrivning av hur målen har uppnåtts och åtgärderna har genomförts"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Mitkä toiminnalle asetetut tavoitteet ja toimenpiteet ovat toteutuneet? Miten ne ovat toteutuneet? Mikä on edistänyt/estänyt tavoitteiden ja toimenpiteiden toteutumista? Arviointi kohdistuu toiminnan, järjestelyjen, oppimisympäristöjen ja pedagogiikan arviointiin, ei lapsen arviointiin. Arvioinnin yhteydessä henkilöstö sekä huoltaja ja lapsi pohtivat kuinka hyvin lapsen vasuun kirjatut kasvatukselle, opetukselle ja hoidolle asetetut tavoitteet ovat toteutuneet ja ovatko toimenpiteet olleet tarkoituksenmukaisia."
                        VasuLanguage.SV -> "Vilka av de mål och åtgärder som fastställts för verksamheten har uppnåtts och genomförts? Hur har de uppnåtts och genomförts? Vad har bidragit till/förhindrat att målen och åtgärderna uppnåtts och genomförts? Utvärderingen fokuserar på att utvärdera verksamheten, åtgärderna, lärmiljöerna och pedagogiken, inte på en bedömning av barnet. Vid utvärderingen reflekterar personalen, vårdnadshavaren och barnet över hur väl de mål som satts upp för barnets fostran, undervisning och vård har uppnåtts och om åtgärderna har varit ändamålsenliga."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen tuen arviointi"
                        VasuLanguage.SV -> "Utvärdering av tidigare stödåtgärder"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Ovatko lapselle annettu tuki ja tukitoimenpiteet olleet toimivia ja riittäviä? Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Miten sovitut yhteistyökäytännöt ovat toteutuneet? Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä lapsen vasua päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea lapsen vasua päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                        VasuLanguage.SV -> "Utvärderingen av stödet omfattar en beskrivning av stödåtgärderna och en utvärdering av hur väl de fungerat. Har barnets stöd och stödåtgärderna som getts varit effektiva och tillräckliga? Hur har de överenskomna pedagogiska, strukturella och/eller vårdinriktade stödformerna genomförts och vilka effekter har de haft? Vilka stödåtgärder gynnar barnet i enlighet med dess individuella stödbehov, och varför?Stödbehovet samt stödets tillräcklighet, ändamålsenlighet och effekt ska utvärderas och följas upp, och barnets plan för småbarnspedagogik ska uppdateras alltid när stödbehovet ändras.  Om barnet får intensifierat eller särskilt stöd eller stödtjänster som en del av det allmänna stödet uppdateras barnets plan för småbarnspedagogik enligt innehållet i förvaltningsbeslutet. Detta fält fylls i om barnet har fått stöd."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta"
                        VasuLanguage.SV -> "Övriga kommentarer om barnets tidigare plan för småbarnspedagogik"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Lapsen vasu tulee arvioida ja tarkistaa vähintään kerran vuodessa tai useammin jos lapsen tarpeet sitä edellyttävät. Lapsen vasua arvioitaessa arviointi kohdistuu pedagogiikan toteutumiseen, oppimisympäristöihin ja toiminnan järjestelyihin sekä mahdolliseen tuen vaikuttavuuteen ja tukitoimien toteutumiseen. Lapsen vasun tarkistaminen perustuu lapsen vasun arviointiin yhdessä lapsen ja huoltajan kanssa. Tarkoituksena on varmistaa, että lapsen vasusta muodostuu jatkumo. Tässä osiossa tarkastellaan aiemmin laadittua lapsen vasua ja arvioidaan siihen kirjattujen tavoitteiden toteutumista. Mikäli lapsen vasua ollaan laatimassa ensimmäistä kertaa, tätä arviointia ei luonnollisesti tehdä. Lapsen vasun tavoitteita sekä niiden toteuttamista seurataan ja arvioidaan säännöllisesti."
                        VasuLanguage.SV -> "Barnets plan för småbarnspedagogik ska utvärderas och granskas minst en gång om året eller oftare om barnets behov kräver det. Utvärderingen av barnets plan för småbarnspedagogik fokuserar på genomförandet av pedagogiken, lärmiljöerna och åtgärderna för verksamheten samt det eventuella stödets effekt och genomförandet av stödåtgärderna. Barnets plan för småbarnspedagogik granskas utgående från den utvärdering av barnets plan som görs tillsammans med barnet och vårdnadshavare. Detta görs för att skapa kontinuitet i barnets småbarnspedagogik. Vid denna punkt granskas barnets tidigare plan för småbarnspedagogik och det görs en utvärdering av hur väl målen i planen uppnåtts. Om barnets plan för småbarnspedagogik utarbetas för första gången görs inte denna utvärdering. Målen i barnets plan, samt hur väl de uppnåtts, ska följas upp och utvärderas regelbundet."
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
            questions = listOfNotNull(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                        VasuLanguage.SV -> "Barnets styrkor, intressen och behov samt hur dessa beaktas i verksamheten"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kuvataan lapsen keskeiset vahvuudet ja kiinnostuksen kohteet sekä tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi."
                        VasuLanguage.SV ->
                            "Här beskrivs barnets styrkor, intressen och behov som grund för målsättningen och planeringen av verksamheten.\n\n" +
                                "Ex. Nelle är fysiskt aktiv och har ett stort rörelsebehov. Hen deltar aktivt i rörelsestunder och utevistelse och är skicklig på att klättra och springa. Vi tar Nelles rörelseglädje i beaktande då vi planerar verksamheten. Samlingarna förverkligar vi t.ex. på ett sådant sätt att rörelse är en naturlig del av dem och då gruppen förflyttar sig gör vi det på ett lekfullt sätt genom att t.ex. hoppa som grodor eller smyga som ninjor. "
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Kieleen ja kulttuuriin liittyviä tarkentavia näkökulmia"
                        VasuLanguage.SV -> "Mål och metoder för hur barnets kulturella- och språkliga identitet/identiteter bekräftas samt hur barnets språkutveckling stöds"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kohdassa kirjataan, miten edistetään monipuolisesti vieraskielisten ja monikielisten lasten kielitaidon sekä kieli- ja kulttuuri-identiteettien ja itsetunnon kehittymistä. Huoltajien kanssa keskustellaan myös lapsen oman äidinkielen/äidinkielien tukemisesta."
                        VasuLanguage.SV -> "Här antecknas hur man bidrar till en mångsidig utveckling av språkkunskaperna, den språkliga och kulturella identiteten och självförtroendet hos fler– och tvåspråkiga barn, eller barn med ett främmande språk som modersmål. I den svenskspråkiga småbarnspedagogiken i Esbo är det vanligt med barn som hemma talar både finska och svenska. I detta fält antecknas hur man i verksamheten målmedvetet stöder och följer upp barnets språkutveckling i verksamhetsspråket, samtidigt som man bekräftar barnets båda språkliga identiteter. Använd barnets språkbarometer som stöd för en helhetsbild av barnets språkliga miljö."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Mahdolliset lapsen kehityksen, oppimisen ja hyvinvoinnin tukeen liittyvät tarpeet sekä lapsen tuen toteuttamiseen liittyvät tuen muodot (pedagogiset, rakenteelliset ja hoidolliset)"
                        VasuLanguage.SV -> "Barnets eventuella behov av stöd för utveckling, lärande och välbefinnande samt stödformerna för genomförandet av barnets stöd (pedagogiska, strukturella och vårdinriktade)"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Lapsen vasua hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu lapsen vasussa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Lapsen vasua päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi lapsen vasuun kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen varhaiskasvatuksen järjestämisen näkökulmasta."
                        VasuLanguage.SV -> "Här antecknas barnets eventuella behov av stöd samt de pedagogiska, strukturella och vårdinriktade stödformerna i anslutning till genomförandet av barnets stöd. Även barnets eventuella stödtjänster antecknas här. Barnets plan för småbarnspedagogik utnyttjas när man fattar förvaltningsbeslut om intensifierat eller särskilt stöd eller om stödtjänster inom allmänt stöd. Om barnets stödbehov har bedömts i barnets plan för småbarnspedagogik, ska bedömningen beaktas när förvaltningsbeslut om intensifierat eller särskilt stöd och beslut om stödtjänsterna inom allmänt stöd fattas. Barnets plan för småbarnspedagogik ska uppdateras i enlighet med innehållet i förvaltningsbeslutet. Dessutom antecknas i barnets plan för småbarnspedagogik eventuella social- och hälsovårdstjänster, såsom barnets rehabilitering, om de är väsentliga för ordnandet av barnets småbarnspedagogik."
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
                            listOf(
                                Field(
                                    name = "Pedagogiska stödformer",
                                    info = "• lösningar gällande dagliga rutiner och struktur inom småbarnspedagogiken\n" +
                                        "• lösningar gällande lärmiljöer\n" +
                                        "• specialpedagogiska metoder\n" +
                                        "• metoder för växelverkan och kommunikation, till exempel användning av tecken och bilder\n" +
                                        "• verksamhetssätt för hur barnet kan vara delaktigt i barngruppens verksamhet, till exempel beaktande av tillgänglighet (t.ex. delaktighet i leken/sociala gemenskapen, fysisk tillgänglighet, möjlighet att uttrycka sig på ett för barnet tillgängligt sätt etc.)"
                                ),
                                Field(
                                    name = "Strukturella stödformer",
                                    info = "• att stärka kunnandet och den specialpedagogiska kompetensen som berör genomförande av stödet\n" +
                                        "• lösningar angående personaldimensionering och personalstruktur\n" +
                                        "• lösningar som rör barngruppens storlek och gruppens sammansättning\n" +
                                        "• tolknings- och assistenttjänster samt användning av hjälpmedel\n" +
                                        "• smågrupp, specialgrupp eller annan gruppform enligt behoven\n" +
                                        "• konsultation eller undervisning som ges på hel- eller deltid av en speciallärare inom småbarnspedagogik"
                                ),
                                Field(
                                    name = "Vårdinriktade stödformer",
                                    info = "• metoder som berör grundläggande vård, omsorg och assistans\n" +
                                        "• verksamhet som tillgodoser behov av hälso- och sjukvård, till exempel hjälpmedel och assistans som hänför sig till barnets långtidssjukdomar, medicinering, kost och rörlighet\n"
                                )
                            )
                    },
                    value = listOf("", "", ""),
                    separateRows = true
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tavoitteet henkilöstön pedagogiselle toiminnalle"
                        VasuLanguage.SV -> "Mål för personalens pedagogiska verksamhet"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Olennaista on kirjata tavoitteet lapsen kasvatukselle, opetukselle ja hoidolle. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja oppimisen alueita. Lisäksi tähän kirjataan mahdolliset kehityksen, oppimisen ja hyvinvoinnin tuen kannalta merkitykselliset tavoitteet. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus."
                        VasuLanguage.SV ->
                            "Här anges centrala mål för personalens pedagogiska verksamhet. Målen ska beskriva hur man med den pedagogiska verksamheten och lärmiljöerna stöder barnets utveckling, lärande och välbefinnande. Barnets styrkor, intressen, färdigheter och behov ska beaktas då målen anges. Här beaktas även delområden inom mångsidig kompetens och lärområden. Det är viktigt att de mål som ställs upp utgår från barngruppen och dess situation, samt att de är konkreta och möjliga att utvärdera.\n\n" +
                                "I barnets plan ska flera mål som beaktar barnets färdigheter och hur de kan främjas i verksamheten anges. Nedan ges ett exempel på ett mål:\n\n" +
                                "Nelle har ett stort ordförråd och mycket att berätta - så mycket att hen inte alltid hinner lyssna. Vi strävar till att stärka Nelles förmåga till växelverkan och turtagning men ger även Nelle möjligheter att få utnyttja sin verbala förmåga på olika sätt."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                        VasuLanguage.SV -> "Åtgärder och metoder för att uppnå målen för den pedagogiska verksamheten"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tässä kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista."
                        VasuLanguage.SV ->
                            "Här antecknas de konkreta pedagogiska åtgärder och metoder som syftar till att uppnå de mål som angetts för den pedagogiska verksamheten. Metoderna ska beskrivas på en så konkret nivå att förverkligandet av dem kan utvärderas.\n\n" +
                                "Nedan ges exempel på metoder för att uppnå det mål som angavs för den pedagogiska verksamheten i punkt 5.4.\n\n" +
                                "1. Vi använder oss av små grupper för att stärka varje barns möjlighet till delaktighet och att bli hörda. Vi använder oss även av ett taltursdjur under samlingen som signalerar att det barn som har djuret pratar.\n" +
                                "2. För att ge Nelle möjlighet att använda och utveckla sin verbala förmåga arbetar hen med en digital bok på pekplattan. Hen kan själv fotografera, rita och spela in ljud samt berätta sagor eller om något hen gjort. Vi hjälper Nelle att leta reda på bokstäver på tangentbordet för att skriva rubriker eller korta meningar."
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.RadioGroupQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Lapsen tuen taso jatkossa"
                        VasuLanguage.SV -> "Barnets stödnivå i fortsättningen"
                    },
                    options = listOf(
                        QuestionOption(
                            key = "general",
                            name = when (lang) {
                                VasuLanguage.FI -> "Yleinen tuki"
                                VasuLanguage.SV -> "Allmänt stöd"
                            }
                        ),
                        QuestionOption(
                            key = "",
                            name = when (lang) {
                                VasuLanguage.FI -> "Lapsen tuen toteuttamista koskeva hallintopäätös"
                                VasuLanguage.SV -> "Förvaltningsbeslut om barnets stöd"
                            },
                            isIntervention = true,
                            info = when (lang) {
                                VasuLanguage.FI -> "Tämä kohta kirjataan, jos lapsen tuesta on annettu hallintopäätös. Lapsen vasuun kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                                VasuLanguage.SV -> "Här antecknas om det fattats ett förvaltningsbeslut om barnets stöd. Om förvaltningsbeslutet hävs, ska datumet för detta även antecknas i barnets plan för småbarnspedagogik. I fältet Övrigt att beakta kan man anteckna preciserande synpunkter gällande förvaltningsbeslutet."
                            }
                        ),
                        QuestionOption(
                            key = "during_range",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tukipalvelut ajalla"
                                VasuLanguage.SV -> "Stödtjänster för tiden"
                            },
                            dateRange = true
                        ),
                        QuestionOption(
                            key = "intensified",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tehostettu tuki"
                                VasuLanguage.SV -> "Intensifierat stöd"
                            }
                        ),
                        QuestionOption(
                            key = "special",
                            name = when (lang) {
                                VasuLanguage.FI -> "Erityinen tuki"
                                VasuLanguage.SV -> "Särskilt stöd"
                            }
                        )
                    ),
                    value = null
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muita huomioita"
                        VasuLanguage.SV -> "Övrigt att beakta"
                    },
                    multiline = true,
                    value = ""
                ),
                when (lang) {
                    VasuLanguage.FI -> VasuQuestion.Followup(
                        title = "Tarkennuksia toimintavuoden aikana lapsen tarpeiden mukaan",
                        name = "Päivämäärä ja kirjaus",
                        info = "Tämän kohdan tarkoituksena on varmistaa lapsen vasuun kirjattujen tavoitteiden ja toimenpiteiden toteutumisen jatkuva arviointi. Jatkuvalla arvioinnilla tarkoitetaan havainnoinnin ja pedagogisen dokumentoinnin avulla tarkennettavia tavoitteita ja toimenpiteitä. Näistä keskustellaan huoltajien kanssa päivittäisissä kohtaamisissa. Jatkuvan arvioinnin avulla lapsen vasu pysyy ajan tasalla.",
                        value = emptyList(),
                        continuesNumbering = true
                    )
                    VasuLanguage.SV -> null
                }
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Muut mahdolliset lapsen varhaiskasvatuksessa huomioitavat asiat"
                VasuLanguage.SV -> "Övrigt att beakta i barnets småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muita huomioita"
                        VasuLanguage.SV -> "Övrigt att beakta"
                    },
                    info = when (lang) {
                        VasuLanguage.FI ->
                            "Tähän osioon kirjataan muita huomioitavia asioita, kuten esimerkiksi lepoon, ruokailuun tai pukemiseen liittyvät asiat.\n" +
                                "Keskustele tarvittaessa huoltajien ajatuksista tyttöjen ympärileikkauksesta, ks. erillinen ohje Tyttöjen sukuelinten silpomisen estäminen. Tähän kirjataan huoltajien ajatukset asiasta. Jos huoli herää, toimi em. ohjeistuksen mukaan."
                        VasuLanguage.SV -> "Här antecknas övriga saker som ska beaktas, till exempel gällande vila, måltider eller påklädning."
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                VasuLanguage.SV -> "Eventuella övriga dokument och planer som använts vid uppgörandet av barnets plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                        VasuLanguage.SV -> "Övriga dokument och planer"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Lapsen vasun laatimisessa voidaan hyödyntää muita mahdollisia suunnitelmia, esimerkiksi lääkehoitosuunnitelmaa."
                        VasuLanguage.SV -> "Vid uppgörandet av barnets plan för småbarnspedagogik kan eventuella andra planer och dokument, såsom planen för läkemedelsbehandling, Hyve4-blanketter, språkbarometern m.m. som utgör bilagor till barnets plan för småbarnspedagogik, utnyttjas."
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Tiedonsaajatahot"
                VasuLanguage.SV -> "Överförande av barnets plan till berörda parter"
            },
            questions = listOf(
                VasuQuestion.MultiSelectQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla"
                        VasuLanguage.SV -> "Barnets plan kan med vårdnadshavares tillstånd överföras till"
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
                        VasuLanguage.SV -> "Övriga mottagare, vilka?"
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
                        VasuLanguage.SV -> "Barnets plan för småbarnspedagogik har gjorts upp i samarbete med vårdnadshavare."
                    },
                    paragraph = ""
                ),
                VasuQuestion.DateQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatuskeskustelun päivämäärä"
                        VasuLanguage.SV -> "Datum för samtalet om barnets plan"
                    },
                    trackedInEvents = true,
                    nameInEvents = when (lang) {
                        VasuLanguage.FI -> "Varhaiskasvatussuunnitelmakeskustelu"
                        VasuLanguage.SV -> "Samtalet om barnets plan"
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
                        VasuLanguage.FI -> "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä/huoltajien kuuleminen hallintopäätöstä koskien"
                        VasuLanguage.SV -> "Vårdnadshavares tankar kring innehållet i barnets plan/hörande av vårdnadshavare gällande förvaltningsbeslut"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan huoltajien kuuleminen, mikäli tuesta tehdään hallintopäätös."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                )
            )
        ),
        VasuSection(
            name = when (lang) {
                VasuLanguage.FI -> "Seuranta- ja arviointiajankohdat"
                VasuLanguage.SV -> "Tidpunkterna för uppföljning och utvärdering av barnets plan för småbarnspedagogik"
            },
            questions = listOf(
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Ajankohdat ja kuvaus"
                        VasuLanguage.SV -> "Tidpunkter och beskrivning"
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan huoltajan kanssa yhdessä sovittu jatkosuunnitelma ja milloin suunnitelmaa seuraavan kerran arvioidaan."
                        VasuLanguage.SV -> "Här antecknas hur man tillsammans med vårdnadshavarna går vidare med planen, samt datum för när planen ska utvärderas nästa gång."
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
                            "Tässä tarkoituksena on varmistaa, että huoltajilla on mahdollisuus osallistua suunnitelman laadintaan ja arviointiin. Huoltajien kanssa keskustellaan lapsen esiopetusvuodesta, oppimisesta, kasvusta ja hyvinvoinnista suunnitelmallisissa keskustelutilanteissa lukuvuoden aikana sekä päivittäisissä arjen kohtaamisissa. Huoltajien on mahdollista pohtia oman lapsensa esiopetukseen liittyviä toiveita ja odotuksia etukäteen huoltajan oma osio -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista yhteisissä tilaisuuksissa.\n" +
                                "Tähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan.\n" +
                                "Tarvittaessa mm. tulkkauspalveluihin liittyen lapsen pakolaistausta selvitetään osoitteesta pakolaiskorvaukset@espoo.fi , ks. erillinen ohje Tulkkauspalveluiden hankinta ja tilaaminen."
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
                        VasuLanguage.FI -> "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi opiskeluhuollon, lastenneuvolan, koulujen ja lastensuojelun kanssa. Tähän kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot. Lisäksi kirjataan yhteisesti sovitut asiat ja mahdollinen vastuunjako."
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
                        VasuLanguage.FI -> "Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan\n\n• yhteistyö lapsen ja huoltajan kanssa\n• lapsen tuen toteuttamisen vastuut \n• erityisasiantuntijoiden palvelujen käyttö \n• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio\n• mahdollisten kuljetusten järjestelyt ja vastuut\n• perusopetuksen järjestämässä esiopetuksessa lapsen mahdollinen osallistuminen liittyvään varhaiskasvatukseen ja kuvaus yhteistyöstä toimijoiden kesken."
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
                                "Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä leopsia päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea leopsia päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                        VasuLanguage.SV -> ""
                    },
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.TextQuestion(
                    name = when (lang) {
                        VasuLanguage.FI -> "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta tai esiopetuksen oppimissuunnitelmasta"
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
                    info = "Tähän kuvataan lapsen keskeiset vahvuudet, kiinnostuksen kohteet, oppimisvalmiudet ja tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi.",
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
                    title = "Lapsen tukeen liittyvät tarpeet, tuen järjestämiseen liittyvät asiat sekä tuen muodot (pedagogiset, rakenteelliset ja hoidolliset) esiopetuksessa ja liittyvässä varhaiskasvatuksessa",
                    paragraph = "Tuen järjestämisen lähtökohtana on lapsen kokonaisen päivän huomioiminen."
                ),
                VasuQuestion.TextQuestion(
                    name = "Lapsen tukeen liittyvät tarpeet esiopetuksessa ja liittyvässä varhaiskasvatuksessa",
                    info = "Tähän  kohtaan  kirjataan  lapsen  tukeen  liittyvät  tarpeet,  jotka  heijastuvat  toiminnalle  asetettaviin tavoitteisiin sekä toimenpiteisiin ja menetelmiin tavoitteiden saavuttamiseksi.\nKirjaa tähän myös se, miten huoltajat tukevat lasta sovituissa asioissa.",
                    multiline = true,
                    value = "",
                ),
                VasuQuestion.MultiSelectQuestion(
                    name = "Tuen järjestämiseen liittyvät asiat esiopetuksessa",
                    info = "Rastita tarvittavat kohdat.",
                    value = emptyList(),
                    options = listOf(
                        QuestionOption(
                            key = "partTimeSpecialNeedsEducation",
                            name = "Lapsi saa osa-aikaista erityisopetusta.",
                            info = "Jos lapsella ilmenee vaikeuksia oppimisessaan, on hänellä oikeus saada osa-aikaista erityisopetusta muun esiopetuksen ohessa kaikilla tuen tasoilla. Osa-aikaisen erityisopetuksen tavoitteena on vahvistaa lapsen oppimisen edellytyksiä, ehkäistä kehityksen ja oppimisen vaikeuksia. Osa-aikaisen erityisopetuksen tarve arvioidaan ja suunnitellaan yhteistyössä esiopettajan ja varhaiskasvatuksen erityisopettajan kanssa. Osa-aikaista erityisopetusta annetaan joustavin järjestelyin samanaikaisopetuksena, pienryhmissä tai yksilöllisesti. Tavoitteet sisällytetään lapsen saamaan muuhun opetukseen. Vaikutuksia arvioidaan opettajien yhteistyönä sekä lapsen että huoltajien kanssa. Huoltajille tiedotetaan yksikön toimintatavoista."
                        ),
                        QuestionOption(
                            key = "pedagogicalEvaluationMade",
                            name = "Pedagoginen arvio on tehty tehostetun tuen käynnistämiseksi, pvm.",
                            date = true,
                            info = "• Laaditaan tehostettua tukea varten, kun ilmenee, ettei yleinen tuki esiopetuksessa ole lapselle riittävää.\n• Tehdään tehostettua tukea varten lapsen perusopetukseen siirtymistä valmisteltaessa.\n• Laatimisesta vastaa esiopettaja."
                        ),
                        QuestionOption(
                            key = "learningPlanUsedForAssistance",
                            name = "Tätä oppimissuunnitelmaa käytetään tehostetun tuen toteuttamiseksi."
                        ),
                        QuestionOption(
                            key = "pedagogicalStatementMade",
                            name = "Pedagoginen selvitys on tehty erityisen tuen tarpeen arvioimiseksi.",
                            info = "• Laaditaan erityistä tukea varten lapselle, jolle tehostettu tuki ei riitä.\n• Laaditaan tarvittaessa esiopetusvuoden aikana lapsen perusopetukseen siirtymistä valmisteltaessa.\n• Laatimisesta vastaa esiopettaja.\nKs. erilliset ohjeet pedagogisen arvion ja selvityksen lomakkeista."
                        )
                    ),
                    minSelections = 0,
                    maxSelections = null,
                    textValue = emptyMap()
                ),
                VasuQuestion.TextQuestion(
                    name = "Lisätietoja tuen järjestämiseen liittyvistä asioista esiopetuksessa",
                    info = "Lisäksi tähän kuvataan tarvittavat\n• pedagogiset ratkaisut: oppimisympäristöihin liittyvät ratkaisut sekä lapsen tukeen liittyvät ratkaisut (joustavat ryhmittelyt, samanaikaisopetus, opetusmenetelmät, työskentely- ja kommunikointitavat tms.)\n• lapselle tarjottava osa-aikainen erityisopetus\n• esiopetukseen osallistumisen edellyttämät perusopetuslain mukaiset tulkitsemis- ja avustajapalvelut, muut opetuspalvelut ja erityiset apuvälineet, joista on tehty päätös.",
                    multiline = true,
                    value = ""
                ),
                VasuQuestion.MultiField(
                    name = when (lang) {
                        VasuLanguage.FI -> "Tuen muodot, jos lapsi on liittyvässä varhaiskasvatuksessa"
                        VasuLanguage.SV -> ""
                    },
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan esiopetukseen liittyvässä varhaiskasvatuksessa olevan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Leopsia hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu leopsissa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Leopsia päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi leopsiin kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen esiopetuksen ja liittyvän varhaiskasvatuksen järjestämisen näkökulmasta."
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
                    name = "Tavoitteet pedagogiselle toiminnalle",
                    info = "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita, oppimisvalmiuksia ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Henkilöstön toiminnalle asetetut tavoitteet voivat liittyä mm. lapsen oppimiseen, työskentely- ja vuorovaikutustaitoihin. Olennaista on kirjata tavoitteet lapsen kokonaiselle päivälle huomioiden mahdolliset lapsen tuen kannalta merkitykselliset asiat. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja opetuksen yhteisiä tavoitteita. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus.",
                    value = "",
                    multiline = true
                ),
                VasuQuestion.TextQuestion(
                    name = "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi",
                    info = "Tähän kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista.",
                    value = "",
                    multiline = true
                ),
                VasuQuestion.MultiSelectQuestion(
                    name = "Lapsen tuen toteuttamista koskeva hallintopäätös",
                    info = when (lang) {
                        VasuLanguage.FI -> "Tähän kohtaan kirjataan, jos lapsen tuesta esiopetuksessa ja/tai liittyvässä varhaiskasvatuksessa on annettu hallintopäätös. Leopsiin kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                        VasuLanguage.SV -> ""
                    },
                    options = listOf(
                        QuestionOption(
                            key = "general",
                            name = when (lang) {
                                VasuLanguage.FI -> "Tukipalvelut (yleinen tuki)"
                                VasuLanguage.SV -> ""
                            }
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
                    value = emptyList(),
                    minSelections = 0,
                    maxSelections = 2
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
                ),
                VasuQuestion.TextQuestion(
                    name = "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä/huoltajien kuuleminen hallintopäätöstä koskien",
                    info = "Tähän kohtaan kirjataan huoltajien kuuleminen, mikäli tuesta tehdään hallintopäätös.",
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
                            key = "tiedonsaajataho_tuleva_koulu",
                            name = "Tulevaan kouluun"
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
