// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.CitizenCalendarEnv
import fi.espoo.evaka.DatabaseEnv
import fi.espoo.evaka.DvvModificationsEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.JamixEnv
import fi.espoo.evaka.JwtEnv
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.VtjEnv
import fi.espoo.evaka.VtjXroadEnv
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.job.ScheduledJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment

@Configuration
@Lazy
class EnvConfig {
    @Bean fun evakaEnv(env: Environment) = EvakaEnv.fromEnvironment(env)

    @Bean
    fun koskiEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): KoskiEnv? =
        when (evakaEnv.koskiEnabled) {
            true -> KoskiEnv.fromEnvironment(env)
            false -> null
        }

    @Bean fun bucketEnv(env: Environment): BucketEnv = BucketEnv.fromEnvironment(env)

    @Bean fun emailEnv(env: Environment): EmailEnv = EmailEnv.fromEnvironment(env)

    @Bean fun vardaEnv(env: Environment): VardaEnv = VardaEnv.fromEnvironment(env)

    @Bean fun databaseEnv(env: Environment): DatabaseEnv = DatabaseEnv.fromEnvironment(env)

    @Bean
    fun dvvModificationsEnv(env: Environment): DvvModificationsEnv = DvvModificationsEnv.fromEnvironment(env)

    @Bean fun jwtEnv(env: Environment): JwtEnv = JwtEnv.fromEnvironment(env)

    @Bean
    fun scheduledJobsEnv(env: Environment): ScheduledJobsEnv<ScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            ScheduledJob.values().associateWith { it.defaultSettings },
            "evaka.job",
            env
        )

    @Bean fun vtjEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): VtjEnv = VtjEnv.fromEnvironment(env)

    @Bean
    fun vtjXroadEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): VtjXroadEnv = VtjXroadEnv.fromEnvironment(env)

    @Bean fun ophEnv(env: Environment): OphEnv? = OphEnv.fromEnvironment(env)

    @Bean
    fun citizenCalendarEnv(env: Environment): CitizenCalendarEnv? = CitizenCalendarEnv.fromEnvironment(env)

    @Bean
    fun sfiEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): SfiEnv? =
        when (evakaEnv.sfiEnabled) {
            true -> SfiEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun webPushEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): WebPushEnv? =
        when (evakaEnv.webPushEnabled) {
            true -> WebPushEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun jamixEnv(
        evakaEnv: EvakaEnv,
        env: Environment
    ): JamixEnv? =
        when (evakaEnv.jamixEnabled) {
            true -> JamixEnv.fromEnvironment(env)
            false -> null
        }
}
