// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.KoskiEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment

@Configuration
@Lazy
class EnvConfig {
    @Bean
    fun evakaEnv(env: Environment) = EvakaEnv.fromEnvironment(env)

    @Bean
    fun koskiEnv(evakaEnv: EvakaEnv, env: Environment): KoskiEnv? = when (evakaEnv.koskiEnabled) {
        true -> KoskiEnv.fromEnvironment(env)
        false -> null
    }

    @Bean
    fun bucketEnv(env: Environment): BucketEnv = BucketEnv.fromEnvironment(env)

    @Bean
    fun emailEnv(env: Environment): EmailEnv = EmailEnv.fromEnvironment(env)
}
