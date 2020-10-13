// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DvvModificationServiceClientIntegrationTest : DvvModificationServiceIntegrationTestBase() {

    @Test
    fun `get modification token for today`() {
        val response = dvvModificationsServiceClient.getFirstModificationToken(LocalDate.now())
        assertEquals(true, response!!.latestModificationToken > 0)
    }

    @Test
    fun `restricted info has been added`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("turvakielto-lisatty"))!!
        assertEquals(true, response.modifications[0].infoGroups.size == 2)
        assertEquals("TURVAKIELTO", response.modifications[0].infoGroups[0].type)
        val restrictedInfo = response.modifications[0].infoGroups[0] as RestrictedInfoDvvInfoGroup
        assertEquals(true, restrictedInfo.restrictedActive)
    }

    @Test
    fun `restricted info has been removed and address is provided`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("turvakielto-poistettu"))!!
        assertEquals(true, response.modifications[0].infoGroups.size == 2)
        assertEquals("TURVAKIELTO", response.modifications[0].infoGroups[0].type)

        val restrictedInfo = response.modifications[0].infoGroups[0] as RestrictedInfoDvvInfoGroup
        assertEquals(false, restrictedInfo.restrictedActive)
        assertEquals("2019-09-25", restrictedInfo.restrictedEndDate?.date)

        val address = response.modifications[0].infoGroups[1] as AddressDvvInfoGroup
        assertEquals("Gamlagatan", address.streetName!!.sv)
        assertEquals("Espoo", address.postOffice!!.fi)
    }

    @Test
    fun `person has died`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("010180-999A"))!!
        assertEquals("KUOLINPAIVA", response.modifications[0].infoGroups[0].type)
        val dead = response.modifications[0].infoGroups[0] as DeathDvvInfoGroup
        assertEquals(true, dead.dead)
        assertEquals("2019-07-30", dead.dateOfDeath?.date)
    }

    @Test
    fun `name change modification`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("nimenmuutos"))!!
        assertEquals(true, response.modificationToken.length == 9)
        assertEquals(true, response.modifications.size == 1)
        assertEquals("HENKILON_NIMI", response.modifications[0].infoGroups[0].type)
    }

    @Test
    fun `guardian is now a sole guardian`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("yksinhuoltaja-muutos"))!!
        assertEquals("HUOLLETTAVA_SUPPEA", response.modifications[0].infoGroups[0].type)
        val custodian = response.modifications[0].infoGroups[0] as CustodianLimitedDvvInfoGroup
        assertEquals("010118-9999", custodian.custodian.ssn)
        assertEquals("2020-09-08", custodian.caretakingStartDate?.date)
    }

    @Test
    fun `custodian info`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("huoltaja"))!!
        assertEquals("HUOLTAJA_SUPPEA", response.modifications[0].infoGroups[0].type)
        val caretaker = response.modifications[0].infoGroups[0] as CaretakerLimitedDvvInfoGroup
        assertEquals("010579-9999", caretaker.caretaker.ssn)
        assertEquals("2020-09-08", caretaker.caretakingStartDate?.date)
    }

    @Test
    fun `ssn change`() {
        val response: DvvModificationsResponse = dvvModificationsServiceClient.getModifications("100000000", listOf("hetu-muutettu"))!!
        assertEquals("HENKILOTUNNUS_KORJAUS", response.modifications[0].infoGroups[0].type)
        val ssnModified = response.modifications[0].infoGroups[0] as SsnDvvInfoGroup
        assertEquals("LISATTY", ssnModified.changeAttribute)
        assertEquals("AKTIIVI", ssnModified.activeState)
        assertEquals("010218-9999", ssnModified.activeSsn)
        assertEquals("010118-9999", ssnModified.previousSsns.get(0))
    }
}
