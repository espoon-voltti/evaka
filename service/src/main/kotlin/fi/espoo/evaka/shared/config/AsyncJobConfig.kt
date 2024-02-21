// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import io.micrometer.core.instrument.MeterRegistry
import io.opentracing.Tracer
import java.time.Duration
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

// this throttle interval is pessimistic, because it assumes the actual e-mail job is completed
// instantaneously
private fun emailThrottleInterval(maxEmailsPerSecondRate: Int, numServiceInstances: Int) =
    Duration.ofSeconds(1)
        .dividedBy(maxEmailsPerSecondRate.toLong())
        .multipliedBy(numServiceInstances.toLong())

@Configuration
class AsyncJobConfig {
    @Bean
    fun asyncJobRunner(jdbi: Jdbi, tracer: Tracer, env: Environment): AsyncJobRunner<AsyncJob> =
        AsyncJobRunner(
            AsyncJob::class,
            listOf(
                AsyncJob.main,
                // these are reasonable defaults but should probably be configurable
                AsyncJob.email.withThrottleInterval(
                    emailThrottleInterval(maxEmailsPerSecondRate = 14, numServiceInstances = 2)
                ),
                AsyncJob.urgent,
                AsyncJob.varda,
                AsyncJob.suomiFi.withThrottleInterval(
                    Duration.ofSeconds(1).takeIf { env.activeProfiles.contains("production") }
                )
            ),
            jdbi,
            tracer
        )

    @Bean
    fun asyncJobRunnerStarter(
        asyncJobRunners: List<AsyncJobRunner<*>>,
        evakaEnv: EvakaEnv,
        meterRegistry: MeterRegistry
    ) =
        ApplicationListener<ApplicationReadyEvent> {
            val logger = KotlinLogging.logger {}
            if (evakaEnv.asyncJobRunnerDisabled) {
                logger.info("Async job runners disabled")
            } else {
                asyncJobRunners.forEach {
                    it.registerMeters(meterRegistry)
                    it.enableAfterCommitHooks()
                    it.startBackgroundPolling()
                    logger.info("Async job runner ${it.name} started")
                }
            }
        }
}
