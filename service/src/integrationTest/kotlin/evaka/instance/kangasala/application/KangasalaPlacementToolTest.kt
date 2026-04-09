// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.application

import evaka.core.EvakaEnv
import evaka.instance.kangasala.AbstractKangasalaIntegrationTest
import evaka.trevaka.assertPlacementToolServiceNeedOptionIdExists
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KangasalaPlacementToolTest : AbstractKangasalaIntegrationTest() {
    @Autowired private lateinit var evakaEnv: EvakaEnv

    @Test
    fun `placement tool service need option id exists`() {
        assertPlacementToolServiceNeedOptionIdExists(
            db,
            evakaEnv,
            "Täydentävä varhaiskasvatus 86–120 tuntia / kuukausi",
        )
    }
}
