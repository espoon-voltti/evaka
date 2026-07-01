// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.hameenkyro

import evaka.HameenkyroInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "hameenkyro_evaka"])
@Import(HameenkyroInstance::class)
abstract class AbstractHameenkyroIntegrationTest : AbstractIntegrationTest("hameenkyro") {
    @Autowired protected lateinit var properties: HameenkyroProperties
}
