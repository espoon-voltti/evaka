// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.application

import evaka.core.EvakaEnv
import evaka.instance.ylojarvi.AbstractYlojarviIntegrationTest
import evaka.trevaka.assertPlacementToolServiceNeedOptionIdExists
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class YlojarviPlacementToolTest : AbstractYlojarviIntegrationTest() {
    @Autowired private lateinit var evakaEnv: EvakaEnv

    @Test
    fun `placement tool service need option id exists`() {
        assertPlacementToolServiceNeedOptionIdExists(
            db,
            evakaEnv,
            "Täydentävä varhaiskasvatus 51–85 tuntia / kuukausi",
        )
    }
}
