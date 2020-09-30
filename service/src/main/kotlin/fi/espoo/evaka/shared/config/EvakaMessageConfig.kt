// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.algorithms.Algorithm
import fi.espoo.evaka.shared.message.EvakaMessageClient
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class EvakaMessageConfig {
    @Bean
    fun evakaMessageClient(env: Environment, algorithm: Algorithm): IEvakaMessageClient =
        when (env.getProperty("fi.espoo.evaka.message.enabled", Boolean::class.java, true)) {
            true -> EvakaMessageClient(
                env = env,
                algorithm = algorithm,
                objectMapper = defaultObjectMapper()
            )
            false -> MockEvakaMessageClient()
        }
}
