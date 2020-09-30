// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.properties.SfiMessageProperties
import fi.espoo.evaka.msg.properties.SfiPrintingProperties
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SfiAccountDetailsServiceTest {

    @Test
    fun `printing general properties should be reflected in the query with printing details`() {

        val printProps = SfiPrintingProperties(
            enablePrinting = true,
            forcePrintForElectronicUser = true,
            printingProvider = "Prima",
            billingId = "123312123"
        )

        val testedService = SfiAccountDetailsService(
            messageProperties = SfiMessageProperties(),
            printingProperties = printProps
        )

        val expected = KyselyWS2A().apply {
            isPaperi = printProps.forcePrintForElectronicUser
            isLahetaTulostukseen = printProps.enablePrinting
            tulostustoimittaja = printProps.printingProvider
        }

        val response = testedService.createQueryWithPrintingDetails()

        assertThat(response.isPaperi).isEqualTo(expected.isPaperi)
        assertThat(response.isLahetaTulostukseen).isEqualTo(expected.isLahetaTulostukseen)
        assertThat(response.tulostustoimittaja).isEqualTo(expected.tulostustoimittaja)
    }

    @Test
    fun `print billing properties should be reflected in the query with printing details`() {

        val printProps = SfiPrintingProperties(
            enablePrinting = false,
            forcePrintForElectronicUser = false,
            printingProvider = "Prima-Book",
            billingId = "123312123",
            billingPassword = "trustno1"
        )

        val testedService = SfiAccountDetailsService(
            messageProperties = SfiMessageProperties(),
            printingProperties = printProps
        )

        val expected = KyselyWS2A().apply {
            isPaperi = printProps.forcePrintForElectronicUser
            isLahetaTulostukseen = printProps.enablePrinting
            tulostustoimittaja = printProps.printingProvider
            laskutus = KyselyWS2A.Laskutus().apply {
                tunniste = printProps.billingId
                salasana = printProps.billingPassword
            }
        }

        val response = testedService.createQueryWithPrintingDetails()

        // these were already asserted in the previous test but expected values are opposite
        assertThat(response.isPaperi).isEqualTo(expected.isPaperi)
        assertThat(response.isLahetaTulostukseen).isEqualTo(expected.isLahetaTulostukseen)
        assertThat(response.tulostustoimittaja).isEqualTo(expected.tulostustoimittaja)

        assertThat(response.laskutus.tunniste).isEqualToIgnoringCase(expected.laskutus.tunniste)
        assertThat(response.laskutus.salasana).isEqualTo(expected.laskutus.salasana)
    }

    @Test
    fun `empty billing password should be translated to a null value`() {

        val printProps = SfiPrintingProperties(
            enablePrinting = true,
            forcePrintForElectronicUser = true,
            printingProvider = "Prima",
            billingId = "123312123",
            billingPassword = ""
        )

        val testedService = SfiAccountDetailsService(
            messageProperties = SfiMessageProperties(),
            printingProperties = printProps
        )

        val expected = KyselyWS2A().apply {
            isPaperi = printProps.forcePrintForElectronicUser
            isLahetaTulostukseen = printProps.enablePrinting
            tulostustoimittaja = printProps.printingProvider
            laskutus = KyselyWS2A.Laskutus().apply {
                tunniste = printProps.billingId
                salasana = null
            }
        }

        val response = testedService.createQueryWithPrintingDetails()

        assertThat(response.isPaperi).isEqualTo(expected.isPaperi)
        assertThat(response.isLahetaTulostukseen).isEqualTo(expected.isLahetaTulostukseen)
        assertThat(response.tulostustoimittaja).isEqualTo(expected.tulostustoimittaja)

        assertThat(response.laskutus.tunniste).isEqualToIgnoringCase(expected.laskutus.tunniste)
        assertThat(response.laskutus.salasana).isEqualTo(expected.laskutus.salasana)
    }
}
