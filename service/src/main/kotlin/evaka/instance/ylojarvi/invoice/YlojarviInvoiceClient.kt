// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.invoice

import evaka.core.daycare.CareType
import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.domain.InvoiceRowDetailed
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient.SendResult
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.sftp.SftpClient
import evaka.instance.ylojarvi.InvoiceProperties
import evaka.instance.ylojarvi.invoice.config.findProduct
import evaka.trevaka.invoice.FixedLengthField
import evaka.trevaka.invoice.FixedLengthField.Cents
import evaka.trevaka.invoice.FixedLengthField.Date
import evaka.trevaka.invoice.FixedLengthField.Empty
import evaka.trevaka.invoice.FixedLengthField.Number
import evaka.trevaka.invoice.FixedLengthField.Text
import evaka.trevaka.invoice.InvoiceAddress
import evaka.trevaka.invoice.InvoicePerson
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min

private val FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy")
private val RESTRICTED_ADDRESS =
    InvoiceAddress("Varhaiskasvatus ja esiopetus, Kuruntie 14", "33470", "Ylöjärvi")

class YlojarviInvoiceClient(
    private val sftpClient: SftpClient,
    private val properties: InvoiceProperties,
) : InvoiceIntegrationClient {
    override fun send(now: HelsinkiDateTime, invoices: List<InvoiceDetailed>): SendResult {
        val (zeroSumInvoices, nonZeroSumInvoices) =
            invoices.partition { invoice -> invoice.totalPrice == 0 }
        val (nonSsnInvoices, ssnInvoices) =
            nonZeroSumInvoices.partition { invoice -> invoice.headOfFamily.ssn == null }
        if (ssnInvoices.isEmpty())
            return SendResult(succeeded = zeroSumInvoices, manuallySent = nonSsnInvoices)

        val data =
            FixedLengthField.render(
                ssnInvoices.flatMap { invoice -> toInvoiceData(invoice, now, properties) }
            )
        val timestamp = now.toLocalDateTime().format(FILENAME_DATE_TIME_FORMATTER)
        val filename =
            "${properties.sftp.prefix}${properties.municipalityCode}_${properties.invoiceType}_$timestamp.dat"
        data.byteInputStream(Charsets.ISO_8859_1).use { sftpClient.put(it, filename) }
        return SendResult(succeeded = zeroSumInvoices + ssnInvoices, manuallySent = nonSsnInvoices)
    }
}

// RD Myyntilaskut liittymäkuvaus MRP 2.3.pdf

internal fun toInvoiceData(
    invoice: InvoiceDetailed,
    now: HelsinkiDateTime,
    properties: InvoiceProperties,
): List<List<FixedLengthField>> {
    val header = invoice.headOfFamily.ssn!!
    val invoiceRows =
        invoice.rows
            .sortedBy { row -> row.child.firstName }
            .groupBy { row -> "${row.child.lastName} ${row.child.firstName}" }
            .flatMap { (childName, rows) ->
                listOf(
                    toDescriptionRow(header, childName),
                    *toInvoiceRows(header, rows).toTypedArray(),
                )
            }
    return listOfNotNull(
        toInvoiceHeaderRow(header, invoice, now, properties),
        invoice.codebtor?.let { toCodebtorRow(header, it) },
        *invoiceRows.toTypedArray(),
    )
}

internal fun toInvoiceRows(
    header: String,
    rows: List<InvoiceRowDetailed>,
): List<List<FixedLengthField>> = rows.flatMap { row ->
    listOfNotNull(
        toDescriptionRow(
            header,
            "${row.periodStart.format(DATE_FORMATTER)} - ${row.periodEnd.format(DATE_FORMATTER)}",
        ),
        toInvoiceRow(header, row),
        if (row.description.isNotEmpty()) toDescriptionRow(header, row.description) else null,
    )
}

/** 3.1 Laskun otsikkotietue */
fun toInvoiceHeaderRow(
    header: String,
    invoice: InvoiceDetailed,
    now: HelsinkiDateTime,
    properties: InvoiceProperties,
): List<FixedLengthField> {
    val person = InvoicePerson.of(invoice.headOfFamily, RESTRICTED_ADDRESS)
    return listOf(
        Text(
            header,
            11,
        ), // 1. Tunniste: Asiakkaan tunniste, hetu, y-tunnus tai muu (esim. jatkettu y-tunnus)
        Text("L", 1), // 2. Rivikoodi: Rivityyppi = 'L'
        Text(null, 2), // 3. Ei käytössä
        Text(person.name.take(50), 50), // 4. Asiakkaan nimi
        Text(
            if (person.name.length > 50) person.name.substring(50, min(100, person.name.length))
            else null,
            50,
        ), // 5. Asiakkaan nimi 2
        Text(person.address.streetAddress, 30), // 6. Lähiosoite: Asiakkaan lähiosoite
        Text(
            "${person.address.postalCode} ${person.address.postOffice}",
            30,
        ), // 7. Postiosoite: Asiakkaan postinro + postitoimipaikka
        Text(null, 15), // 8. Ei käytössä
        Text(
            "ä",
            1,
        ), // 9. Varattu (ei käsitellä liittymässä): Ääkkönen, jos ANSI-koodaus sen vaatii
        Empty(14), // Puuttuu dokumentaatiosta
        Text(
            null,
            30,
        ), // 10. Viitteenne: Viitteenne-tieto laskulla. Tiedon tulee siirtyä aineistossa.
        Text(null, 30), // 11. Ei käytössä
        Text(null, 15), // 12. Ei käytössä
        Number(null, 1), // 13. Ei käytössä
        Number(1, 1), // 14. Kielikoodi
        Text(null, 1), // 15. Karhuohje: Karhukoodi E=Ei kehoteta, muutoin tyhjä.
        Text(null, 1), // 16. Ei käytössä
        Number(null, 1), // 17. Ei käytössä
        Text("K", 1), // 18. Tulostustapa: Tulostetaan K/E
        Date(invoice.invoiceDate), // 19. Laskupvm: Laskutuspäivä, vvvvkkpp
        Date(invoice.dueDate), // 20. Eräpvm: Eräpäivä, vvvvkkpp
        Date(invoice.periodEnd), // 21. Kirjauspvm: Kirjauspäivä, vvvvkkpp
        Date(null), // 22. Tulostuspvm: Käytössä tulostustavalla E (ks. nro 18)
        Text(null, 10), // 23. Hyvityslaskunumero: Hyvitettävän laskun numero
        Empty(2), // Puuttuu dokumentaatiosta
        Text(null, 8), // 24. Laskunumero
        Text(null, 20), // 25. Viitenumero: Laskun viitenumero
        Text(null, 1), // 26. Ei käytössä
        Text("1000", 10), // 27. Kumppani: Asiakkaan kumppanikoodi (henkilöasiakkailla aina 1000)
        Text("EUR", 3), // 28. Valuutta: Valuutta, vain EUR
        Text(properties.invoiceType, 2), // 29. Laskulaji: Laskulajin koodi
        Text(null, 3), // 30. Ei käytössä
        Text(
            "Varhaiskasvatus ${now.minusMonths(1).let { YearMonth.of(it.year, it.month) }.format(YEAR_MONTH_FORMATTER)}",
            30,
        ), // 31. Viitteemme: Viitteemme-tieto laskulla. Tiedon tulee siirtyä aineistossa.
        Text(
            "K".takeIf { invoice.headOfFamily.restrictedDetailsEnabled },
            1,
        ), // 32. Turvakielto: Vain henkilöasiakkaat. K = Turvakieltomerkitty osoite (muulloin
        // tyhjä)
        Text(null, 80), // 33. Laskun kuvaus: Laskurivien yläpuolelle tuleva tieto
        // Text(null, 0), // 34. Ei käytössä
        Text(null, 30), // 35. Ei käytössä
        Text(null, 2), // 36. Maakoodi: Asiakkaan lähiosoitteen mukainen maa
        Empty(28), // Puuttuu dokumentaatiosta
        Text(
            null,
            11,
        ), // 37. Hetu / y-tunnus: Asiakkaan hetu / y-tunnus (virallinen) mikäli ei tuoda kohdassa 1
        Text(null, 6), // 38. Ei käytössä
        Text(null, 35), // 118. Ei käytössä
        Number(null, 8), // 119. Ei käytössä
        Text(null, 35), // 122. Ei käytössä
        Text(null, 10), // 123. Ei käytössä
        Text(null, 25), // 124. RF-viite: Viitenumero kansainvälissä muodossa
        Text(null, 20), // 150. Sopimusnumero: Vain yritysasiakkaat: Laskuun liittyvä sopimusnumero
        Text(
            null,
            70,
        ), // 151. TenderReference: Vain yritysasiakkaat: TenderReference (Kilpailutuksen numero)
        Text(
            null,
            35,
        ), // 152. Tiliöintiehdotus: Vain yritysasiakkaat: Tiliöintiehdotus ostajan kirjanpitoa
        // varten
        Text(null, 70), // 153. Tilausnumero: Vain yritysasiakkaat: Laskuun liittyvä tilausnumero
    )
}

/** 3.3 Yhteisvelallisen tiedot */
fun toCodebtorRow(header: String, codebtor: PersonDetailed): List<FixedLengthField> {
    val person = InvoicePerson.of(codebtor, RESTRICTED_ADDRESS)
    return listOf(
        Text(header, 11), // 129. Tunniste: Sama kuin laskun otsikkotietueella
        Text("Y", 1), // 130. Rivikoodi: Rivityyppi = 'Y'
        Text(codebtor.ssn, 11), // 131. Yhteisvelallisen tunniste: Yhteisvelallisen asiakastunniste
        Text(person.name, 50), // 132. Nimi: Yhteisvelallisen nimi
        Text(person.address.streetAddress, 30), // 133. Lähiosoite: Yhteisvelallisen lähiosoite
        Text(
            "${person.address.postalCode} ${person.address.postOffice}",
            30,
        ), // 134. Postiosoite: Yhteisvelallisen postinumero ja postitoimipaikka
        Text(null, 15), // 135. Ei käytössä
        Text(null, 15), // 136. Ei käytössä
        Text(null, 30), // 137. Ei käytössä
        Text(null, 30), // 138. Ei käytössä
        Text(null, 15), // 139. Ei käytössä
        Number(1, 1), // 140. Kielikoodi
        Text(null, 1), // 141. Ei käytössä
        Text(null, 10), // 142. Kumppani: Yhteisvelallisen kumppanikoodi
        Text(null, 1), // 143. Ei käytössä
        Text(null, 30), // 144. Ei käytössä
        Text(null, 2), // 145. Maakoodi: Yhteisvelallisen osoitteen mukainen maa
        Empty(28), // Puuttuu dokumentaatiosta
        Text(null, 50), // 146. Ei käytössä
    )
}

/** 3.5 Määrämuotoisen laskurivin kuvaus */
fun toInvoiceRow(header: String, row: InvoiceRowDetailed): List<FixedLengthField> =
    listOf(
        Text(header, 11), // 77. Tunniste: Sama kuin laskun otsikolla
        Number(1, 1), // 78. Rivikoodi: Rivityyppi=1
        Text(findProduct(row.product).nameFi, 40), // 79. Tuotenimi
        Text(null, 1), // 80. Hinta-etumerkki: Yksikköhinnan etumerkki
        Cents(
            abs(row.unitPrice),
            8,
            4,
        ), // 81. Yksikköhinta: Yksikköhinta (Sovittaessa käyttöön useampi desimaali, esim.
        // Vesikanta)
        Text("KPL", 3), // 82. Yksikkö: Yksikkö (isoilla kirjaimilla), esim. KPL, KG, H, jne.
        Text(
            "-".takeIf { row.unitPrice < 0 },
            1,
        ), // 83. Lkm-etumerkki: Laskutettavien yksiköiden lukumäärän etumerkki
        Number(row.amount, 8, 4), // 84. Lkm: Laskutettavien yksiköiden lukumäärä
        Text("00", 2), // 85. Alvkoodi: Alv-koodi
        Text(null, 60), // 86. Ei käytössä
        Text(null, 1), // 87. Brutto_netto: B=brutto, N=netto (oletus)
        Text(null, 60), // 88. Ei käytössä
        // 89. KP_tili_kr: Kredit-tiliöinti (Tiliöinnin tarkat positiot lueteltu alla)
        Text("3257", 4), // TILI: Tilinumero
        Text("300", 3), // ALV: Alv-koodi (Nro 85 tiedon mukainen)
        Text("1000", 4), // KUMP: Kumppanikoodi (tulee asiakastiedosta)
        Text(row.costCenter, 4), // KP: Kustannuspaikka
        Text(
            when {
                row.daycareType.contains(CareType.FAMILY) -> "483"
                row.daycareType.contains(CareType.CENTRE) -> "485"
                row.daycareType.contains(CareType.CLUB) -> "484"
                else -> throw Error("DaycareType ${row.daycareType} not supported")
            },
            4,
        ), // TOIM: Toimintokoodi
        Text(null, 2), // RY: Ryhmittelykoodi
        Text(null, 4), // PROJ: Projektikoodi
        Text(null, 4), // KOHDE: Kohdekoodi
        Text(null, 10), // TUOTE: Tuotekoodi (pituus max 10 mrk)
        Text(null, 10), // YL: Yleinen-koodi (pituus max 10 mrk)
        Text(null, 1), // ERIT: Erittelykoodi: 1 = ulkoinen
        Empty(9), // Puuttuu dokumentaatiosta
        Text(
            "-".takeIf { row.price < 0 },
            1,
        ), // 90A. Summa-etumerkki: Laskurivin rivisumman etumerkki
        Cents(
            abs(row.price),
            9,
            2,
        ), // 90B. Rivisumma: Laskurivin rivisumma (voi myös sisältää etumerkin pos. 265)
    )

/** 3.7 Tekstimuotoisen laskurivin kuvaus */
fun toDescriptionRow(header: String, description: String): List<FixedLengthField> =
    listOf(
        Text(header, 11), // 101. Tunniste: Sama kuin laskun otsikolla
        Number(3, 1), // 102. Rivikoodi: Rivityyppi = 3
        Text(description, 80), // 103. Teksti: Laskurivin teksti, voi olla myös blankkoa
        Text(null, 28), // 104. Ei käytössä
        Text(null, 10), // 105. Ei käytössä
    )
