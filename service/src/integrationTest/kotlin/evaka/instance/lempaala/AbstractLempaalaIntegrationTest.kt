// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala

import evaka.LempaalaInstance
import evaka.trevaka.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "lempaala_evaka"])
@Import(LempaalaInstance::class)
abstract class AbstractLempaalaIntegrationTest : AbstractIntegrationTest("lempaala") {
    @Autowired protected lateinit var properties: LempaalaProperties
}
