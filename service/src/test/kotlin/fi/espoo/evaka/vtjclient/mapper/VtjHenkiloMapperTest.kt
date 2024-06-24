// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.Aidinkieli
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.Huollettava
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.Turvakielto
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VtjHenkiloMapperTest {
    @Test
    fun `valid person should be mapped to person details`() {
        val value =
            Henkilo().apply {
                nykyisetEtunimet = NykyisetEtunimet().apply { etunimet = "Heikki Jaska" }
                nykyinenSukunimi = NykyinenSukunimi().apply { sukunimi = "Heikinheimo" }
                henkilotunnus = Henkilo.Henkilotunnus().apply { value = "33221A312Z" }
                kansalaisuus.clear()
                kansalaisuus.add(
                    Kansalaisuus().apply {
                        kansalaisuusS = "Suomi"
                        kansalaisuuskoodi3 = "247"
                    }
                )
                huollettava.clear()
                huollettava.add(
                    Huollettava().apply {
                        henkilotunnus = "020202A0202"
                        nykyisetEtunimet =
                            Huollettava.NykyisetEtunimet().apply { etunimet = "Lapsi" }
                        nykyinenSukunimi =
                            Huollettava.NykyinenSukunimi().apply { sukunimi = "Lapsonen" }
                    }
                )
                vakinainenKotimainenLahiosoite =
                    VakinainenKotimainenLahiosoite().apply {
                        lahiosoiteS = "Reitti 1"
                        postinumero = "232323"
                        postitoimipaikkaS = "Espoo"
                        lahiosoiteR = "Vägen 1"
                        postitoimipaikkaR = "Esbo"
                    }
                vakinainenAsuinpaikka =
                    Henkilo.VakinainenAsuinpaikka().apply {
                        asuinpaikantunnus = "testiasuinpaikkatunnus"
                    }
                aidinkieli =
                    Aidinkieli().apply {
                        kieliS = "ruotsi"
                        kielikoodi = "sv"
                    }

                turvakielto =
                    Turvakielto().apply {
                        turvakieltoTieto = "1"
                        turvakieltoPaattymispvm = "20191110"
                    }
                kuolintiedot =
                    Henkilo.Kuolintiedot().apply {
                        kuollut = "1"
                        kuolinpvm = "20191231"
                    }
            }

        val result = value.mapToVtjPerson()
        assertThat(result).isNotNull
        assertThat(result.firstNames).isEqualTo(value.nykyisetEtunimet.etunimet)
        assertThat(result.lastName).isEqualTo(value.nykyinenSukunimi.sukunimi)

        assertThat(result.dependants).size().isEqualTo(1)
        assertThat(result.dependants).allMatch {
            it.socialSecurityNumber == value.huollettava[0].henkilotunnus
        }

        assertThat(result.address!!.streetAddress)
            .isEqualTo(value.vakinainenKotimainenLahiosoite.lahiosoiteS)
        assertThat(result.address!!.postOffice)
            .isEqualTo(value.vakinainenKotimainenLahiosoite.postitoimipaikkaS)
        assertThat(result.address!!.streetAddressSe)
            .isEqualTo(value.vakinainenKotimainenLahiosoite.lahiosoiteR)
        assertThat(result.address!!.postOfficeSe)
            .isEqualTo(value.vakinainenKotimainenLahiosoite.postitoimipaikkaR)
        assertThat(result.address!!.postalCode)
            .isEqualTo(value.vakinainenKotimainenLahiosoite.postinumero)
        assertThat(result.residenceCode).isEqualTo("testiasuinpaikkatunnus")

        assertThat(result.nativeLanguage!!.languageName).isEqualTo(value.aidinkieli.kieliS)

        val date = result.restrictedDetails!!.endDate!!
        // NB: this comparison is dependant on the month and day having 2 digits
        assertThat("${date.year}${date.monthValue}${date.dayOfMonth}")
            .isEqualTo(value.turvakielto.turvakieltoPaattymispvm)
    }

    @Test
    fun `valid dependants should be mapped to person's children`() {
        val dependant =
            Huollettava().apply {
                henkilotunnus = "010104A123E"
                nykyisetEtunimet =
                    Huollettava.NykyisetEtunimet().apply { etunimet = "Karlo Jaskari Jooseppi" }
                nykyinenSukunimi =
                    Huollettava.NykyinenSukunimi().apply { sukunimi = "Joosepinpoika" }
            }

        val value =
            Henkilo().apply {
                huollettava.clear()
                huollettava.add(dependant)
            }

        val result = value.mapDependants()
        assertThat(result).size().isEqualTo(1)

        val child = result[0]
        assertThat(child.socialSecurityNumber).isEqualTo(dependant.henkilotunnus)
        assertThat(child.firstNames).isEqualTo(dependant.nykyisetEtunimet.etunimet)
        assertThat(child.lastName).isEqualTo(dependant.nykyinenSukunimi.sukunimi)
    }

    @Test
    fun `empty dependants should be mapped to person's children`() {
        val value = Henkilo().apply { huollettava.clear() }

        val result = value.mapDependants()
        assertThat(result).isEmpty()
    }

    @Test
    fun `valid local permanent address should be mapped to person's address`() {
        val addressFi = "Vaakonkuja 3B"
        val postOfficeFi = "Hanko"
        val postalCode = "12345"
        val addressSe = "Vägen 1"
        val postOfficeSe = "Staden"

        val value =
            VakinainenKotimainenLahiosoite().apply {
                lahiosoiteS = addressFi
                postinumero = postalCode
                postitoimipaikkaS = postOfficeFi
                lahiosoiteR = addressSe
                postitoimipaikkaR = postOfficeSe
            }

        val result = parseRegularAddress(value)
        assertThat(result).isNotNull

        assertThat(result!!.streetAddress).isEqualTo(addressFi)
        assertThat(result.postalCode).isEqualTo(postalCode)
        assertThat(result.postOffice).isEqualTo(postOfficeFi)
        assertThat(result.streetAddressSe).isEqualTo(addressSe)
        assertThat(result.postOfficeSe).isEqualTo(postOfficeSe)
    }

    @Test
    fun `empty local permanent address should be mapped to person's address`() {
        val value =
            VakinainenKotimainenLahiosoite().apply {
                lahiosoiteS = ""
                postinumero = ""
                postitoimipaikkaS = ""
                lahiosoiteR = ""
                postitoimipaikkaR = ""
            }

        val result = parseRegularAddress(value)
        assertThat(result).isNull()
    }

    @Test
    fun `parseAddress uses mailAddress is available`() {
        val mailAddress =
            Henkilo.KotimainenPostiosoite().apply {
                postiosoiteS = "postiosoiteS"
                postinumero = "postinumero"
                postitoimipaikkaS = "postitoimipaikkaS"
                postiosoiteR = "postiosoiteR"
                postitoimipaikkaR = "postitoimipaikkaR"
                postiosoiteAlkupvm = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                postiosoiteLoppupvm =
                    LocalDate.now().plusDays(10).format(DateTimeFormatter.BASIC_ISO_DATE)
            }

        val temporaryAddress =
            Henkilo.TilapainenKotimainenLahiosoite().apply {
                lahiosoiteS = ""
                postinumero = ""
                postitoimipaikkaS = ""
                lahiosoiteR = ""
                postitoimipaikkaR = ""
            }

        val address =
            VakinainenKotimainenLahiosoite().apply {
                lahiosoiteS = "lahiosoiteS"
                postinumero = "postinumero"
                postitoimipaikkaS = "postitoimipaikkaS"
                lahiosoiteR = "lahiosoiteR"
                postitoimipaikkaR = "postitoimipaikkaR"
            }

        assertThat(mailAddress.postiosoiteS).isEqualTo("postiosoiteS")

        val result = parseAddress(listOf(mailAddress), listOf(temporaryAddress), address)
        assertThat(result?.streetAddress).isEqualTo(mailAddress.postiosoiteS)
        assertThat(result?.postalCode).isEqualTo(mailAddress.postinumero)
        assertThat(result?.postOffice).isEqualTo(mailAddress.postitoimipaikkaS)
        assertThat(result?.streetAddressSe).isEqualTo(mailAddress.postiosoiteR)
        assertThat(result?.postOfficeSe).isEqualTo(mailAddress.postitoimipaikkaR)
    }

    @Test
    fun `parseAddress uses temporaryAddress if mailAddress is unavailable`() {
        val mailAddress =
            Henkilo.KotimainenPostiosoite().apply {
                postiosoiteS = ""
                postinumero = ""
                postitoimipaikkaS = ""
                postiosoiteR = ""
                postitoimipaikkaR = ""
            }

        val temporaryAddress =
            Henkilo.TilapainenKotimainenLahiosoite().apply {
                lahiosoiteS = "tilap-lahiosoiteS"
                postinumero = "tilap-postinumero"
                postitoimipaikkaS = "tilap-postitoimipaikkaS"
                lahiosoiteR = "tilap-lahiosoiteR"
                postitoimipaikkaR = "tilap-postitoimipaikkaR"
                asuminenAlkupvm = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                asuminenLoppupvm =
                    LocalDate.now().plusDays(10).format(DateTimeFormatter.BASIC_ISO_DATE)
            }

        val address =
            VakinainenKotimainenLahiosoite().apply {
                lahiosoiteS = "lahiosoiteS"
                postinumero = "postinumero"
                postitoimipaikkaS = "postitoimipaikkaS"
                lahiosoiteR = "lahiosoiteR"
                postitoimipaikkaR = "postitoimipaikkaR"
            }

        val result = parseAddress(listOf(mailAddress), listOf(temporaryAddress), address)
        assertThat(result?.streetAddress).isEqualTo(temporaryAddress.lahiosoiteS)
        assertThat(result?.postalCode).isEqualTo(temporaryAddress.postinumero)
        assertThat(result?.postOffice).isEqualTo(temporaryAddress.postitoimipaikkaS)
        assertThat(result?.streetAddressSe).isEqualTo(temporaryAddress.lahiosoiteR)
        assertThat(result?.postOfficeSe).isEqualTo(temporaryAddress.postitoimipaikkaR)
    }

    @Test
    fun `valid nationality should be mapped to a list of Nationalities`() {
        val value =
            Henkilo().apply {
                kansalaisuus.clear()
                kansalaisuus.add(
                    Kansalaisuus().apply {
                        kansalaisuusS = "Suomi"
                        kansalaisuuskoodi3 = "247"
                    }
                )
            }
        val result: List<Nationality> = value.mapNationalities()

        assertThat(result).size().isEqualTo(1)

        val nationality = result[0]
        assertThat(nationality.countryName).isEqualTo("Suomi")
        assertThat(nationality.countryCode).isEqualTo("247")
    }

    @Test
    fun `empty nationality list should be mapped to an empty list of Nationalities`() {
        val value = Henkilo().apply { kansalaisuus.clear() }
        val result: List<Nationality> = value.mapNationalities()

        assertThat(result).size().isEqualTo(0)
    }

    @Test
    fun `multiple nationalities should be mapped to a Nationality`() {
        val value =
            Henkilo().apply {
                kansalaisuus.clear()
                kansalaisuus.add(
                    Kansalaisuus().apply {
                        kansalaisuusS = "Suomi"
                        kansalaisuuskoodi3 = "247"
                    }
                )
                kansalaisuus.add(
                    Kansalaisuus().apply {
                        kansalaisuusS = "Ruotsi"
                        kansalaisuuskoodi3 = "123"
                    }
                )
            }
        val result: List<Nationality> = value.mapNationalities()

        assertThat(result).size().isEqualTo(2)

        val nationality1 = result[0]
        assertThat(nationality1.countryName).isEqualTo("Suomi")
        assertThat(nationality1.countryCode).isEqualTo("247")

        val nationality2 = result[1]
        assertThat(nationality2.countryName).isEqualTo("Ruotsi")
        assertThat(nationality2.countryCode).isEqualTo("123")
    }

    @Test
    fun `valid native language should be mapped to a NativeLanguage`() {
        val value =
            Henkilo().apply {
                aidinkieli =
                    Aidinkieli().apply {
                        kieliS = "suomi"
                        kielikoodi = "fi"
                    }
            }
        val result: NativeLanguage? = value.mapNativeLanguage()

        assertThat(result!!.code).isEqualTo("fi")
        assertThat(result.languageName).isEqualTo("suomi")
    }

    @Test
    fun `empty native language fields should be mapped to an empty NativeLanguage`() {
        val value =
            Henkilo().apply {
                aidinkieli =
                    Aidinkieli().apply {
                        kieliS = ""
                        kielikoodi = ""
                    }
            }
        val result: NativeLanguage? = value.mapNativeLanguage()

        assertThat(result!!.code).isEqualTo("")
        assertThat(result.languageName).isEqualTo("")
    }

    @Test
    fun `null native language fields should be mapped to an empty NativeLanguage`() {
        val value = Henkilo().apply { aidinkieli = Aidinkieli() }
        val result: NativeLanguage? = value.mapNativeLanguage()

        assertThat(result!!.code).isEqualTo("")
        assertThat(result.languageName).isEqualTo("")
    }

    @Test
    fun `valid Turvakielto end date should be mapped to a temporary RestrictedDetails`() {
        val value =
            Turvakielto().apply {
                turvakieltoTieto = "1"
                turvakieltoPaattymispvm = "20200630"
            }
        val result: RestrictedDetails = value.mapToRestrictedDetails()

        assertThat(result.enabled).isTrue

        val endDate = result.endDate
        assertThat(endDate!!.year).isEqualTo(2020)
        assertThat(endDate.month).isEqualTo(Month.JUNE)
        assertThat(endDate.dayOfMonth).isEqualTo(30)
    }

    @Test
    fun `empty Turvakielto end date should be mapped to a permanent RestrictedDetails`() {
        val value = Turvakielto().apply { turvakieltoTieto = "1" }
        val result: RestrictedDetails = value.mapToRestrictedDetails()

        assertThat(result.enabled).isTrue
        assertThat(result.endDate).isNull()
    }

    @Test
    fun `empty Turvakielto should be mapped to a disabled RestrictedDetails`() {
        val value = Turvakielto()
        val result: RestrictedDetails = value.mapToRestrictedDetails()

        assertThat(result.enabled).isFalse
        assertThat(result.endDate).isNull()
    }

    @Test
    fun `parseLocalDateFromString should parse date in format vvvvkkpp`() {
        val dateString = "20080229"
        val result = parseLocalDateFromString(dateString)

        assertThat(result!!.year).isEqualTo(2008)
        assertThat(result.month).isEqualTo(Month.FEBRUARY)
        assertThat(result.dayOfMonth).isEqualTo(29)
    }

    @Test
    fun `parseLocalDateFromString should parse empty date`() {
        val dateString = ""
        val result = parseLocalDateFromString(dateString)

        assertThat(result).isNull()
    }

    @Test
    fun `parseLocalDateFromString should parse null date`() {
        val result = parseLocalDateFromString(null)

        assertThat(result).isNull()
    }
}
