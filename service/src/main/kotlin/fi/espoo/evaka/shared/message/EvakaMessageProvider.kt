// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.message

import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.shared.domain.OfficialLanguage

class EvakaMessageProvider : IMessageProvider {

    override fun getDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> """Espoon varhaiskasvatukseen liittyvät päätökset"""
            OfficialLanguage.SV -> """Beslut gällande Esbos småbarnspedagogik"""
        }

    override fun getDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI ->
                """Olette hakenut lapsellenne Espoon kaupungin varhaiskasvatus-, esiopetus- ja/tai kerhopaikkaa. Koska olette ottanut Suomi.fi viestit -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.

Päätös on hakemuksen tehneen huoltajan hyväksyttävissä/hylättävissä Espoon kaupungin varhaiskasvatuksen sähköisessä palvelussa osoitteessa espoonvarhaiskasvatus.fi . Suomi.fi -palvelussa ei voi antaa vastausta sähköisesti, mutta päätöksen yhteydestä voi tulostaa paperisen vastauslomakkeen.

Huomioittehan, että vastaus päätökseen tulee antaa kahden viikon kuluessa.



In English:

You have applied for a place in the City of Espoo’s early childhood education, pre-primary education and/or a club for your child. As you are a user of Suomi.fi Messages, you can find the decision in the attachments below.

The guardian who submitted the application can accept or reject the decision through the online service of the City of Espoo Early Childhood Education at espoonvarhaiskasvatus.fi. You cannot respond to the decision online through the Suomi.fi service, but you can print out a response form that is attached to the decision.

Please note that you have to respond to the decision within two weeks."""
            OfficialLanguage.SV ->
                """Du har ansökt om plats i Esbo stads småbarnspedagogiska verksamhet, förskoleundervisning och/eller klubbverksamhet. Eftersom du har tagit i bruk Suomi.fi-meddelandetjänsten kan du läsa beslutet från bilagorna nedan.

Vårdnadshavaren, som har gjort ansökan om plats inom småbarnspedagogik, kan godkänna eller avstå från platsen i Esbo stads elektroniska tjänst på adressen esbosmabarnspedagogik.fi. I tjänsten Suomi.fi kan du inte svara elektroniskt, men du kan skriva ut en svarsblankett.

Vänligen observera att du ska ge ditt svar till beslutet inom två veckor."""
        }

    override fun getFeeDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> """Espoon varhaiskasvatukseen liittyvät päätökset"""
            OfficialLanguage.SV -> """Beslut gällande Esbos småbarnspedagogik"""
        }

    override fun getFeeDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI ->
                """Kunnallisen varhaiskasvatuksen asiakasmaksut vaihtelevat perheen koon ja tulojen sekä varhaiskasvatusajan mukaan. Huoltajat saavat varhaiskasvatuksen maksuista kirjallisen päätöksen. Maksut laskutetaan palvelun antamisesta seuraavan kuukauden puolivälissä.\n\nVarhaiskasvatuksen asiakasmaksu on voimassa toistaiseksi ja perheellä on velvollisuus ilmoittaa, mikäli perheen tulot olennaisesti muuttuvat (+/- 10 %). Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.\n\nIn English:\n\nThe client fees for municipal early childhood education vary according to family size, income and the number of hours the child spends attending early childhood education. The City of Espoo sends the guardians a written decision on early childhood education fees. The fees are invoiced in the middle of the month following the provision of the service.\n\nThe early childhood education fee will remain in force until further notice. Your family has an obligation to notify the City of Espoo if the family’s income changes substantially (+/– 10%). As you are a user of Suomi.fi, you can find the decision in the attachments below."""
            OfficialLanguage.SV ->
                """Klientavgifterna för kommunal småbarnspedagogik varierar enligt familjens storlek och inkomster samt tiden för småbarnspedagogiken. Vårdnadshavarna får ett skriftligt beslut om avgifterna för småbarnspedagogik. Avgifterna faktureras i mitten av den månad som följer på den månad då servicen getts.\n\nKlientavgiften för småbarnspedagogik gäller tills vidare och familjen är skyldig att meddela om familjens inkomster väsentligt förändras (+/- 10 %). Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du läsa beslutet i bilagorna nedan."""
        }

    override fun getVoucherValueDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> "Espoon varhaiskasvatukseen liittyvät päätökset"
            OfficialLanguage.SV -> "Beslut gällande Esbos småbarnspedagogik"
        }

    override fun getVoucherValueDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI ->
                """
Kunnallisen varhaiskasvatuksen asiakasmaksut vaihtelevat perheen koon ja tulojen sekä varhaiskasvatusajan mukaan. Huoltajat saavat varhaiskasvatuksen maksuista kirjallisen päätöksen. Maksut laskutetaan palvelun antamisesta seuraavan kuukauden puolivälissä.

Varhaiskasvatuksen asiakasmaksu on voimassa toistaiseksi ja perheellä on velvollisuus ilmoittaa, mikäli perheen tulot olennaisesti muuttuvat (+/- 10 %). Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.

In English:

The client fees for municipal early childhood education vary according to family size, income and the number of hours the child spends attending early childhood education. The City of Espoo sends the guardians a written decision on early childhood education fees. The fees are invoiced in the middle of the month following the provision of the service.

The early childhood education fee will remain in force until further notice. Your family has an obligation to notify the City of Espoo if the family’s income changes substantially (+/– 10%). As you are a user of Suomi.fi, you can find the decision in the attachments below.
"""
            OfficialLanguage.SV ->
                """
Klientavgifterna för kommunal småbarnspedagogik varierar enligt familjens storlek och inkomster samt tiden för småbarnspedagogiken. Vårdnadshavarna får ett skriftligt beslut om avgifterna för småbarnspedagogik. Avgifterna faktureras i mitten av den månad som följer på den månad då servicen getts.

Klientavgiften för småbarnspedagogik gäller tills vidare och familjen är skyldig att meddela om familjens inkomster väsentligt förändras (+/- 10 %). Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du läsa beslutet i bilagorna nedan.
"""
        }

    override fun getAssistanceNeedDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> "Espoon varhaiskasvatukseen liittyvät päätökset"
            OfficialLanguage.SV -> "Beslut gällande Esbos småbarnspedagogik"
        }

    override fun getAssistanceNeedDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI ->
                """
            Lapsellenne on tehty päätös tuesta. Voit katsella päätöstä eVakassa.
    
            Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös myös luettavissa alla olevista liitteistä.
            
            In English:
            
            A decision for special support has been made for your child. You can view the decision on eVaka.
            
            As you are a user of Suomi.fi, you can also find the decision in the attachments below.
            """
                    .trimIndent()
            OfficialLanguage.SV ->
                """
            Beslut om behov har fattats för ditt barn. Du kan se beslutet i eVaka.
            
            Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du också läsa beslutet i bilagorna nedan.
            """
                    .trimIndent()
        }

    override fun getAssistanceNeedPreschoolDecisionHeader(lang: OfficialLanguage): String =
        getAssistanceNeedDecisionHeader(lang)

    override fun getAssistanceNeedPreschoolDecisionContent(lang: OfficialLanguage): String =
        getAssistanceNeedDecisionContent(lang)

    override fun getDefaultDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        when (lang) {
            OfficialLanguage.FI ->
                DecisionSendAddress(
                    street = "PL 3125",
                    postalCode = "02070",
                    postOffice = "Espoon kaupunki",
                    row1 = "Varhaiskasvatuksen palveluohjaus",
                    row2 = "PL 3125",
                    row3 = "02070 Espoon kaupunki"
                )
            OfficialLanguage.SV ->
                DecisionSendAddress(
                    street = "PB 32",
                    postalCode = "02070",
                    postOffice = "ESBO STAD",
                    row1 = "Svenska bildningstjänster",
                    row2 = "Småbarnspedagogik",
                    row3 = "PB 32, 02070 ESBO STAD"
                )
        }

    override fun getDefaultFinancialDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        when (lang) {
            OfficialLanguage.FI ->
                DecisionSendAddress(
                    "Espoon Kaupunki, Talousyksikkö, Varhaiskasvatuksen laskutus, PL 30",
                    "02070",
                    "Espoon kaupunki",
                    "PL 30",
                    "02070 Espoon kaupunki",
                    ""
                )
            OfficialLanguage.SV ->
                DecisionSendAddress(
                    "Esbo stad, ekonomieheten/småbarnspedagogik, PB 30",
                    "02070",
                    "Esbo",
                    "PB 30",
                    "02070 Esbo",
                    ""
                )
        }
}
