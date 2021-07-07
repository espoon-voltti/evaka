// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.properties.SfiMessageProperties
import fi.espoo.evaka.msg.properties.SfiPrintingProperties
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A.Laskutus
import fi.espoo.evaka.msg.sficlient.soap.Viranomainen
import org.springframework.stereotype.Service

@Service
class SfiAccountDetailsService(
    private val messageProperties: SfiMessageProperties,
    private val printingProperties: SfiPrintingProperties
) {

    fun getAuthorityDetails(uniqueSfiMessageId: String): Viranomainen = Viranomainen().apply {
        // Should add trace parsing or create new trace, currently, there is none
        sanomaTunniste = uniqueSfiMessageId
        sanomaVarmenneNimi = messageProperties.certificateCommonName
        viranomaisTunnus = messageProperties.authorityIdentifier
        palveluTunnus = messageProperties.serviceIdentifier
        sanomaVersio = messageProperties.messageApiVersion
    }

    fun createQueryWithPrintingDetails(): KyselyWS2A = KyselyWS2A().apply {
        isLahetaTulostukseen = printingProperties.enablePrinting
        tulostustoimittaja = printingProperties.printingProvider
        isPaperi = printingProperties.forcePrintForElectronicUser
        laskutus = createBillingDetails()
    }

    private fun createBillingDetails() = Laskutus().apply {
        tunniste = printingProperties.billingId
        salasana = when {
            printingProperties.billingPassword.isNullOrEmpty() -> null
            else -> printingProperties.billingPassword
        }
    }
}
