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
    fun `name change update info is received`() {
        val response: DvvUpdateInfoResponse = dvvUpdateInfoServiceClient.getUpdateInfo("100000000", listOf("010579-9999"))!!
        assertEquals(true, response.updateToken.length == 9)
        assertEquals(true, response.updateInfos.size == 1)
        assertEquals("HENKILON_NIMI", response.updateInfos[0].infoGroups[0].type)
    }
}
