// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.algorithms.Algorithm
import fi.espoo.evaka.MessageEnv
import fi.espoo.evaka.shared.message.EvakaMessageClient
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EvakaMessageConfig {
    @Bean
    fun evakaMessageClient(env: ObjectProvider<MessageEnv>, algorithm: Algorithm): IEvakaMessageClient = env.ifAvailable?.let {
        EvakaMessageClient(algorithm, defaultObjectMapper(), it)
    } ?: MockEvakaMessageClient()
}
