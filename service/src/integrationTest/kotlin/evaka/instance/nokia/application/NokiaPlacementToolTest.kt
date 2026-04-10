// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia.application

import evaka.core.EvakaEnv
import evaka.instance.nokia.AbstractNokiaIntegrationTest
import evaka.trevaka.assertPlacementToolServiceNeedOptionIdExists
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NokiaPlacementToolTest : AbstractNokiaIntegrationTest() {
    @Autowired private lateinit var evakaEnv: EvakaEnv

    @Test
    fun `placement tool service need option id exists`() {
        assertPlacementToolServiceNeedOptionIdExists(
            db,
            evakaEnv,
            "Täydentävä varhaiskasvatus 0–50 tuntia / kuukausi",
        )
    }
}
