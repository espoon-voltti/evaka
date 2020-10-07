// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DvvUpdateInfoServiceIntegrationTest : FullApplicationTest() {

    @Test
    fun `get update token for today`() {
        val response = dvvUpdateInfoServiceClient.getFirstUpdateToken(LocalDate.now())
        assertEquals(true, response!!.latestUpdateToken > 0)
    }

    @Test
    fun `restricted info has been added`() {
        val response: DvvUpdateInfoResponse = dvvUpdateInfoServiceClient.getUpdateInfo("100000000", listOf("turvakielto-lisatty"))!!
        assertEquals(true, response.updateInfos[0].infoGroups.size == 2)
        assertEquals("TURVAKIELTO", response.updateInfos[0].infoGroups[0].type)
        val restrictedInfo = response.updateInfos[0].infoGroups[0] as RestrictedInfoDvvInfoGroup
        assertEquals(true, restrictedInfo.restrictedActive)
    }

    @Test
    fun `restricted info has been removed and address is provided`() {
        val response: DvvUpdateInfoResponse = dvvUpdateInfoServiceClient.getUpdateInfo("100000000", listOf("turvakielto-poistettu"))!!
        assertEquals(true, response.updateInfos[0].infoGroups.size == 2)
        assertEquals("TURVAKIELTO", response.updateInfos[0].infoGroups[0].type)

        val restrictedInfo = response.updateInfos[0].infoGroups[0] as RestrictedInfoDvvInfoGroup
        assertEquals(false, restrictedInfo.restrictedActive)
        assertEquals("2019-09-25", restrictedInfo.restrictedEndDate?.date)

        val address = response.updateInfos[0].infoGroups[1] as AddressDvvInfoGroup
        assertEquals("Gamlagatan", address.streetName!!.sv)
        assertEquals("Espoo", address.postOffice!!.fi)
    }

    @Test
    fun `person has died`() {
        val response: DvvUpdateInfoResponse = dvvUpdateInfoServiceClient.getUpdateInfo("100000000", listOf("kuollut"))!!
        assertEquals("KUOLINPAIVA", response.updateInfos[0].infoGroups[0].type)
        val dead = response.updateInfos[0].infoGroups[0] as DeathDvvInfoGroup
        assertEquals(true, dead.dead)
        assertEquals("2019-07-30", dead.dateOfDeath?.date)
    }

    @Test
    fun `name change update info is received`() {
        val response: DvvUpdateInfoResponse = dvvUpdateInfoServiceClient.getUpdateInfo("100000000", listOf("010579-9999"))!!
        assertEquals(true, response.updateToken.length == 9)
        assertEquals(true, response.updateInfos.size == 1)
        assertEquals("HENKILON_NIMI", response.updateInfos[0].infoGroups[0].type)
    }
}
