// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.config.defaultJsonMapperBuilder
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import tools.jackson.module.kotlin.readValue

/** The fixtures are responses of the DVV sandbox. */
class DvvInfoGroupsTest {
    @Test
    fun `every VAKA group of the fixtures is read with its fields`() {
        // The fixtures are responses of the DVV sandbox, see
        // https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/usage/test-cases/henkilo
        val groups =
            resources("dvv/modifications/*.json").flatMap { json ->
                jsonMapper.readValue<DvvModificationsResponse>(json).muutokset.flatMap {
                    it.tietoryhmat
                }
            } +
                resources("dvv/basic-info/*.json").flatMap { json ->
                    jsonMapper.readValue<DvvBasicInfoResponse>(json).perustiedot.flatMap {
                        it.tietoryhmat
                    }
                }

        val vakaGroups =
            setOf(
                "HENKILOTUNNUS_KORJAUS",
                "HENKILON_NIMI",
                "AIDINKIELI",
                "KUOLINPAIVA",
                "TURVAKIELTO",
                "HUOLTAJA_SUPPEA",
                "HUOLLETTAVA_SUPPEA",
                "VAKINAINEN_KOTIMAINEN_OSOITE",
                "VAKINAINEN_ULKOMAINEN_OSOITE",
                "TILAPAINEN_KOTIMAINEN_OSOITE",
                "TILAPAINEN_ULKOMAINEN_OSOITE",
                "KOTIMAINEN_POSTIOSOITE",
                "KOTIMAINEN_YHTEYSOSOITE",
                "KOTIKUNTA",
                "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS",
            )
        assertEquals(vakaGroups, groups.map { it.tietoryhma }.filter { it in vakaGroups }.toSet())
        assertEquals(
            emptyList(),
            groups.filter { it is DefaultTietoryhma && it.tietoryhma in vakaGroups },
        )
    }

    @Test
    fun `a date with an unknown precision has no date`() {
        assertEquals(null, DvvDate("0001-01-01", "EI_TIEDOSSA").asLocalDate())
        assertEquals(LocalDate.of(1965, 10, 1), DvvDate("1965-10-01", "KUUKAUSI").asLocalDate())
    }

    @Test
    fun `a censored group has only the names of the censored fields`() {
        val censored =
            jsonMapper
                .readValue<DvvBasicInfoResponse>(
                    resources("dvv/basic-info/restricted-details.json").single()
                )
                .perustiedot
                .single()
                .tietoryhmat
                .filterIsInstance<KotimainenOsoite>()

        assertTrue(censored.isNotEmpty())
        assertTrue(censored.all { it.turvakiellonAlaisetKentat != null && it.katunimi == null })
    }

    private fun resources(pattern: String): List<ByteArray> =
        PathMatchingResourcePatternResolver().getResources("classpath:$pattern").map {
            it.inputStream.readAllBytes()
        }

    companion object {
        private val jsonMapper = defaultJsonMapperBuilder().build()
    }
}
