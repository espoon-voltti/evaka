// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance

import evaka.core.dataremoval.DataRetentionOverrides
import evaka.core.dataremoval.buildDataRetentionSchema
import evaka.instance.espoo.EspooConfig
import evaka.instance.hameenkyro.HameenkyroConfig
import evaka.instance.kangasala.KangasalaConfig
import evaka.instance.lempaala.LempaalaConfig
import evaka.instance.nokia.NokiaConfig
import evaka.instance.orivesi.OrivesiConfig
import evaka.instance.oulu.OuluConfig
import evaka.instance.pirkkala.PirkkalaConfig
import evaka.instance.tampere.TampereConfig
import evaka.instance.turku.TurkuConfig
import evaka.instance.vesilahti.VesilahtiConfig
import evaka.instance.ylojarvi.YlojarviConfig
import kotlin.test.Test
import kotlin.test.assertEquals

/** What the bean of each municipality in this repository gives */
val dataRetentionOverridesByMunicipality: Map<String, DataRetentionOverrides> =
    mapOf(
        "espoo" to EspooConfig().dataRetentionOverrides(),
        "oulu" to OuluConfig().dataRetentionOverrides(),
        "turku" to TurkuConfig().dataRetentionOverrides(),
        "tampere" to TampereConfig().dataRetentionOverrides(),
        "vesilahti" to VesilahtiConfig().dataRetentionOverrides(),
        "hameenkyro" to HameenkyroConfig().dataRetentionOverrides(),
        "ylojarvi" to YlojarviConfig().dataRetentionOverrides(),
        "pirkkala" to PirkkalaConfig().dataRetentionOverrides(),
        "nokia" to NokiaConfig().dataRetentionOverrides(),
        "kangasala" to KangasalaConfig().dataRetentionOverrides(),
        "lempaala" to LempaalaConfig().dataRetentionOverrides(),
        "orivesi" to OrivesiConfig().dataRetentionOverrides(),
    )

class DataRetentionOverridesTest {
    @Test
    fun `the data retention schema definition of every municipality is valid with its overrides`() {
        val errors = dataRetentionOverridesByMunicipality.flatMap { (municipality, overrides) ->
            listOf(false, true).mapNotNull { valueDecisionCapacityFactorEnabled ->
                runCatching {
                    buildDataRetentionSchema(valueDecisionCapacityFactorEnabled, overrides)
                }
                    .exceptionOrNull()
                    ?.let { "$municipality: ${it.message}" }
            }
        }
        assertEquals(emptyList(), errors)
    }
}
