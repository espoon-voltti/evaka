// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.vasu.CurriculumType.DAYCARE
import fi.espoo.evaka.vasu.CurriculumType.PRESCHOOL

enum class OphQuestionKey {
    PEDAGOGIC_ACTIVITY_GOALS,
    PEDAGOGIC_GOALS_DESCRIPTION
}

data class OphQuestion(
    val name: Map<OfficialLanguage, String>,
    val options: List<OphQuestionOption>
)

data class OphQuestionOption(val key: String, val name: Map<OfficialLanguage, String>)

fun getDefaultTemplateContent(type: CurriculumType, lang: OfficialLanguage) =
    when (type) {
        DAYCARE -> getDefaultVasuContent(lang)
        PRESCHOOL -> getDefaultLeopsContent(lang)
    }

fun getDefaultVasuContent(lang: OfficialLanguage) =
    VasuContent(
        hasDynamicFirstSection = true,
        sections =
            listOf(
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Perustiedot"
                            OfficialLanguage.SV -> ""
                        },
                    questions =
                        listOf(
                            VasuQuestion.StaticInfoSubSection(),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Yhteydenpitoon liittyviä lisätietoja"
                                        OfficialLanguage.SV -> "Ytterligare kontaktinformation"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Yhteydenpitoon liittyvät lisätiedot voivat esimerkiksi olla yhteishuoltajuuteen tai turvakieltoon liittyviä asioita."
                                        OfficialLanguage.SV ->
                                            "Ytterligare kontaktinformation kan gälla till exempel uppgifter om gemensam vårdnad eller säkerhetsförbud, t.ex. gällande spärrmarkering."
                                    },
                                value = "",
                                multiline = true
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Lapsen varhaiskasvatussuunnitelman laatiminen"
                            OfficialLanguage.SV ->
                                "Uppgörande av barnets plan för småbarnspedagogik"
                        },
                    questions =
                        listOf(
                            VasuQuestion.MultiField(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Laatimisesta vastaava henkilö"
                                        OfficialLanguage.SV -> "Person som ansvarar för uppgörandet"
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field("Etunimi"),
                                                Field("Sukunimi"),
                                                Field("Nimike"),
                                                Field("Puhelinnumero")
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field("Förnamn"),
                                                Field("Efternamn"),
                                                Field("Benämning"),
                                                Field("Telefonnummer")
                                            )
                                    },
                                value = listOf("", "", "", "")
                            ),
                            VasuQuestion.MultiFieldList(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Muu laatimiseen osallistunut henkilöstö/asiantuntijat"
                                        OfficialLanguage.SV ->
                                            "Övrig personal/sakkunniga som deltagit i uppgörandet"
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field("Etunimi"),
                                                Field("Sukunimi"),
                                                Field("Nimike"),
                                                Field("Puhelinnumero")
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field("Förnamn"),
                                                Field("Efternamn"),
                                                Field("Benämning"),
                                                Field("Telefonnummer")
                                            )
                                    },
                                value = listOf()
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Miten lapsen näkökulma ja mielipiteet otetaan huomioon"
                                        OfficialLanguage.SV ->
                                            "Hur har barnets perspektiv och åsikter beaktats och på vilket sätt är barnet delaktigt i uppgörandet av sin plan"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi osallistuu oman varhaiskasvatuksensa suunnitteluun ja arviointiin. Lapsen varhaiskasvatussuunnitelmaa tehtäessä keskustellaan lapsen vahvuuksista, kiinnostuksen kohteista, osaamisesta ja yksilöllisistä tarpeista. Lapsen toiveita, mielipiteitä ja odotuksia selvitetään erilaisin tavoin lapsen ikä- ja kehitystaso huomioiden."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs de typer av samtal och aktiviteter genom vilka barnet deltar i planeringen och utvärderingen av sin småbarnspedagogik. Barnets styrkor, intressen, kunskaper och individuella behov diskuteras när barnets plan för småbarnspedagogik görs upp. Barnets önskemål, åsikter och förväntningar utreds på olika sätt med hänsyn till barnets ålder och utvecklingsnivå. Använd gärna Svenska bildningstjänsters stödmaterial."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Miten huoltajien näkemykset otetaan huomioon ja yhteistyötä toteutetaan"
                                        OfficialLanguage.SV ->
                                            "Vårdnadshavares synpunkter, samt hur samarbetet genomförs"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kuvataan, miten huoltajien kanssa keskustellaan lapsen oppimisesta, kasvusta ja hyvinvoinnista toimintavuoden aikana. Huoltajien on mahdollista pohtia oman lapsensa varhaiskasvatukseen liittyviä toiveita ja odotuksia ennen vasu-keskustelua huoltajan oma osio -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista erilaisissa varhaiskasvatusyksikön tilaisuuksissa.\n" +
                                                "Tähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan.\n" +
                                                "Tarvittaessa mm. tulkkauspalveluihin liittyen lapsen pakolaistausta selvitetään osoitteesta pakolaiskorvaukset@espoo.fi , ks. erillinen ohje Tulkkauspalveluiden hankinta ja tilaaminen."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs hur barnets lärande, utveckling och välbefinnande diskuteras med vårdnadshavarna under verksamhetsåret. I sin egen del i barnets plan har vårdnadshavarna möjlighet att reflektera över sina önskemål och förväntningar på barnets småbarnspedagogik före samtalet om barnets plan för småbarnspedagogik.\n\n" +
                                                "Här kan antecknas önskemål och överenskommelser gällande familjens språkliga, kulturella eller åskådningsmässiga bakgrund, såsom hemspråk, användning av tolkningstjänster eller hur frågor kring livsåskådning ska diskuteras. Vid behov, till exempel i anslutning till tolkningstjänster, utreds barnets flyktingstatus på adressen pakolaiskorvaukset@espoo.fi. Se anvisningen om beställning av tolktjänster."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Monialainen yhteistyö"
                            OfficialLanguage.SV -> "Sektorsövergripande samarbete"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Yhteistyökumppanit ja yhteystiedot"
                                        OfficialLanguage.SV ->
                                            "Samarbetspartners och kontaktuppgifter"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi lastenneuvolan tai lastensuojelun kanssa. Lisäksi kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot."
                                        OfficialLanguage.SV ->
                                            "Här kan antecknas genomförandet av sektorsövergripande samarbete med till exempel barnrådgivningen eller barnskyddet. Här antecknas även organisationerna, namnen och kontaktuppgifterna till aktörerna inom det sektorsövergripande arbetet."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Sovitut yhteistyötavat, vastuut ja palvelut"
                                        OfficialLanguage.SV ->
                                            "Överenskomna samarbetsformer, ansvarsområden och tjänster"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kirjataan yhteisesti sovitut asiat. Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan\n\n• yhteistyö lapsen ja huoltajan kanssa\n• lapsen tuen toteuttamisen vastuut \n• erityisasiantuntijoiden palvelujen käyttö \n• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio\n• mahdollisten kuljetusten järjestelyt ja vastuut."
                                        OfficialLanguage.SV ->
                                            "Här antecknas gemensamma överenskommelser. Gällande det samarbete och de tjänster som det eventuella stödet kräver, beaktas\n\n• samarbetet med barnet och vårdnadshavaren\n• ansvarsfördelningen gällande genomförandet av barnets stöd\n• användningen av specialisttjänster\n• vägledningen av och konsultationer med sakkunniga inom social- och hälsovården samt övriga sakkunniga\n• ansvar för anordnandet av eventuella transporter."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Lapsen varhaiskasvatussuunnitelman tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                            OfficialLanguage.SV ->
                                "Utvärdering av hur målen uppnåtts och åtgärderna genomförts i barnets tidigare plan för småbarnspedagogik"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tavoitteiden ja toimenpiteiden toteutuminen"
                                        OfficialLanguage.SV ->
                                            "Beskrivning av hur de tidigare målen har uppnåtts och åtgärderna har genomförts"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Mitkä toiminnalle asetetut tavoitteet ja toimenpiteet ovat toteutuneet? Miten ne ovat toteutuneet? Mikä on edistänyt/estänyt tavoitteiden ja toimenpiteiden toteutumista? Arviointi kohdistuu toiminnan, järjestelyjen, oppimisympäristöjen ja pedagogiikan arviointiin, ei lapsen arviointiin. Arvioinnin yhteydessä henkilöstö sekä huoltaja ja lapsi pohtivat kuinka hyvin lapsen vasuun kirjatut kasvatukselle, opetukselle ja hoidolle asetetut tavoitteet ovat toteutuneet ja ovatko toimenpiteet olleet tarkoituksenmukaisia."
                                        OfficialLanguage.SV ->
                                            "Vilka av de mål och åtgärder som fastställts för verksamheten har uppnåtts och genomförts? Hur har de uppnåtts och genomförts? Vad har bidragit till/förhindrat att målen och åtgärderna uppnåtts och genomförts? Utvärderingen fokuserar på att utvärdera verksamheten, åtgärderna, lärmiljöerna och pedagogiken, inte på en bedömning av barnet. Vid utvärderingen reflekterar personalen, vårdnadshavaren och barnet över hur väl de mål som satts upp för barnets fostran, undervisning och vård har uppnåtts och om åtgärderna har varit ändamålsenliga."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Lapsen tuen arviointi"
                                        OfficialLanguage.SV ->
                                            "Utvärdering av tidigare stödåtgärder"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Ovatko lapselle annettu tuki ja tukitoimenpiteet olleet toimivia ja riittäviä? Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Miten sovitut yhteistyökäytännöt ovat toteutuneet? Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä lapsen vasua päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea lapsen vasua päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                                        OfficialLanguage.SV ->
                                            "Utvärderingen av stödet omfattar en beskrivning av stödåtgärderna och en utvärdering av hur väl de fungerat. Har barnets stöd och stödåtgärderna som getts varit effektiva och tillräckliga? Hur har de överenskomna pedagogiska, strukturella och/eller vårdinriktade stödformerna genomförts och vilka effekter har de haft? Vilka stödåtgärder gynnar barnet i enlighet med dess individuella stödbehov, och varför?Stödbehovet samt stödets tillräcklighet, ändamålsenlighet och effekt ska utvärderas och följas upp, och barnets plan för småbarnspedagogik ska uppdateras alltid när stödbehovet ändras.  Om barnet får intensifierat eller särskilt stöd eller stödtjänster som en del av det allmänna stödet uppdateras barnets plan för småbarnspedagogik enligt innehållet i förvaltningsbeslutet. Detta fält fylls i om barnet har fått stöd."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta"
                                        OfficialLanguage.SV ->
                                            "Övriga kommentarer om barnets tidigare plan för småbarnspedagogik"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen vasu tulee arvioida ja tarkistaa vähintään kerran vuodessa tai useammin jos lapsen tarpeet sitä edellyttävät. Lapsen vasua arvioitaessa arviointi kohdistuu pedagogiikan toteutumiseen, oppimisympäristöihin ja toiminnan järjestelyihin sekä mahdolliseen tuen vaikuttavuuteen ja tukitoimien toteutumiseen. Lapsen vasun tarkistaminen perustuu lapsen vasun arviointiin yhdessä lapsen ja huoltajan kanssa. Tarkoituksena on varmistaa, että lapsen vasusta muodostuu jatkumo. Tässä osiossa tarkastellaan aiemmin laadittua lapsen vasua ja arvioidaan siihen kirjattujen tavoitteiden toteutumista. Mikäli lapsen vasua ollaan laatimassa ensimmäistä kertaa, tätä arviointia ei luonnollisesti tehdä. Lapsen vasun tavoitteita sekä niiden toteuttamista seurataan ja arvioidaan säännöllisesti."
                                        OfficialLanguage.SV ->
                                            "Barnets plan för småbarnspedagogik ska utvärderas och granskas minst en gång om året eller oftare om barnets behov kräver det. Utvärderingen av barnets plan för småbarnspedagogik fokuserar på genomförandet av pedagogiken, lärmiljöerna och åtgärderna för verksamheten samt det eventuella stödets effekt och genomförandet av stödåtgärderna. Barnets plan för småbarnspedagogik granskas utgående från den utvärdering av barnets plan som görs tillsammans med barnet och vårdnadshavare. Detta görs för att skapa kontinuitet i barnets småbarnspedagogik. Vid denna punkt granskas barnets tidigare plan för småbarnspedagogik och det görs en utvärdering av hur väl målen i planen uppnåtts. Om barnets plan för småbarnspedagogik utarbetas för första gången görs inte denna utvärdering. Målen i barnets plan, samt hur väl de uppnåtts, ska följas upp och utvärderas regelbundet."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
                            OfficialLanguage.SV ->
                                "Mål för den pedagogiska verksamheten och åtgärder för att uppnå målen"
                        },
                    questions =
                        listOfNotNull(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                                        OfficialLanguage.SV ->
                                            "Barnets styrkor, intressen och behov samt hur dessa beaktas i verksamheten"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kuvataan lapsen keskeiset vahvuudet ja kiinnostuksen kohteet sekä tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs barnets styrkor, intressen och behov som grund för målsättningen och planeringen av verksamheten.\n\n" +
                                                "Ex. Nelle är fysiskt aktiv och har ett stort rörelsebehov. Hen deltar aktivt i rörelsestunder och utevistelse och är skicklig på att klättra och springa. Vi tar Nelles rörelseglädje i beaktande då vi planerar verksamheten. Samlingarna förverkligar vi t.ex. på ett sådant sätt att rörelse är en naturlig del av dem och då gruppen förflyttar sig gör vi det på ett lekfullt sätt genom att t.ex. hoppa som grodor eller smyga som ninjor. "
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Kieleen ja kulttuuriin liittyviä tarkentavia näkökulmia"
                                        OfficialLanguage.SV ->
                                            "Mål och metoder för hur barnets kulturella- och språkliga identitet/identiteter bekräftas samt hur barnets språkutveckling stöds"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kohdassa kirjataan, miten edistetään monipuolisesti vieraskielisten ja monikielisten lasten kielitaidon sekä kieli- ja kulttuuri-identiteettien ja itsetunnon kehittymistä. Huoltajien kanssa keskustellaan myös lapsen oman äidinkielen/äidinkielien tukemisesta."
                                        OfficialLanguage.SV ->
                                            "Här antecknas hur man bidrar till en mångsidig utveckling av språkkunskaperna, den språkliga och kulturella identiteten och självförtroendet hos fler– och tvåspråkiga barn, eller barn med ett främmande språk som modersmål. I den svenskspråkiga småbarnspedagogiken i Esbo är det vanligt med barn som hemma talar både finska och svenska. I detta fält antecknas hur man i verksamheten målmedvetet stöder och följer upp barnets språkutveckling i verksamhetsspråket, samtidigt som man bekräftar barnets båda språkliga identiteter. Använd barnets språkbarometer som stöd för en helhetsbild av barnets språkliga miljö."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.MultiField(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Mahdolliset lapsen kehityksen, oppimisen ja hyvinvoinnin tukeen liittyvät tarpeet sekä lapsen tuen toteuttamiseen liittyvät tuen muodot (pedagogiset, rakenteelliset ja hoidolliset)"
                                        OfficialLanguage.SV ->
                                            "Barnets eventuella behov av stöd för utveckling, lärande och välbefinnande samt stödformerna för genomförandet av barnets stöd (pedagogiska, strukturella och vårdinriktade)"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Lapsen vasua hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu lapsen vasussa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Lapsen vasua päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi lapsen vasuun kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen varhaiskasvatuksen järjestämisen näkökulmasta."
                                        OfficialLanguage.SV ->
                                            "Detta fält fylls i om barnet är i behov av stöd. Här antecknas barnets eventuella behov av stöd samt de pedagogiska, strukturella och vårdinriktade stödformerna i anslutning till genomförandet av barnets stöd. Även barnets eventuella stödtjänster antecknas här. Barnets plan för småbarnspedagogik utnyttjas när man fattar förvaltningsbeslut om intensifierat eller särskilt stöd eller om stödtjänster inom allmänt stöd. Om barnets stödbehov har bedömts i barnets plan för småbarnspedagogik, ska bedömningen beaktas när förvaltningsbeslut om intensifierat eller särskilt stöd och beslut om stödtjänsterna inom allmänt stöd fattas. Barnets plan för småbarnspedagogik ska uppdateras i enlighet med innehållet i förvaltningsbeslutet. Dessutom antecknas i barnets plan för småbarnspedagogik eventuella social- och hälsovårdstjänster, såsom barnets rehabilitering, om de är väsentliga för ordnandet av barnets småbarnspedagogik."
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field(
                                                    name = "Pedagogiset tuen muodot",
                                                    info =
                                                        "• varhaiskasvatuspäivän rakenteeseen ja päivärytmiin liittyvät ratkaisut \n" +
                                                            "• oppimisympäristöihin liittyvät ratkaisut \n" +
                                                            "• tarvittavat erityispedagogiset menetelmät \n" +
                                                            "• vuorovaikutus- ja kommunikointitavat, esimerkiksi viittomien ja kuvien käyttö \n" +
                                                            "• käytännöt, miten lapsi pääsee osalliseksi vertaisryhmän toimintaa, esimerkiksi esteettömyyden huomiointi."
                                                ),
                                                Field(
                                                    name = "Rakenteelliset tuen muodot",
                                                    info =
                                                        "• tuen toteuttamiseen liittyvän osaamisen ja erityispedagogisen osaamisen vahvistaminen \n" +
                                                            "• henkilöstön mitoitukseen ja rakenteeseen liittyvät ratkaisut \n" +
                                                            "• lapsiryhmän kokoon ja ryhmärakenteeseen liittyvät ratkaisut \n" +
                                                            "• tulkitsemis- ja avustamispalvelut sekä apuvälineiden käyttö \n" +
                                                            "• pien- tai erityisryhmä tai muu tarvittava ryhmämuoto \n" +
                                                            "• varhaiskasvatuksen erityisopettajan osa- tai kokoaikainen opetus tai konsultaatio."
                                                ),
                                                Field(
                                                    name = "Hoidolliset tuen muodot",
                                                    info =
                                                        "• perushoitoon, hoivaan ja avustamiseen liittyvät menetelmät \n" +
                                                            "• terveydenhoidolliset tarpeet, esimerkiksi lapsen pitkäaikaissairauksien hoitoon, lääkitykseen, ruokavalioon ja liikkumiseen liittyvä avustaminen ja apuvälineet.\n"
                                                )
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field(
                                                    name = "Pedagogiska stödformer",
                                                    info =
                                                        "• lösningar gällande dagliga rutiner och struktur inom småbarnspedagogiken\n" +
                                                            "• lösningar gällande lärmiljöer\n" +
                                                            "• specialpedagogiska metoder\n" +
                                                            "• metoder för växelverkan och kommunikation, till exempel användning av tecken och bilder\n" +
                                                            "• verksamhetssätt för hur barnet kan vara delaktigt i barngruppens verksamhet, till exempel beaktande av tillgänglighet (t.ex. delaktighet i leken/sociala gemenskapen, fysisk tillgänglighet, möjlighet att uttrycka sig på ett för barnet tillgängligt sätt etc.)"
                                                ),
                                                Field(
                                                    name = "Strukturella stödformer",
                                                    info =
                                                        "• att stärka kunnandet och den specialpedagogiska kompetensen som berör genomförande av stödet\n" +
                                                            "• lösningar angående personaldimensionering och personalstruktur\n" +
                                                            "• lösningar som rör barngruppens storlek och gruppens sammansättning\n" +
                                                            "• tolknings- och assistenttjänster samt användning av hjälpmedel\n" +
                                                            "• smågrupp, specialgrupp eller annan gruppform enligt behoven\n" +
                                                            "• konsultation eller undervisning som ges på hel- eller deltid av en speciallärare inom småbarnspedagogik"
                                                ),
                                                Field(
                                                    name = "Vårdinriktade stödformer",
                                                    info =
                                                        "• metoder som berör grundläggande vård, omsorg och assistans\n" +
                                                            "• verksamhet som tillgodoser behov av hälso- och sjukvård, till exempel hjälpmedel och assistans som hänför sig till barnets långtidssjukdomar, medicinering, kost och rörlighet\n"
                                                )
                                            )
                                    },
                                value = listOf("", "", ""),
                                separateRows = true
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tavoitteet henkilöstön pedagogiselle toiminnalle"
                                        OfficialLanguage.SV ->
                                            "Mål för personalens pedagogiska verksamhet"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Olennaista on kirjata tavoitteet lapsen kasvatukselle, opetukselle ja hoidolle. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja oppimisen alueita. Lisäksi tähän kirjataan mahdolliset kehityksen, oppimisen ja hyvinvoinnin tuen kannalta merkitykselliset tavoitteet. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus."
                                        OfficialLanguage.SV ->
                                            "Här anges centrala mål för personalens pedagogiska verksamhet. Målen ska beskriva hur man med den pedagogiska verksamheten och lärmiljöerna stöder barnets utveckling, lärande och välbefinnande. Barnets styrkor, intressen, färdigheter och behov ska beaktas då målen anges. Här beaktas även delområden inom mångsidig kompetens och lärområden. Det är viktigt att de mål som ställs upp utgår från barngruppen och dess situation, samt att de är konkreta och möjliga att utvärdera.\n\n" +
                                                "I barnets plan ska flera mål som beaktar barnets färdigheter och hur de kan främjas i verksamheten anges. Nedan ges ett exempel på ett mål:\n\n" +
                                                "Nelle har ett stort ordförråd och mycket att berätta - så mycket att hen inte alltid hinner lyssna. Vi strävar till att stärka Nelles förmåga till växelverkan och turtagning men ger även Nelle möjligheter att få utnyttja sin verbala förmåga på olika sätt."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                                        OfficialLanguage.SV ->
                                            "Åtgärder och metoder för att uppnå målen för den pedagogiska verksamheten"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista."
                                        OfficialLanguage.SV ->
                                            "Här antecknas de konkreta pedagogiska åtgärder och metoder som syftar till att uppnå de mål som angetts för den pedagogiska verksamheten. Metoderna ska beskrivas på en så konkret nivå att förverkligandet av dem kan utvärderas.\n\n" +
                                                "Nedan ges exempel på metoder för att uppnå det mål som angavs för den pedagogiska verksamheten i punkt 5.4.\n\n" +
                                                "1. Vi använder oss av små grupper för att stärka varje barns möjlighet till delaktighet och att bli hörda. Vi använder oss även av ett taltursdjur under samlingen som signalerar att det barn som har djuret pratar.\n" +
                                                "2. För att ge Nelle möjlighet att använda och utveckla sin verbala förmåga arbetar hen med en digital bok på pekplattan. Hen kan själv fotografera, rita och spela in ljud samt berätta sagor eller om något hen gjort. Vi hjälper Nelle att leta reda på bokstäver på tangentbordet för att skriva rubriker eller korta meningar."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.MultiSelectQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Lapsen tuen taso jatkossa"
                                        OfficialLanguage.SV -> "Barnets stödnivå i fortsättningen"
                                    },
                                options =
                                    listOf(
                                        QuestionOption(
                                            key = "general",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Yleinen tuki"
                                                    OfficialLanguage.SV -> "Allmänt stöd"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Lapsen tuen toteuttamista koskeva hallintopäätös"
                                                    OfficialLanguage.SV ->
                                                        "Förvaltningsbeslut om barnets stöd"
                                                },
                                            isIntervention = true,
                                            info =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Tämä kohta kirjataan, jos lapsen tuesta on annettu hallintopäätös. Lapsen vasuun kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                                                    OfficialLanguage.SV ->
                                                        "Här antecknas om det fattats ett förvaltningsbeslut om barnets stöd. Om förvaltningsbeslutet hävs, ska datumet för detta även antecknas i barnets plan för småbarnspedagogik. I fältet Övrigt att beakta kan man anteckna preciserande synpunkter gällande förvaltningsbeslutet."
                                                }
                                        ),
                                        QuestionOption(
                                            key = "during_range",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Tukipalvelut ajalla"
                                                    OfficialLanguage.SV -> "Stödtjänster för tiden"
                                                },
                                            dateRange = true
                                        ),
                                        QuestionOption(
                                            key = "intensified",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Tehostettu tuki"
                                                    OfficialLanguage.SV -> "Intensifierat stöd"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "special",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Erityinen tuki"
                                                    OfficialLanguage.SV -> "Särskilt stöd"
                                                }
                                        )
                                    ),
                                minSelections = 0,
                                maxSelections = null,
                                value = listOf(),
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muita huomioita"
                                        OfficialLanguage.SV -> "Övrigt att beakta"
                                    },
                                multiline = true,
                                value = ""
                            ),
                            when (lang) {
                                OfficialLanguage.FI ->
                                    VasuQuestion.Followup(
                                        title =
                                            "Tarkennuksia toimintavuoden aikana lapsen tarpeiden mukaan",
                                        name = "Päivämäärä ja kirjaus",
                                        info =
                                            "Tämän kohdan tarkoituksena on varmistaa lapsen vasuun kirjattujen tavoitteiden ja toimenpiteiden toteutumisen jatkuva arviointi. Jatkuvalla arvioinnilla tarkoitetaan havainnoinnin ja pedagogisen dokumentoinnin avulla tarkennettavia tavoitteita ja toimenpiteitä. Näistä keskustellaan huoltajien kanssa päivittäisissä kohtaamisissa. Jatkuvan arvioinnin avulla lapsen vasu pysyy ajan tasalla.",
                                        value = emptyList(),
                                        continuesNumbering = true
                                    )
                                OfficialLanguage.SV -> null
                            }
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Muut mahdolliset lapsen varhaiskasvatuksessa huomioitavat asiat"
                            OfficialLanguage.SV -> "Övrigt att beakta i barnets småbarnspedagogik"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muita huomioita"
                                        OfficialLanguage.SV -> "Övrigt att beakta"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän osioon kirjataan muita huomioitavia asioita, kuten esimerkiksi lepoon, ruokailuun tai pukemiseen liittyvät asiat.\n" +
                                                "Keskustele tarvittaessa huoltajien ajatuksista tyttöjen ympärileikkauksesta, ks. erillinen ohje Tyttöjen sukuelinten silpomisen estäminen. Tähän kirjataan huoltajien ajatukset asiasta. Jos huoli herää, toimi em. ohjeistuksen mukaan."
                                        OfficialLanguage.SV ->
                                            "Här antecknas övriga saker som ska beaktas, till exempel gällande vila, måltider eller påklädning."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                            OfficialLanguage.SV ->
                                "Eventuella övriga dokument och planer som använts vid uppgörandet av barnets plan för småbarnspedagogik"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                                        OfficialLanguage.SV -> "Övriga dokument och planer"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen vasun laatimisessa voidaan hyödyntää muita mahdollisia suunnitelmia, esimerkiksi lääkehoitosuunnitelmaa."
                                        OfficialLanguage.SV ->
                                            "Vid uppgörandet av barnets plan för småbarnspedagogik kan eventuella andra planer och dokument, såsom planen för läkemedelsbehandling, Hyve4-blanketter, språkbarometern m.m. som utgör bilagor till barnets plan för småbarnspedagogik, utnyttjas."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Tiedonsaajatahot"
                            OfficialLanguage.SV -> "Överförande av barnets plan till berörda parter"
                        },
                    questions =
                        listOf(
                            VasuQuestion.MultiSelectQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tämä varhaiskasvatussuunnitelma luovutetaan huoltajan/huoltajien luvalla"
                                        OfficialLanguage.SV ->
                                            "Barnets plan kan med vårdnadshavares tillstånd överföras till"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen vaihtaessa toiseen kaupungin varhaiskasvatusyksikköön, lapsen varhaiskasvatussuunnitelma ja muut varhaiskasvatuksen asiakirjat siirtyvät uuteen yksikköön (koskee myös ostopalveluna järjestettävää varhaiskasvatusta). Varhaiskasvatussuunnitelman luovuttamiseen ulkopuoliselle taholle pyydämme huoltajilta luvan. Lapsen siirtyessä yksityisen palveluntuottajan tai toisen kunnan järjestämään varhaiskasvatukseen, on varhaiskasvatussuunnitelma kuitenkin toimitettava uudelle varhaiskasvatuksen järjestäjälle myös ilman huoltajan lupaa, mikäli tiedot ovat välttämättömiä lapsen varhaiskasvatuksen järjestämiseksi (Varhaiskasvatuslaki 41 §:n 3 mom.). Lisäksi varhaiskasvatussuunnitelma on toimitettava esi- tai perusopetuksen järjestäjälle, jos se on välttämätöntä lapsen opetuksen järjestämiseksi (Varhaiskasvatuslaki 40 §:n 3 mom., perusopetuslaki 41 §:n 3 mom.). Luovuttamisesta informoidaan huoltajaa etukäteen."
                                        OfficialLanguage.SV ->
                                            "Då barnets plats inom småbarnspedagogik byts till en annan av stads enheter för småbarnspedagogik överförs barnets plan och andra handlingar för småbarnspedagogik till den nya enheten (gäller även stads köpavtalsenheter). Om barnets plan lämnas över till en utomstående aktör bes vårdnadshavarnas samtycke. Om ett barn övergår till småbarnspedagogik i annan kommun eller till privat anordnare ska barnets plan lämnas över till den nya anordnaren av småbarnspedagogik, också utan samtycke av vårdnadshavare, ifall uppgifterna är nödvändiga för att ordna småbarnspedagogik för barnet (lag om småbarnspedagogik 40 § och 41 §). Barnets plan ska även lämnas över till anordnare av förskoleundervisning och grundläggande utbildning om uppgifterna är nödvändiga för att anordna undervisningen för barnet (lag om småbarnspedagogik 41 §, lag om grundläggande utbildning 41 §)  Vårdnadshavare informeras på förhand om överföringen."
                                    },
                                options =
                                    listOf(
                                        QuestionOption(
                                            key = "Tulevaan esiopetusryhmään",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Tulevaan esiopetusryhmään"
                                                    OfficialLanguage.SV ->
                                                        "den blivande förskolegruppen"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "Neuvolaan",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Neuvolaan"
                                                    OfficialLanguage.SV -> "barnrådgivningen"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "Lasten terapiapalveluihin",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Lasten terapiapalveluihin"
                                                    OfficialLanguage.SV ->
                                                        "terapitjänsterna för barn"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "Erikoissairaanhoitoon",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Erikoissairaanhoitoon"
                                                    OfficialLanguage.SV -> "specialsjukvården"
                                                }
                                        )
                                    ),
                                minSelections = 0,
                                maxSelections = null,
                                value = emptyList(),
                                textValue = emptyMap()
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muualle, minne?"
                                        OfficialLanguage.SV -> "Övriga mottagare, vilka?"
                                    },
                                multiline = false,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Lapsen varhaiskasvatussuunnitelmakeskustelu"
                            OfficialLanguage.SV -> "Samtal om barnets plan för småbarnspedagogik"
                        },
                    questions =
                        listOfNotNull(
                            VasuQuestion.Paragraph(
                                title =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Varhaiskasvatussuunnitelma on käyty läpi yhteistyössä huoltajien kanssa"
                                        OfficialLanguage.SV ->
                                            "Barnets plan för småbarnspedagogik har gjorts upp i samarbete med vårdnadshavare."
                                    },
                                paragraph = ""
                            ),
                            VasuQuestion.DateQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Varhaiskasvatuskeskustelun päivämäärä"
                                        OfficialLanguage.SV -> "Datum för samtalet om barnets plan"
                                    },
                                trackedInEvents = true,
                                nameInEvents =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Varhaiskasvatussuunnitelmakeskustelu"
                                        OfficialLanguage.SV -> "Samtalet om barnets plan"
                                    },
                                value = null
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Keskusteluun osallistuneet huoltajat"
                                        OfficialLanguage.SV ->
                                            "Vårdnadshavare som deltog i samtalet"
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä/huoltajien kuuleminen hallintopäätöstä koskien"
                                        OfficialLanguage.SV ->
                                            "Vårdnadshavares tankar kring innehållet i barnets plan/hörande av vårdnadshavare gällande förvaltningsbeslut"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan huoltajien kuuleminen, mikäli tuesta tehdään hallintopäätös."
                                        OfficialLanguage.SV -> ""
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Seuranta- ja arviointiajankohdat"
                            OfficialLanguage.SV ->
                                "Tidpunkterna för uppföljning och utvärdering av barnets plan för småbarnspedagogik"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Ajankohdat ja kuvaus"
                                        OfficialLanguage.SV -> "Tidpunkter och beskrivning"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan huoltajan kanssa yhdessä sovittu jatkosuunnitelma ja milloin suunnitelmaa seuraavan kerran arvioidaan."
                                        OfficialLanguage.SV ->
                                            "Här antecknas hur man tillsammans med vårdnadshavarna går vidare med planen, samt datum för när planen ska utvärderas nästa gång."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                )
            )
    )

fun getDefaultLeopsContent(lang: OfficialLanguage) =
    VasuContent(
        hasDynamicFirstSection = true,
        sections =
            listOf(
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Perustiedot"
                            OfficialLanguage.SV -> "Basuppgifter"
                        },
                    questions =
                        listOf(
                            VasuQuestion.StaticInfoSubSection(),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Yhteydenpitoon liittyviä lisätietoja"
                                        OfficialLanguage.SV -> "Ytterligare kontaktinformation"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Yhteydenpitoon liittyvät lisätiedot voivat esimerkiksi olla yhteishuoltajuuteen tai turvakieltoon liittyviä asioita."
                                        OfficialLanguage.SV ->
                                            "Ytterligare kontaktinformation kan gälla till exempel uppgifter om gemensam vårdnad eller säkerhetsförbud, t.ex. gällande spärrmarkering."
                                    },
                                value = "",
                                multiline = true
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Lapsen esiopetuksen oppimissuunnitelman laatiminen"
                            OfficialLanguage.SV ->
                                "Uppgörandet av plan för barnets lärande i förskolan"
                        },
                    questions =
                        listOf(
                            VasuQuestion.MultiField(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Laatimisesta vastaava henkilö"
                                        OfficialLanguage.SV ->
                                            "Person som ansvarar för uppgörandet av planen"
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field("Etunimi"),
                                                Field("Sukunimi"),
                                                Field("Nimike"),
                                                Field("Puhelinnumero")
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field("Förnamn"),
                                                Field("Efternamn"),
                                                Field("Benämning"),
                                                Field("Telefonnummer")
                                            )
                                    },
                                value = listOf("", "", "", "")
                            ),
                            VasuQuestion.MultiFieldList(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Muu laatimiseen osallistunut henkilöstö/asiantuntijat"
                                        OfficialLanguage.SV ->
                                            "Övrig personal/övriga sakkunniga som deltagit i uppgörandet av planen"
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field("Etunimi"),
                                                Field("Sukunimi"),
                                                Field("Nimike"),
                                                Field("Puhelinnumero")
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field("Förnamn"),
                                                Field("Efternamn"),
                                                Field("Benämning"),
                                                Field("Telefonnummer")
                                            )
                                    },
                                value = listOf(listOf("", "", "", ""))
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Miten lapsen näkökulma ja mielipiteet otetaan huomioon ja lapsi on osallisena prosessissa"
                                        OfficialLanguage.SV ->
                                            "Hur har barnets perspektiv och åsikter beaktats och på vilket sätt är barnet delaktigt i uppgörandet av sin plan"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kuvataan, millaisten keskustelujen ja toiminnan kautta lapsi voi osallistua oman esiopetusvuotensa sekä omien oppimistavoitteidensa suunnitteluun ja arviointiin. Lapsen esiopetuksen oppimissuunnitelmaa (leops) tehtäessä keskustellaan lapsen vahvuuksista, kiinnostuksen kohteista, osaamisesta, yksilöllisistä tarpeista ja oppimisesta. Lapsen toiveita, mielipiteitä ja odotuksia selvitetään erilaisin tavoin."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs de typer av samtal och aktiviteter genom vilka barnet deltar i planeringen och utvärderingen av sin förskoleundervisning. Barnets styrkor, intressen, kunskaper och individuella behov diskuteras när plan för barnets lärande i förskolan görs upp. Barnets önskemål, åsikter och förväntningar utreds på olika sätt med hänsyn till barnets ålder och utvecklingsnivå."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Miten huoltajien näkemykset otetaan huomioon ja yhteistyötä toteutetaan"
                                        OfficialLanguage.SV ->
                                            "Vårdnadshavarnas synpunkter samt hur samarbetet ordnas"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä tarkoituksena on varmistaa, että huoltajilla on mahdollisuus osallistua suunnitelman laadintaan ja arviointiin. Huoltajien kanssa keskustellaan lapsen esiopetusvuodesta, oppimisesta, kasvusta ja hyvinvoinnista suunnitelmallisissa keskustelutilanteissa lukuvuoden aikana sekä päivittäisissä arjen kohtaamisissa. Huoltajien on mahdollista pohtia oman lapsensa esiopetukseen liittyviä toiveita ja odotuksia etukäteen huoltajan oma osio -lomakkeen avulla. Lisäksi huoltajalla on mahdollisuus keskustella muiden huoltajien kanssa lasten oppimiseen, kasvuun ja hyvinvointiin liittyvistä asioista yhteisissä tilaisuuksissa.\n" +
                                                "Tähän kohtaan voidaan kirjata perheen kielelliseen, kulttuuriseen tai katsomukselliseen taustaan liittyvät toiveet ja yhdessä sovitut asiat, kuten esimerkiksi kotikielet, tulkkauspalveluiden käyttö tai miten katsomuksellisista asioista keskustellaan.\n" +
                                                "Tarvittaessa mm. tulkkauspalveluihin liittyen lapsen pakolaistausta selvitetään osoitteesta pakolaiskorvaukset@espoo.fi , ks. erillinen ohje Tulkkauspalveluiden hankinta ja tilaaminen."
                                        OfficialLanguage.SV ->
                                            "Här är syftet att garantera att vårdnadshavarna får möjlighet att delta i uppgörandet och utvärderingen av plan för barnets lärande i förskolan. Barnets förskoleår, lärande, utveckling och välbefinnande diskuteras med vårdnadshavarna under såväl strukturerade samtal under verksamhetsåret samt vid daglig kontakt. I sin egen del i plan för barnets lärande ges vårdnadshavarna möjlighet att uttrycka önskemål och förväntningar om barnets förskoleundervisning.\n\n" +
                                                "Här kan även antecknas önskemål och överenskommelser gällande familjens språkliga, kulturella eller åskådningsmässiga bakgrund, såsom hemspråk, användning av tolkningstjänster eller hur frågor kring livsåskådning ska diskuteras."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Monialainen yhteistyö"
                            OfficialLanguage.SV -> "Sektorsövergripande samarbete"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Yhteistyökumppanit ja yhteystiedot"
                                        OfficialLanguage.SV ->
                                            "Samarbetspartners och kontaktuppgifter"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan voidaan kirjata monialaisen yhteistyön toteuttaminen, esimerkiksi opiskeluhuollon, lastenneuvolan, koulujen ja lastensuojelun kanssa. Tähän kirjataan monialaisten toimijoiden organisaatiot, nimet ja yhteystiedot. Lisäksi kirjataan yhteisesti sovitut asiat ja mahdollinen vastuunjako."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs genomförandet av sektorsövergripande samarbete med till exempel barnrådgivningen, barnskyddet eller elevvården. Här antecknas även organisationerna, namnen och kontaktuppgifterna till aktörerna inom det sektorsövergripande arbetet."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Sovitut yhteistyötavat, vastuut ja palvelut"
                                        OfficialLanguage.SV ->
                                            "Överenskomna samarbetsformer, ansvarsområden och tjänster"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Mahdollisen tuen edellyttämän yhteistyön ja palvelujen näkökulmasta huomioidaan\n\n• yhteistyö lapsen ja huoltajan kanssa\n• lapsen tuen toteuttamisen vastuut \n• erityisasiantuntijoiden palvelujen käyttö \n• sosiaali- ja terveydenhuollon sekä muiden tarvittavien asiantuntijoiden antama ohjaus ja konsultaatio\n• mahdollisten kuljetusten järjestelyt ja vastuut\n• perusopetuksen järjestämässä esiopetuksessa lapsen mahdollinen osallistuminen liittyvään varhaiskasvatukseen ja kuvaus yhteistyöstä toimijoiden kesken."
                                        OfficialLanguage.SV ->
                                            "Här antecknas gemensamma överenskommelser. Gällande det samarbete och de tjänster som det eventuella stödet kräver, beaktas\n\n• samarbetet med barnet och vårdnadshavaren\n• ansvarsfördelningen gällande genomförandet av barnets stöd\n• användningen av specialisttjänster\n• vägledningen av och konsultationer med sakkunniga inom social- och hälsovården samt övriga sakkunniga\n• ansvar för anordnandet av eventuella transporter\n• samarbetet med anordnaren av kompletterande småbarnspedagogik om barnet deltar i denna."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Lapsen esiopetuksen oppimissuunnitelman tavoitteiden ja toimenpiteiden toteutumisen arviointi"
                            OfficialLanguage.SV ->
                                "Utvärdering av hur målen uppnåtts och åtgärderna genomförts i plan för barnets lärande i förskolan"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tavoitteiden ja toimenpiteiden toteutuminen"
                                        OfficialLanguage.SV ->
                                            "Beskrivning av hur de tidigare målen har uppnåtts och åtgärderna har genomförts"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Mitkä toiminnalle asetetut tavoitteet ja toimenpiteet ovat toteutuneet? Miten ne ovat toteutuneet? Mikä on edistänyt/estänyt tavoitteiden ja toimenpiteiden toteutumista? Arviointi kohdistuu toiminnan, järjestelyjen, oppimisympäristöjen ja pedagogiikan arviointiin. Arvioinnin avulla seurataan lapsen oppimista, kasvua ja hyvinvointia ja kuinka pedagogiselle toiminnalle asetetut tavoitteet ovat niitä tukeneet."
                                        OfficialLanguage.SV ->
                                            "Vilka av de mål som angetts för verksamheten har uppnåtts? Hur har de genomförts? Vad har bidragit till/förhindrat att målen och åtgärderna uppnåtts och genomförts? Utvärderingen fokuserar på att utvärdera verksamheten, metoderna, åtgärderna, lärmiljöerna och pedagogiken, inte på en bedömning av barnet. Med hjälp av utvärderingen är det möjligt att följa upp barnets lärande, utveckling och välbefinnande samt hur väl målen som ställts upp för verksamheten stöder dessa."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Lapsen tuen arviointi"
                                        OfficialLanguage.SV -> "Utvärdering av stöd"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Ovatko lapselle annettu tuki ja tukitoimenpiteet olleet toimivia ja riittäviä? Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Miten sovitut yhteistyökäytännöt ovat toteutuneet? \n" +
                                                "Tässä kohdassa huomioidaan seuraavat asiat, jos lapsi on liittyvässä varhaiskasvatuksessa:\n" +
                                                "Miten sovitut pedagogiset, rakenteelliset ja/tai hoidolliset tuen muodot ovat toteutuneet, ja mitkä ovat olleet niiden vaikutukset? Lapsen tuen tarvetta sekä tuen riittävyyttä, tarkoituksenmukaisuutta ja vaikuttavuutta on arvioitava ja seurattava sekä leopsia päivitettävä aina tuen tarpeen muuttuessa. Tuen vaikuttavuuden arviointi pitää sisällään kuvauksen tukitoimista, niiden vaikuttavuuden arvioinnista ja kehittämisestä sekä perustelut siitä, millaisista tuen toimista lapsi hyötyy ja mitkä parhaiten toteuttavat yksilöllisesti lapsen etua. Jos lapsi saa tehostettua tai erityistä tukea, tai tukipalveluita osana yleistä tukea leopsia päivitetään hallinnollisen päätöksen sisällön mukaisesti."
                                        OfficialLanguage.SV ->
                                            "Detta fält fylls i om barnet har fått stöd. Utvärderingen av stödet omfattar en beskrivning av stödåtgärderna och en utvärdering av hur väl de fungerat. Har barnets stöd och stödåtgärderna som getts varit effektiva och tillräckliga?\nHur har de överenskomna pedagogiska, strukturella och/eller vårdinriktade stödformerna genomförts och vilka effekter har de haft? Hur har det sektorsövergripande samarbetet genomförts?\n\n" +
                                                "Om barnet deltar i kompletterande småbarnspedagogik ska även följande stycke beaktas i detta fält:\n\n" +
                                                "Barnets behov av stöd samt stödets tillräcklighet, ändamålsenlighet och effekt ska utvärderas och följas upp och plan för barnets lärande ska uppdateras alltid när stödbehovet ändrar. Utvärderingen av stödets effekt ska omfatta en beskrivning av stödåtgärderna och deras effekt samt argument för vilka stödåtgärder som bäst gynnar barnet. Om barnet får intensifierat eller särskilt stöd eller stödtjänster som en del av det allmänna stödet uppdateras plan för barnets lärande i förskolan i enlighet med förvaltningsbeslutet."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Muut havainnot lapselle aiemmin laaditusta varhaiskasvatussuunnitelmasta tai esiopetuksen oppimissuunnitelmasta"
                                        OfficialLanguage.SV ->
                                            "Övriga kommentarer om barnets tidigare plan för småbarnspedagogik eller plan för barnets lärande i förskolan"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen esiopetuksen oppimissuunnitelma tulee tehdä, arvioida ja tarkistaa vähintään kaksi kertaa vuodessa tai useammin, jos lapsen tarpeet sitä edellyttävät. Leopsia arvioitaessa arviointi kohdistuu pedagogiikan toteutumiseen, oppimisympäristöihin ja toiminnan järjestelyihin sekä mahdolliseen tuen vaikuttavuuteen ja tukitoimien toteutumiseen. Lapsen tuki tulee tarkistaa esiopetuksen alkaessa. \n" +
                                                "Leopsin tarkistaminen perustuu arviointiin yhdessä lapsen ja huoltajan kanssa. Tarkoituksena on varmistaa, että lapsen vasusta ja leopsista muodostuu jatkumo. Tässä osiossa voidaan tarkastella aiemmin laadittua lapsen vasua ja yhdessä huoltajan kanssa nostaa keskeisiä asioita leopsiin. Leopsin tavoitteita sekä niiden toteuttamista seurataan ja arvioidaan säännöllisesti."
                                        OfficialLanguage.SV ->
                                            "Plan för barnets lärande i förskolan ska utvärderas och granskas minst en gång om året eller oftare om barnets behov kräver det. Utvärderingen av plan för barnets lärande fokuserar på genomförandet av pedagogiken, lärmiljöerna och åtgärderna för verksamheten samt det eventuella stödets effekt och genomförandet av stödåtgärderna. Barnets stöd ska granskas när hen inleder förskoleundervisning.\n\n" +
                                                "Plan för barnets lärande i förskolan granskas utgående från den utvärdering som görs tillsammans med barnet och vårdnadshavare. Detta görs för att skapa kontinuitet mellan barnets plan för småbarnspedagogik och plan för barnets lärande i förskolan. I detta fält kan barnets tidigare plan för småbarnspedagogik granskas tillsammans med vårdnadshavare och därifrån kan innehåll eventuellt överföras till plan för barnets lärande i förskolan. Målen i plan för barnets lärande i förskolan, samt hur väl de uppnåtts, ska följas upp och utvärderas regelbundet."
                                    },
                                multiline = true,
                                value = ""
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Tavoitteet pedagogiselle toiminnalle ja toimenpiteet tavoitteiden saavuttamiseksi"
                            OfficialLanguage.SV ->
                                "Mål för den pedagogiska verksamheten och åtgärder för att uppnå målen"
                        },
                    questions =
                        listOfNotNull(
                            VasuQuestion.Paragraph(
                                title = "",
                                paragraph =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tavoitteet ja toimenpiteet koskevat esiopetuksen ja liittyvän varhaiskasvatuksen kokonaisuutta sisältäen lapsen tuen."
                                        OfficialLanguage.SV ->
                                            "Målen och åtgärderna omfattar den helhet som utgörs av förskoleundervisningen och den kompletterande småbarnspedagogiken, inklusive barnets eventuella stöd."
                                    }
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen vahvuudet, kiinnostuksen kohteet ja tarpeet sekä niiden huomioon ottaminen"
                                        OfficialLanguage.SV ->
                                            "Barnets styrkor, intressen och behov samt hur dessa beaktas i verksamheten"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kuvataan lapsen keskeiset vahvuudet, kiinnostuksen kohteet, oppimisvalmiudet ja tarpeet tavoitteiden asettamisen ja toiminnan suunnittelun pohjaksi."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs barnets styrkor, intressen och behov som grund för målsättningen och planeringen av verksamheten."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Kieleen ja kulttuuriin liittyviä tarkentavia näkökulmia"
                                        OfficialLanguage.SV ->
                                            "Mål och metoder för hur barnets kulturella- och språkliga identitet/identiteter bekräftas samt hur barnets språkutveckling stöds"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tässä kohdassa kirjataan, miten edistetään monipuolisesti vieraskielisten ja monikielisten lasten kielitaidon sekä kieli- ja kulttuuri-identiteettien ja itsetunnon kehittymistä. Huoltajien kanssa keskustellaan myös lapsen oman äidinkielen/äidinkielien tukemisesta.\n" +
                                                "KieliPeda-työvälineen osat 2 ja 3 ovat leopsin liitteitä. Tähän kohtaan merkitään niistä nousevat keskeiset asiat, kuten lapsen kielimaailma ja suomen kielen taidon kehittyminen. Suomen kielen taitotaso merkitään kohtaan 5.3.\n" +
                                                "Tähän kohtaan voi myös kirjata kieleen ja kulttuuriin liittyviä muita tarkentavia näkökulmia pedagogisiin tavoitteisiin ja toimenpiteisiin liittyen esimerkiksi saamen kieleen, romanikieleen ja viittomakieleen."
                                        OfficialLanguage.SV ->
                                            "Här antecknas hur man bidrar till en mångsidig utveckling av språkkunskaperna, den språkliga och kulturella identiteten och självförtroendet hos fler– och tvåspråkiga barn, eller barn med ett främmande språk som modersmål. I den svenskspråkiga förskoleundervisningen i Esbo är det vanligt med barn som hemma talar både finska och svenska. I detta fält antecknas hur man i verksamheten målmedvetet stöder och följer upp barnets språkutveckling i verksamhetsspråket, samtidigt som man bekräftar barnets båda språkliga identiteter. Använd det språkliga observationsschemat som stöd för en helhetsbild av barnets språkliga miljö."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            when (lang) {
                                OfficialLanguage.FI ->
                                    VasuQuestion.CheckboxQuestion(
                                        name =
                                            "Tätä oppimissuunnitelmaa käytetään perusopetukseen valmistavan opetuksen toteuttamiseksi",
                                        value = false,
                                        info =
                                            "Tähän kohtaan merkitään rasti, jos tätä oppimissuunnitelmaa käytetään perusopetukseen valmistavan opetuksen toteuttamiseksi.",
                                        notNumbered = true
                                    )
                                OfficialLanguage.SV -> null
                            },
                            VasuQuestion.Paragraph(
                                title =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen tukeen liittyvät tarpeet, tuen järjestämiseen liittyvät asiat sekä tuen muodot (pedagogiset, rakenteelliset ja hoidolliset) esiopetuksessa ja liittyvässä varhaiskasvatuksessa"
                                        OfficialLanguage.SV ->
                                            "Barnets behov av stöd, ordnande av stödet samt stödformerna (pedagogiska, strukturella och vårdinriktade) i förskoleundervisningen och den kompletterande småbarnspedagogiken"
                                    },
                                paragraph =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tuen järjestämisen lähtökohtana on lapsen kokonaisen päivän huomioiminen."
                                        OfficialLanguage.SV ->
                                            "Hela barnets dag beaktas när stödet ordnas."
                                    }
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen tukeen liittyvät tarpeet esiopetuksessa ja liittyvässä varhaiskasvatuksessa"
                                        OfficialLanguage.SV ->
                                            "Barnets behov av stöd i förskoleundervisningen och den kompletterande småbarnspedagogiken"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän  kohtaan  kirjataan  lapsen  tukeen  liittyvät  tarpeet,  jotka  heijastuvat  toiminnalle  asetettaviin tavoitteisiin sekä toimenpiteisiin ja menetelmiin tavoitteiden saavuttamiseksi.\nKirjaa tähän myös se, miten huoltajat tukevat lasta sovituissa asioissa."
                                        OfficialLanguage.SV ->
                                            "I detta fält beskrivs barnets eventuella behov av stöd som påverkar de mål som anges för verksamheten samt metoderna och åtgärderna för att uppnå målen."
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.MultiSelectQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tuen järjestämiseen liittyvät asiat esiopetuksessa"
                                        OfficialLanguage.SV ->
                                            "Ordnande av stöd i förskoleundervisningen"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Rastita tarvittavat kohdat."
                                        OfficialLanguage.SV -> "Kryssa för de rutor som är aktuella"
                                    },
                                value = emptyList(),
                                options =
                                    listOf(
                                        QuestionOption(
                                            key = "partTimeSpecialNeedsEducation",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Lapsi saa osa-aikaista erityisopetusta."
                                                    OfficialLanguage.SV ->
                                                        "Barnet får specialundervisning på deltid"
                                                },
                                            info =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Jos lapsella ilmenee vaikeuksia oppimisessaan, on hänellä oikeus saada osa-aikaista erityisopetusta muun esiopetuksen ohessa kaikilla tuen tasoilla. Osa-aikaisen erityisopetuksen tavoitteena on vahvistaa lapsen oppimisen edellytyksiä, ehkäistä kehityksen ja oppimisen vaikeuksia. Osa-aikaisen erityisopetuksen tarve arvioidaan ja suunnitellaan yhteistyössä esiopettajan ja varhaiskasvatuksen erityisopettajan kanssa. Osa-aikaista erityisopetusta annetaan joustavin järjestelyin samanaikaisopetuksena, pienryhmissä tai yksilöllisesti. Tavoitteet sisällytetään lapsen saamaan muuhun opetukseen. Vaikutuksia arvioidaan opettajien yhteistyönä sekä lapsen että huoltajien kanssa. Huoltajille tiedotetaan yksikön toimintatavoista."
                                                    OfficialLanguage.SV ->
                                                        "Ett barn som har svårigheter i sitt lärande eller sin skolgång har rätt att få specialundervisning på deltid vid sidan om den övriga förskoleundervisningen.Målet med specialundervisningen på deltid är att stärka barnets inlärningsförutsättningar och förebygga svårigheter i utvecklingen och lärandet. Specialundervisning på deltid ges på alla nivåer av stöd. Behovet av specialundervisning på deltid bedöms och planeras i samarbete med förskolelärare och speciallärare inom småbarnspedagogik. Specialundervisningen på deltid ska ordnas flexibelt som kompanjonundervisning, i en liten grupp eller som individuell undervisning.Målen för och innehållet i specialundervisningen på deltid ska integreras i barnets övriga undervisning.Effekten av undervisningen bedöms av lärarna i samråd med både barnet och vårdnadshavarna. Alla vårdnadshavare ska informeras om hur specialundervisning på deltid genomförs i förskolan."
                                                }
                                        ),
                                        QuestionOption(
                                            key = "pedagogicalEvaluationMade",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Pedagoginen arvio on tehty tehostetun tuen käynnistämiseksi, pvm."
                                                    OfficialLanguage.SV ->
                                                        "En pedagogisk bedömning har gjorts för att inleda intensifierat stöd, datum"
                                                },
                                            date = true,
                                            info =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "• Laaditaan tehostettua tukea varten, kun ilmenee, ettei yleinen tuki esiopetuksessa ole lapselle riittävää.\n• Tehdään tehostettua tukea varten lapsen perusopetukseen siirtymistä valmisteltaessa.\n• Laatimisesta vastaa esiopettaja."
                                                    OfficialLanguage.SV ->
                                                        "• Görs innan intensifierat stöd inleds om det framkommer att det allmänna stödet inte är tillräckligt för barnet\n• Görs för att utreda behovet av intensifierat stöd innan övergången till den grundläggande utbildningen\n• Den pedagogiska bedömningen görs av barnets lärare"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "learningPlanUsedForAssistance",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Tätä oppimissuunnitelmaa käytetään tehostetun tuen toteuttamiseksi."
                                                    OfficialLanguage.SV ->
                                                        "Denna plan för barnets lärande i förskolan används vid genomförandet av det intensifierade stödet"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "pedagogicalStatementMade",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Pedagoginen selvitys on tehty erityisen tuen tarpeen arvioimiseksi."
                                                    OfficialLanguage.SV ->
                                                        "En pedagogisk utredning har gjorts för att utreda behovet av särskilt stöd"
                                                },
                                            info =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "• Laaditaan erityistä tukea varten lapselle, jolle tehostettu tuki ei riitä.\n• Laaditaan tarvittaessa esiopetusvuoden aikana lapsen perusopetukseen siirtymistä valmisteltaessa.\n• Laatimisesta vastaa esiopettaja.\nKs. erilliset ohjeet pedagogisen arvion ja selvityksen lomakkeista."
                                                    OfficialLanguage.SV ->
                                                        "• Görs innan särskilt stöd inleds om det framkommer att intensifierat stöd inte är tillräckligt för barnet\n• Görs vid behov under verksamhetsåret innan övergången till den grundläggande utbildningen\n• Den pedagogiska utredningen görs av barnets lärare i samarbete med en speciallärare inom småbarnspedagogik och vid behov övriga sakkunniga"
                                                }
                                        )
                                    ),
                                minSelections = 0,
                                maxSelections = null,
                                textValue = emptyMap()
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lisätietoja tuen järjestämiseen liittyvistä asioista esiopetuksessa"
                                        OfficialLanguage.SV ->
                                            "Ytterligare information gällande ordnande av stöd inom förskoleundervisningen"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lisäksi tähän kuvataan tarvittavat\n• pedagogiset ratkaisut: oppimisympäristöihin liittyvät ratkaisut sekä lapsen tukeen liittyvät ratkaisut (joustavat ryhmittelyt, samanaikaisopetus, opetusmenetelmät, työskentely- ja kommunikointitavat tms.)\n• lapselle tarjottava osa-aikainen erityisopetus\n• esiopetukseen osallistumisen edellyttämät perusopetuslain mukaiset tulkitsemis- ja avustajapalvelut, muut opetuspalvelut ja erityiset apuvälineet, joista on tehty päätös."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs\n\n• Pedagogiska lösningar: lösningar gällande lärmiljöer samt lösningar i anslutning till barnets stöd (såsom flexibel gruppindelning, kompanjonundervisning, undervisningsmetoder, arbetssätt och sätt att kommunicera)\n• Specialundervisning på deltid som ges till barnet\n• Tolknings- och assistenttjänster som är nödvändiga för att barnet ska kunna delta i förskoleundervisningen, övriga stödtjänster som det beslutats om"
                                    },
                                multiline = true,
                                value = ""
                            ),
                            VasuQuestion.MultiField(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tuen muodot, jos lapsi on liittyvässä varhaiskasvatuksessa"
                                        OfficialLanguage.SV ->
                                            "Stödformer, om barnet deltar i kompletterande småbarnspedagogik"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan esiopetukseen liittyvässä varhaiskasvatuksessa olevan lapsen tukeen liittyvät mahdolliset muut tarpeet sekä lapsen tuen toteuttamiseen liittyvät pedagogiset, rakenteelliset ja hoidolliset tuen muodot. Tähän kirjataan myös mahdolliset lapselle annettavat tukipalvelut. Leopsia hyödynnetään tehtäessä hallinnollista päätöstä annettavasta tehostetusta tai erityisestä tuesta tai yleisen tuen tukipalveluista. Mikäli lapsen tuen tarvetta on arvioitu leopsissa, tulee arviointi huomioida annettaessa tehostetun tai erityisen tuen hallinnollista päätöstä tai päätöstä yleisen tuen tukipalveluista. Leopsia päivitetään hallintopäätöksen sisällön mukaisesti. Lisäksi leopsiin kirjataan mahdolliset sosiaali- ja terveyspalvelut, kuten lapsen saama kuntoutus, jos se on olennaista lapsen esiopetuksen ja liittyvän varhaiskasvatuksen järjestämisen näkökulmasta."
                                        OfficialLanguage.SV ->
                                            "Här antecknas barnets eventuella behov av stöd samt de pedagogiska, strukturella och vårdinriktade stödformerna i anslutning till genomförandet av barnets stöd. Även barnets eventuella stödtjänster antecknas här. Plan för barnets lärande i förskolan utnyttjas när man fattar förvaltningsbeslut om intensifierat eller särskilt stöd eller om stödtjänster inom allmänt stöd. Om barnets stödbehov har bedömts i plan för barnets lärande i förskolan, ska bedömningen beaktas när förvaltningsbeslut om intensifierat eller särskilt stöd och beslut om stödtjänsterna inom allmänt stöd fattas. Plan för barnets lärande i förskolan ska uppdateras i enlighet med innehållet i förvaltningsbeslutet. I planen ska även antecknas eventuella social- och hälsovårdstjänster, såsom barnets rehabilitering, om de är väsentliga för ordnandet av barnets förskoleundervisning och kompletterande småbarnspedagogik."
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field(
                                                    name = "Pedagogiset tuen muodot",
                                                    info =
                                                        "• päivän rakenteeseen ja päivärytmiin liittyvät ratkaisut \n" +
                                                            "• oppimisympäristöihin liittyvät ratkaisut \n" +
                                                            "• tarvittavat erityispedagogiset menetelmät \n" +
                                                            "• vuorovaikutus- ja kommunikointitavat, esimerkiksi viittomien ja kuvien käyttö \n" +
                                                            "• käytännöt, miten lapsi pääsee osalliseksi vertaisryhmän toimintaa, esimerkiksi esteettömyyden huomiointi."
                                                ),
                                                Field(
                                                    name = "Rakenteelliset tuen muodot",
                                                    info =
                                                        "• tuen toteuttamiseen liittyvän osaamisen ja erityispedagogisen osaamisen vahvistaminen \n" +
                                                            "• henkilöstön mitoitukseen ja rakenteeseen liittyvät ratkaisut \n" +
                                                            "• lapsiryhmän kokoon ja ryhmärakenteeseen liittyvät ratkaisut \n" +
                                                            "• tulkitsemis- ja avustamispalvelut sekä apuvälineiden käyttö \n" +
                                                            "• pien- tai erityisryhmä tai muu tarvittava ryhmämuoto \n" +
                                                            "• varhaiskasvatuksen erityisopettajan osa- tai kokoaikainen opetus tai konsultaatio."
                                                ),
                                                Field(
                                                    name = "Hoidolliset tuen muodot",
                                                    info =
                                                        "• perushoitoon, hoivaan ja avustamiseen liittyvät menetelmät \n" +
                                                            "• terveydenhoidolliset tarpeet, esimerkiksi lapsen pitkäaikaissairauksien hoitoon, lääkitykseen, ruokavalioon ja liikkumiseen liittyvä avustaminen ja apuvälineet."
                                                )
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field(
                                                    name = "Pedagogiska stödformer",
                                                    info =
                                                        "• lösningar gällande dagliga rutiner och struktur inom småbarnspedagogiken\n" +
                                                            "• lösningar gällande lärmiljöer\n" +
                                                            "• specialpedagogiska metoder\n" +
                                                            "• metoder för växelverkan och kommunikation, till exempel användning av tecken och bilder\n" +
                                                            "• verksamhetssätt för hur barnet kan vara delaktigt i barngruppens verksamhet, till exempel beaktande av tillgänglighet (t.ex. delaktighet i leken/sociala gemenskapen, fysisk tillgänglighet, möjlighet att uttrycka sig på ett för barnet tillgängligt sätt etc.)"
                                                ),
                                                Field(
                                                    name = "Strukturella stödformer",
                                                    info =
                                                        "• att stärka kunnandet och den specialpedagogiska kompetensen som berör genomförande av stödet\n" +
                                                            "• lösningar angående personaldimensionering och personalstruktur\n" +
                                                            "• lösningar som rör barngruppens storlek och gruppens sammansättning\n" +
                                                            "• tolknings- och assistenttjänster samt användning av hjälpmedel\n" +
                                                            "• smågrupp, specialgrupp eller annan gruppform enligt behoven\n" +
                                                            "• konsultation eller undervisning som ges på hel- eller deltid av en speciallärare inom småbarnspedagogik"
                                                ),
                                                Field(
                                                    name = "Vårdinriktade stödformer",
                                                    info =
                                                        "• metoder som berör grundläggande vård, omsorg och assistans\n" +
                                                            "• verksamhet som tillgodoser behov av hälso- och sjukvård, till exempel hjälpmedel och assistans som hänför sig till barnets långtidssjukdomar, medicinering, kost och rörlighet"
                                                )
                                            )
                                    },
                                value = listOf("", "", ""),
                                separateRows = true
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tavoitteet pedagogiselle toiminnalle"
                                        OfficialLanguage.SV ->
                                            "Mål för den pedagogiska verksamheten"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kirjataan keskeiset tavoitteet henkilöstön pedagogiselle toiminnalle. Tavoitteiden asettamisessa tulee hyödyntää lapsen vahvuuksia, kiinnostuksen kohteita, oppimisvalmiuksia ja tarpeita. Tässä osassa huomioidaan lapsen orastavat taidot ja se, miten niitä voidaan edistää pedagogisella toiminnalla. Henkilöstön toiminnalle asetetut tavoitteet voivat liittyä mm. lapsen oppimiseen, työskentely- ja vuorovaikutustaitoihin. Olennaista on kirjata tavoitteet lapsen kokonaiselle päivälle huomioiden mahdolliset lapsen tuen kannalta merkitykselliset asiat. Tässä huomioidaan myös laaja-alaisen osaamisen osa-alueita ja opetuksen yhteisiä tavoitteita. Tavoitteita asetettaessa otetaan huomioon lapsiryhmä ja ryhmän kokonaistilanne, tavoitteiden konkreettisuus ja arvioitavuus."
                                        OfficialLanguage.SV ->
                                            "Här anges centrala mål för personalens pedagogiska verksamhet. Målen ska beskriva hur man med den pedagogiska verksamheten och lärmiljöerna stöder barnets utveckling, lärande och välbefinnande. Barnets styrkor, intressen, färdigheter och behov ska beaktas då målen anges. Målen för personalens pedagogiska verksamhet kan gälla t.ex. barnets lärande eller förmåga till kommunikation och växelverkan. Här beaktas även delområden inom mångsidig kompetens och lärområden. Det är viktigt att de mål som ställs upp utgår från barngruppen och dess situation, samt att de är konkreta och möjliga att utvärdera."
                                    },
                                value = "",
                                multiline = true
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Toimenpiteet ja menetelmät tavoitteiden saavuttamiseksi"
                                        OfficialLanguage.SV ->
                                            "Åtgärder, metoder och lärmiljöer för att uppnå målen för den pedagogiska verksamheten"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kirjataan konkreettiset pedagogiset toimenpiteet ja menetelmät pedagogiselle toiminnalle asetettujen tavoitteiden saavuttamiseksi. Menetelmät tulee kirjata niin konkreettisina, että niiden toteutumisen arviointi on mahdollista."
                                        OfficialLanguage.SV ->
                                            "Här beskrivs de konkreta pedagogiska åtgärder och metoder som syftar till att uppnå de mål som angetts för den pedagogiska verksamheten. Metoderna ska beskrivas på så konkret nivå att förverkligandet av dem kan utvärderas."
                                    },
                                value = "",
                                multiline = true
                            ),
                            VasuQuestion.MultiSelectQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen tuen toteuttamista koskeva hallintopäätös"
                                        OfficialLanguage.SV -> "Förvaltningsbeslut om barnets stöd"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan, jos lapsen tuesta esiopetuksessa ja/tai liittyvässä varhaiskasvatuksessa on annettu hallintopäätös. Leopsiin kirjataan myös päivämäärä, jos hallintopäätös kumotaan. Muihin huomioihin voidaan kirjata hallintopäätökseen liittyviä tarkentavia näkökulmia."
                                        OfficialLanguage.SV ->
                                            "Här antecknas datumet för förvaltningsbeslutet om barnets stöd inom förskoleundervisningen och/eller den kompletterande småbarnspedagogiken om ett sådant gjorts. Om förvaltningsbeslutet hävs, ska datumet för detta likaså antecknas i plan för barnets lärande i förskolan. I fältet Övrigt att beakta kan man anteckna preciserande synpunkter gällande förvaltningsbeslutet."
                                    },
                                options =
                                    listOf(
                                        QuestionOption(
                                            key = "general",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Tukipalvelut (yleinen tuki)"
                                                    OfficialLanguage.SV ->
                                                        "Stödtjänster (allmänt stöd)"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "intensified",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Tehostettu tuki"
                                                    OfficialLanguage.SV -> "Intensifierat stöd"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "special",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Erityinen tuki"
                                                    OfficialLanguage.SV -> "Särskilt stöd"
                                                }
                                        )
                                    ),
                                value = emptyList(),
                                minSelections = 0,
                                maxSelections = 2
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muita huomioita"
                                        OfficialLanguage.SV -> "Övrigt att beakta"
                                    },
                                value = "",
                                multiline = true
                            ),
                            when (lang) {
                                OfficialLanguage.FI ->
                                    VasuQuestion.Followup(
                                        title =
                                            when (lang) {
                                                OfficialLanguage.FI ->
                                                    "Tarkennuksia esiopetusvuoden aikana lapsen tarpeiden mukaan"
                                                OfficialLanguage.SV -> ""
                                            },
                                        name =
                                            when (lang) {
                                                OfficialLanguage.FI -> "Päivämäärä ja kirjaus"
                                                OfficialLanguage.SV -> ""
                                            },
                                        info =
                                            when (lang) {
                                                OfficialLanguage.FI ->
                                                    "Tämän kohdan tarkoituksena on varmistaa leopsiin kirjattujen tavoitteiden ja toimenpiteiden toteutumisen jatkuva arviointi. Jatkuvalla arvioinnilla tarkoitetaan havainnoinnin ja pedagogisen dokumentoinnin avulla tarkennettavia tavoitteita ja toimenpiteitä. Näistä keskustellaan huoltajien kanssa päivittäisissä kohtaamisissa. Jatkuvan arvioinnin avulla leops pysyy ajan tasalla. \n"
                                                OfficialLanguage.SV -> ""
                                            },
                                        value = emptyList(),
                                        continuesNumbering = true
                                    )
                                OfficialLanguage.SV -> null
                            }
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Muut mahdolliset lapsen esiopetusvuonna huomioitavat asiat"
                            OfficialLanguage.SV ->
                                "Övrigt att beakta i barnets förskoleundervisning"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muita huomioita"
                                        OfficialLanguage.SV -> "Övrigt att beakta"
                                    },
                                multiline = true,
                                value = "",
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän osioon kirjataan muita huomioitavia asioita, kuten esimerkiksi lepoon, ruokailuun tai pukemiseen liittyvät asiat. \n" +
                                                "Keskustellaan tarvittaessa huoltajien ajatuksista tyttöjen ympärileikkauksesta, ks. erillinen ohje Tyttöjen sukuelinten silpomisen estäminen. Tähän kirjataan huoltajien ajatukset asiasta. Jos huoli herää, toimi em. ohjeistuksen mukaan."
                                        OfficialLanguage.SV ->
                                            "Här antecknas övriga saker som ska beaktas, till exempel gällande vila, måltider eller påklädning."
                                    }
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Laatimisessa hyödynnetyt muut mahdolliset asiakirjat ja suunnitelmat"
                            OfficialLanguage.SV ->
                                "Eventuella övriga dokument och planer som använts vid uppgörandet av plan för barnets lärande"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Muut asiakirjat ja suunnitelmat"
                                        OfficialLanguage.SV -> "Övriga dokument och planer"
                                    },
                                multiline = true,
                                value = "",
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen esiopetuksen oppimissuunnitelman laatimisessa voidaan hyödyntää muita mahdollisia suunnitelmia kuten esimerkiksi lääkehoitosuunnitelmaa."
                                        OfficialLanguage.SV ->
                                            "Vid uppgörandet av plan för barnets lärande i förskolan kan eventuella andra planer och dokument utnyttjas, t.ex., planen för läkemedelsbehandling."
                                    }
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI ->
                                "Lapsen esiopetuksen oppimissuunnitelmakeskustelu"
                            OfficialLanguage.SV -> "Samtal om plan för barnets lärande i förskolan"
                        },
                    questions =
                        listOfNotNull(
                            VasuQuestion.Paragraph(
                                title =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Oppimissuunnitelma on laadittu yhteistyössä huoltajien kanssa"
                                        OfficialLanguage.SV ->
                                            "Plan för barnets lärande i förskolan har gjorts upp i samarbete med vårdnadshavare"
                                    },
                                paragraph = ""
                            ),
                            VasuQuestion.DateQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Keskustelun päivämäärä"
                                        OfficialLanguage.SV ->
                                            "Datum för samtalet om plan för barnets lärande"
                                    },
                                trackedInEvents = true,
                                nameInEvents =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Oppimissuunnitelmakeskustelu"
                                        OfficialLanguage.SV -> "Samtalet om barnets plan"
                                    },
                                value = null
                            ),
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Keskusteluun osallistuneet huoltajat"
                                        OfficialLanguage.SV ->
                                            "Vårdnadshavare som deltog i samtalet"
                                    },
                                multiline = true,
                                value = ""
                            ),
                            when (lang) {
                                OfficialLanguage.FI ->
                                    VasuQuestion.TextQuestion(
                                        name =
                                            "Huoltajan/huoltajien kanssa tehty yhteistyö sekä näkemys varhaiskasvatussuunnitelman sisällöstä/huoltajien kuuleminen hallintopäätöstä koskien",
                                        info =
                                            "Tähän kohtaan kirjataan huoltajien kuuleminen, mikäli tuesta tehdään hallintopäätös.",
                                        multiline = true,
                                        value = ""
                                    )
                                OfficialLanguage.SV -> null
                            }
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Tiedonsaajatahot"
                            OfficialLanguage.SV -> "Överförande av barnets plan till berörda parter"
                        },
                    questions =
                        listOf(
                            VasuQuestion.MultiSelectQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tämä oppimissuunnitelma luovutetaan huoltajan/huoltajien luvalla"
                                        OfficialLanguage.SV ->
                                            "Barnets plan kan med vårdnadshavares tillstånd överföras till"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Lapsen esiopetuksen oppimissuunnitelma siirretään lapsen tulevaan kouluun esiopetusvuoden keväällä. Siirrosta sovitaan tapauskohtaisesti huoltajien kanssa käytävässä keskustelussa. Esiopetuksen oppimissuunnitelman luovuttamisesta muille tahoille pyydetään huoltajilta lupa. Oppimissuunnitelma voidaan kuitenkin luovuttaa uudelle varhaiskasvatuksen, esiopetuksen tai perusopetuksen järjestäjälle myös ilman huoltajan lupaa, jos se on välttämätöntä lapsen varhaiskasvatuksen, esi- tai perusopetuksen järjestämiseksi (Varhaiskasvatuslaki 41 §:n 3 mom., perusopetuslaki 40 §:n 4 mom. ja 41 §:n 4 mom.)."
                                        OfficialLanguage.SV ->
                                            "Barnets plan för lärande i förskolan kan lämnas över till den nya anordnaren av småbarnspedagogik, förskoleundervisning eller grundläggande utbildning, också utan samtycke av vårdnadshavare, ifall uppgifterna är nödvändiga för att ordna småbarnspedagogik, förskoleundervisning eller grundläggande utbildning för barnet (lag om småbarnspedagogik 41 §, lag om grundläggande utbildning 40 § och 41 §)."
                                    },
                                options =
                                    listOf(
                                        QuestionOption(
                                            key = "tiedonsaajataho_tuleva_koulu",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Tulevaan kouluun"
                                                    OfficialLanguage.SV ->
                                                        "Den blivande grundskolan"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "tiedonsaajataho_neuvola",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Neuvolaan"
                                                    OfficialLanguage.SV -> "Rådgivningsbyrån"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "tiedonsaajataho_terapiapalveluihin",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI ->
                                                        "Lasten terapiapalveluihin"
                                                    OfficialLanguage.SV ->
                                                        "Terapitjänsterna för barn"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "tiedonsaajataho_erikoissairaanhoitoon",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Erikoissairaanhoitoon"
                                                    OfficialLanguage.SV -> "Specialsjukvården"
                                                }
                                        ),
                                        QuestionOption(
                                            key = "tiedonsaajataho_muualle",
                                            name =
                                                when (lang) {
                                                    OfficialLanguage.FI -> "Muualle, minne?"
                                                    OfficialLanguage.SV -> "Annanstans, vart?"
                                                },
                                            textAnswer = true
                                        )
                                    ),
                                minSelections = 0,
                                maxSelections = null,
                                value = emptyList(),
                                textValue = emptyMap()
                            ),
                            VasuQuestion.MultiFieldList(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Päiväykset ja allekirjoitukset"
                                        OfficialLanguage.SV -> "Datum och underskrifter"
                                    },
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Allekirjoittamalla annan suostumuksen tämän oppimissuunnitelman siirtämiseen yllämainituille tahoille."
                                        OfficialLanguage.SV ->
                                            "Genom att underteckna samtycker jag till överföringen av plan för barnets lärande i förskolan till ovanstående parter."
                                    },
                                keys =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            listOf(
                                                Field("Päiväys"),
                                                Field("Huoltajan allekirjoitus"),
                                                Field("Nimenselvennys")
                                            )
                                        OfficialLanguage.SV ->
                                            listOf(
                                                Field("Datum"),
                                                Field("Vårdnadshavarens underskrift"),
                                                Field("Namnförtydligande")
                                            )
                                    },
                                value = listOf(listOf("", "", ""))
                            )
                        )
                ),
                VasuSection(
                    name =
                        when (lang) {
                            OfficialLanguage.FI -> "Seuranta- ja arviointiajankohdat"
                            OfficialLanguage.SV ->
                                "Tidpunkterna för uppföljning och utvärdering av plan för barnets lärande i förskolan"
                        },
                    questions =
                        listOf(
                            VasuQuestion.TextQuestion(
                                name =
                                    when (lang) {
                                        OfficialLanguage.FI -> "Ajankohdat ja kuvaus"
                                        OfficialLanguage.SV -> "Tidpunkter och beskrivning"
                                    },
                                multiline = true,
                                value = "",
                                info =
                                    when (lang) {
                                        OfficialLanguage.FI ->
                                            "Tähän kohtaan kirjataan huoltajan kanssa yhdessä sovittu jatkosuunnitelma ja milloin suunnitelmaa seuraavan kerran arvioidaan."
                                        OfficialLanguage.SV ->
                                            "Här antecknas hur man tillsammans med vårdnadshavarna går vidare med planen, samt datum för när planen ska utvärderas nästa gång."
                                    }
                            )
                        )
                )
            )
    )

private val ophQuestionMap =
    mapOf(
        OphQuestionKey.PEDAGOGIC_ACTIVITY_GOALS to
            OphQuestion(
                name =
                    mapOf(
                        OfficialLanguage.FI to "Tavoitteet henkilöstön pedagogiselle toiminnalle",
                        OfficialLanguage.SV to "Mål för personalens pedagogiska aktiviteter"
                    ),
                options =
                    listOf(
                        OphQuestionOption(
                            key = "goal1",
                            name =
                                mapOf(
                                    OfficialLanguage.FI to "Joku tavoite",
                                    OfficialLanguage.SV to "mål 1"
                                )
                        ),
                        OphQuestionOption(
                            key = "goal2",
                            name =
                                mapOf(
                                    OfficialLanguage.FI to "Toinen tavoite",
                                    OfficialLanguage.SV to "mål 2"
                                )
                        ),
                        OphQuestionOption(
                            key = "goal3",
                            name =
                                mapOf(
                                    OfficialLanguage.FI to "Kolmas vaihtoehto",
                                    OfficialLanguage.SV to "mål 3"
                                )
                        )
                    )
            ),
        OphQuestionKey.PEDAGOGIC_GOALS_DESCRIPTION to
            OphQuestion(
                name =
                    mapOf(
                        OfficialLanguage.FI to "Kuvaile tavoitteita tarkemmin",
                        OfficialLanguage.SV to "Beskriv målen mer i detalj"
                    ),
                options = emptyList()
            )
    )

// TODO: this won't currently work
fun copyTemplateContentWithCurrentlyValidOphSections(template: VasuTemplate): VasuContent {
    val copyableSections =
        template.content.sections.filter { section ->
            section.questions.all { question -> question.ophKey == null }
        }
    val defaultSections = getDefaultTemplateContent(template.type, template.language).sections
    return VasuContent(sections = copyableSections + defaultSections)
}
