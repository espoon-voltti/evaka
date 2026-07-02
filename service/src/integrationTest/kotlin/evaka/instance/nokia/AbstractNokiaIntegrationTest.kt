// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia

import evaka.NokiaInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "nokia_evaka"])
@Import(NokiaInstance::class)
abstract class AbstractNokiaIntegrationTest : AbstractIntegrationTest("nokia") {
    @Autowired protected lateinit var properties: NokiaProperties
}
