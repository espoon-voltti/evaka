// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.mapper

import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.sficlient.soap.ArrayOfKohdeWS2A
import fi.espoo.evaka.msg.sficlient.soap.ArrayOfTiedosto
import fi.espoo.evaka.msg.sficlient.soap.Asiakas
import fi.espoo.evaka.msg.sficlient.soap.KohdeWS2A
import fi.espoo.evaka.msg.sficlient.soap.Osoite
import fi.espoo.evaka.msg.sficlient.soap.Tiedosto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

@Service
class SfiMapper : ISfiMapper {
    override fun mapToSoapTargets(metadata: MessageMetadata): ArrayOfKohdeWS2A = metadata.message.toMessageTargets()
}

fun LocalDate.toXmlGregorian(): XMLGregorianCalendar =
    GregorianCalendar.from(atStartOfDay(ZoneId.of("Europe/Helsinki")))
        .let {
            DatatypeFactory.newInstance().newXMLGregorianCalendar(it)
        }

fun ISfiClientService.MessageDetails.toMessageTargets() = ArrayOfKohdeWS2A().apply {
    KohdeWS2A().also {
        it.viranomaisTunniste = uniqueCaseIdentifier
        it.nimeke = header
        it.kuvausTeksti = content
        it.lahetysPvm = sendDate.toXmlGregorian()
        it.lahettajaNimi = senderName
        it.tiedostot = pdfDetails.toAttachments()
        it.asiakas.add(recipient.toAsiakas())
        emailNotification?.let { email ->
            it.emailLisatietoOtsikko = email.header
            it.emailLisatietoSisalto = email.message
        }
    }.let(kohde::add)
}

fun ISfiClientService.PdfDetails.toAttachments() = ArrayOfTiedosto().apply {
    Tiedosto()
        .also {
            it.tiedostonKuvaus = fileDescription
            it.tiedostoMuoto = fileType
            it.tiedostoNimi = fileName
            it.tiedostoSisalto = content
        }.let(tiedosto::add)
}

fun ISfiClientService.Recipient.toAsiakas() =
    Asiakas().also {
        it.asiakasTunnus = ssn
        it.tunnusTyyppi = "SSN"
        it.osoite = Osoite()
            .also { o ->
                o.nimi = name
                o.lahiosoite = address.streetAddress
                o.postinumero = address.postalCode
                o.postitoimipaikka = address.postOffice
                o.maa = "FI"
            }
    }
