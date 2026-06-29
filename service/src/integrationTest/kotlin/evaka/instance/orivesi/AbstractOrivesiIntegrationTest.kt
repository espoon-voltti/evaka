// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi

import evaka.OrivesiInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "orivesi_evaka"])
@Import(OrivesiInstance::class)
abstract class AbstractOrivesiIntegrationTest : AbstractIntegrationTest("orivesi") {
    @Autowired protected lateinit var properties: OrivesiProperties
}
