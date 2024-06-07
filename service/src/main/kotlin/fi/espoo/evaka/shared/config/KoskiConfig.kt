// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.koski.KoskiClient
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KoskiConfig {
    @Bean
    fun koskiClient(
        koskiEnv: ObjectProvider<KoskiEnv>,
        ophEnv: ObjectProvider<OphEnv>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>
    ): KoskiClient? =
        koskiEnv.ifAvailable?.let { kEnv ->
            ophEnv.ifAvailable?.let { oEnv -> KoskiClient(kEnv, oEnv, asyncJobRunner) }
        }
}
