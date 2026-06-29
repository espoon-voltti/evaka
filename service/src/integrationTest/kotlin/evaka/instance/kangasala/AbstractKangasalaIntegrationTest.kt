// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala

import evaka.KangasalaInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "kangasala_evaka"])
@Import(KangasalaInstance::class)
abstract class AbstractKangasalaIntegrationTest : AbstractIntegrationTest("kangasala") {
    @Autowired protected lateinit var properties: KangasalaProperties
}
