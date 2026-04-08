// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.KoskiEnv
import evaka.core.OphEnv
import evaka.core.koski.KoskiClient
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KoskiConfig {
    @Bean
    fun koskiClient(
        koskiEnv: ObjectProvider<KoskiEnv>,
        ophEnv: ObjectProvider<OphEnv>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
    ): KoskiClient? =
        koskiEnv.ifAvailable?.let { kEnv ->
            ophEnv.ifAvailable?.let { oEnv -> KoskiClient(kEnv, oEnv, asyncJobRunner) }
        }
}
