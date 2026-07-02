// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi

import evaka.YlojarviInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "ylojarvi_evaka"])
@Import(YlojarviInstance::class)
abstract class AbstractYlojarviIntegrationTest : AbstractIntegrationTest("ylojarvi") {
    @Autowired protected lateinit var properties: YlojarviProperties
}
