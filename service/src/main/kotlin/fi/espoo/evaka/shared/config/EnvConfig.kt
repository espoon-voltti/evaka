// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.DatabaseEnv
import fi.espoo.evaka.DvvModificationsEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.JwtEnv
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.MessageEnv
import fi.espoo.evaka.RedisEnv
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.VtjEnv
import fi.espoo.evaka.VtjXroadEnv
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

    @Bean
    fun vardaEnv(env: Environment): VardaEnv = VardaEnv.fromEnvironment(env)

    @Bean
    fun databaseEnv(env: Environment): DatabaseEnv = DatabaseEnv.fromEnvironment(env)

    @Bean
    fun redisEnv(env: Environment): RedisEnv = RedisEnv.fromEnvironment(env)

    @Bean
    fun dvvModificationsEnv(env: Environment): DvvModificationsEnv = DvvModificationsEnv.fromEnvironment(env)

    @Bean
    fun jwtEnv(env: Environment): JwtEnv = JwtEnv.fromEnvironment(env)

    @Bean
    fun messageEnv(evakaEnv: EvakaEnv, env: Environment): MessageEnv? = when (evakaEnv.messageEnabled) {
        true -> MessageEnv.fromEnvironment(env)
        false -> null
    }

    @Bean
    fun scheduledJobsEnv(env: Environment): ScheduledJobsEnv = ScheduledJobsEnv.fromEnvironment(env)

    @Bean
    fun vtjEnv(evakaEnv: EvakaEnv, env: Environment): VtjEnv = VtjEnv.fromEnvironment(env)

    @Bean
    fun vtjXroadEnv(evakaEnv: EvakaEnv, env: Environment): VtjXroadEnv = VtjXroadEnv.fromEnvironment(env)
}
