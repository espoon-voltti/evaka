// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTempTokenProvider
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import java.net.URI
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired

abstract class VardaIntegrationTest(resetDbBeforeEach: Boolean) :
    FullApplicationTest(resetDbBeforeEach = resetDbBeforeEach) {
    protected lateinit var vardaTokenProvider: VardaTokenProvider
    protected lateinit var vardaClient: VardaClient

    @Autowired protected lateinit var vardaEnv: VardaEnv

    @Autowired protected lateinit var ophEnv: OphEnv

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        val vardaBaseUrl = URI.create("http://localhost:$httpPort/mock-integration/varda/api")
        val vardaEnv = VardaEnv.fromEnvironment(env).copy(url = vardaBaseUrl)
        vardaTokenProvider = VardaTempTokenProvider(jsonMapper, vardaEnv)
        vardaClient = VardaClient(vardaTokenProvider, jsonMapper, vardaEnv)
    }
}
