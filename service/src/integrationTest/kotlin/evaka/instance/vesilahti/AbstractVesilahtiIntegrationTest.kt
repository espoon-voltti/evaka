// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti

import evaka.VesilahtiInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "vesilahti_evaka"])
@Import(VesilahtiInstance::class)
abstract class AbstractVesilahtiIntegrationTest : AbstractIntegrationTest("vesilahti") {
    @Autowired protected lateinit var properties: VesilahtiProperties
}
