// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.AromiEnv
import evaka.core.BucketEnv
import evaka.core.ChildDocumentArchivalEnv
import evaka.core.CitizenCalendarEnv
import evaka.core.DatabaseEnv
import evaka.core.DvvModificationsEnv
import evaka.core.EmailEnv
import evaka.core.EvakaEnv
import evaka.core.JamixEnv
import evaka.core.JwtEnv
import evaka.core.KoskiEnv
import evaka.core.NekkuEnv
import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.SfiEnv
import evaka.core.VardaEnv
import evaka.core.VtjEnv
import evaka.core.VtjXroadEnv
import evaka.core.WebPushEnv
import evaka.core.shared.job.ScheduledJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment

@Configuration
@Lazy
class EnvConfig {
    @Bean fun evakaEnv(env: Environment) = EvakaEnv.fromEnvironment(env)

    @Bean
    fun koskiEnv(evakaEnv: EvakaEnv, env: Environment): KoskiEnv? =
        when (evakaEnv.koskiEnabled) {
            true -> KoskiEnv.fromEnvironment(env)
            false -> null
        }

    @Bean fun bucketEnv(env: Environment): BucketEnv = BucketEnv.fromEnvironment(env)

    @Bean fun emailEnv(env: Environment): EmailEnv = EmailEnv.fromEnvironment(env)

    @Bean fun vardaEnv(env: Environment): VardaEnv = VardaEnv.fromEnvironment(env)

    @Bean fun databaseEnv(env: Environment): DatabaseEnv = DatabaseEnv.fromEnvironment(env)

    @Bean
    fun dvvModificationsEnv(env: Environment): DvvModificationsEnv =
        DvvModificationsEnv.fromEnvironment(env)

    @Bean fun jwtEnv(env: Environment): JwtEnv = JwtEnv.fromEnvironment(env)

    @Bean
    fun scheduledJobsEnv(env: Environment): ScheduledJobsEnv<ScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            ScheduledJob.entries.associateWith { it.defaultSettings },
            "evaka.job",
            env,
        )

    @Bean fun vtjEnv(evakaEnv: EvakaEnv, env: Environment): VtjEnv = VtjEnv.fromEnvironment(env)

    @Bean
    fun vtjXroadEnv(evakaEnv: EvakaEnv, env: Environment): VtjXroadEnv =
        VtjXroadEnv.fromEnvironment(env)

    @Bean fun ophEnv(env: Environment): OphEnv? = OphEnv.fromEnvironment(env)

    @Bean
    fun citizenCalendarEnv(env: Environment): CitizenCalendarEnv? =
        CitizenCalendarEnv.fromEnvironment(env)

    @Bean
    fun sfiEnv(evakaEnv: EvakaEnv, env: Environment): SfiEnv? =
        when (evakaEnv.sfiEnabled) {
            true -> SfiEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun webPushEnv(evakaEnv: EvakaEnv, env: Environment): WebPushEnv? =
        when (evakaEnv.webPushEnabled) {
            true -> WebPushEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun jamixEnv(evakaEnv: EvakaEnv, env: Environment): JamixEnv? =
        when (evakaEnv.jamixEnabled) {
            true -> JamixEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun aromiEnv(evakaEnv: EvakaEnv, env: Environment): AromiEnv? =
        when (evakaEnv.aromiEnabled) {
            true -> AromiEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun nekkuEnv(evakaEnv: EvakaEnv, env: Environment): NekkuEnv? =
        when (evakaEnv.nekkuEnabled) {
            true -> NekkuEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun childDocumentArchivalEnv(env: Environment): ChildDocumentArchivalEnv =
        ChildDocumentArchivalEnv.fromEnvironment(env)
}
