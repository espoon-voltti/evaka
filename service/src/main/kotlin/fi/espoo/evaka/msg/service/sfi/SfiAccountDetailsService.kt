// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.sficlient.soap.Viranomainen
import fi.espoo.evaka.sficlient.soap.Yhteyshenkilo
import org.springframework.stereotype.Service

@Service
class SfiAccountDetailsService(private val sfiEnv: SfiEnv) {
    fun getAuthorityDetails(uniqueSfiMessageId: String): Viranomainen = Viranomainen().apply {
        // Should add trace parsing or create new trace, currently, there is none
        sanomaTunniste = uniqueSfiMessageId
        sanomaVarmenneNimi = sfiEnv.message.certificateCommonName
        viranomaisTunnus = sfiEnv.message.authorityIdentifier
        palveluTunnus = sfiEnv.message.serviceIdentifier
        sanomaVersio = sfiEnv.message.messageApiVersion
        yhteyshenkilo = createContactPerson()
    }

    fun createQueryWithPrintingDetails(): KyselyWS2A = KyselyWS2A().apply {
        isLahetaTulostukseen = sfiEnv.printing.enabled
        tulostustoimittaja = sfiEnv.printing.printingProvider
        isPaperi = sfiEnv.printing.forcePrintForElectronicUser
        laskutus = createBillingDetails()
    }

    private fun createBillingDetails() = KyselyWS2A.Laskutus().apply {
        tunniste = sfiEnv.printing.billingId
        salasana = when {
            sfiEnv.printing.billingPassword.isNullOrEmpty() -> null
            else -> sfiEnv.printing.billingPassword
        }
    }

    private fun createContactPerson() = Yhteyshenkilo().apply {
        nimi = sfiEnv.printing.contactPersonName
        matkapuhelin = sfiEnv.printing.contactPersonPhone
        sahkoposti = sfiEnv.printing.contactPersonEmail
    }
}
