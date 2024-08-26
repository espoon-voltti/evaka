// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class DvvModificationsServiceClientIntegrationTest :
    DvvModificationsServiceIntegrationTestBase(resetDbBeforeEach = false) {

    @Test
    fun `get modification token for today`() {
        val response = dvvModificationsServiceClient.getFirstModificationToken(LocalDate.now())
        assertEquals(true, response!!.latestModificationToken > 0)
    }

    @Test
    fun `customizer is called in first modification token request`() {
        dvvModificationsServiceClient.getFirstModificationToken(LocalDate.now())
        verify(requestCustomizerMock).customize(any())
    }

    @Test
    fun `restricted info has been added`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("020180-999Y"))
        assertEquals(true, response.muutokset[0].tietoryhmat.size == 2)
        assertEquals("TURVAKIELTO", response.muutokset[0].tietoryhmat[0].tietoryhma)
        val restrictedInfo = response.muutokset[0].tietoryhmat[0] as RestrictedInfoDvvInfoGroup
        assertEquals(true, restrictedInfo.turvakieltoAktiivinen)
    }

    @Test
    fun `restricted info has been removed and address is provided`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("030180-999L"))
        assertEquals(true, response.muutokset[0].tietoryhmat.size == 3)
        assertEquals("TURVAKIELTO", response.muutokset[0].tietoryhmat[0].tietoryhma)

        val restrictedInfo = response.muutokset[0].tietoryhmat[0] as RestrictedInfoDvvInfoGroup
        assertEquals(false, restrictedInfo.turvakieltoAktiivinen)
        assertEquals("2030-01-01", restrictedInfo.turvaLoppuPv?.arvo)
    }

    @Test
    fun `person has died`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("010180-999A"))
        assertEquals("KUOLINPAIVA", response.muutokset[0].tietoryhmat[0].tietoryhma)
        val dead = response.muutokset[0].tietoryhmat[0] as DeathDvvInfoGroup
        assertEquals(true, dead.kuollut)
        assertEquals("2019-07-30", dead.kuolinpv?.arvo)
    }

    @Test
    fun `name change modification`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("nimenmuutos"))
        assertEquals(true, response.viimeisinKirjausavain.length == 9)
        assertEquals(true, response.muutokset.size == 1)
        assertEquals("HENKILON_NIMI", response.muutokset[0].tietoryhmat[0].tietoryhma)
    }

    @Test
    fun `guardian is now a sole guardian`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications(
                "100000000",
                listOf("yksinhuoltaja-muutos"),
            )
        assertEquals("HUOLLETTAVA_SUPPEA", response.muutokset[0].tietoryhmat[0].tietoryhma)
        assertEquals("010579-9999", response.muutokset[0].henkilotunnus)
    }

    @Test
    fun `custodian info`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("huoltaja"))
        assertEquals("HUOLTAJA_SUPPEA", response.muutokset[0].tietoryhmat[0].tietoryhma)
        val caretaker = response.muutokset[0].tietoryhmat[0] as CaretakerLimitedDvvInfoGroup
        assertEquals("010579-9999", caretaker.huoltaja.henkilotunnus)
        assertEquals("2020-09-08", caretaker.huoltosuhteenAlkupv?.arvo)
    }

    @Test
    fun `ssn change`() {
        val response: DvvModificationsResponse =
            dvvModificationsServiceClient.getModifications("100000000", listOf("010181-999K"))
        assertEquals("HENKILOTUNNUS_KORJAUS", response.muutokset[0].tietoryhmat[0].tietoryhma)
        val ssnModified = response.muutokset[0].tietoryhmat[0] as SsnDvvInfoGroup
        assertEquals("LISATTY", ssnModified.muutosattribuutti)
        assertEquals("AKTIIVI", ssnModified.voimassaolo)
        assertEquals("010281-999C", ssnModified.aktiivinenHenkilotunnus)
        assertEquals("010181-999K", ssnModified.edellisetHenkilotunnukset.get(0))
    }

    @Test
    fun `customizer is called in modifications request`() {
        dvvModificationsServiceClient.getModifications("100000000", listOf("020180-999Y"))
        verify(requestCustomizerMock).customize(any())
    }
}
