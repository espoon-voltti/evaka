// SPDX-FileCopyrightText: 2021-2023 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.TampereInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "tampere_evaka"])
@Import(TampereInstance::class)
abstract class AbstractTampereIntegrationTest : AbstractIntegrationTest("tampere") {
    @Autowired protected lateinit var properties: TampereProperties
}
