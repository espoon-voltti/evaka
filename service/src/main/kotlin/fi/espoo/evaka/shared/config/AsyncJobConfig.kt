// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobRunnerConfig
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.async.VardaAsyncJob
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.time.Duration

@Configuration
class AsyncJobConfig {
    @Bean
    fun asyncJobRunner(jdbi: Jdbi): AsyncJobRunner<AsyncJob> = AsyncJobRunner(AsyncJob::class, jdbi, AsyncJobRunnerConfig())

    @Bean
    fun vardaAsyncJobRunner(jdbi: Jdbi): AsyncJobRunner<VardaAsyncJob> = AsyncJobRunner(VardaAsyncJob::class, jdbi, AsyncJobRunnerConfig(threadPoolSize = 1))

    @Bean
    fun sfiAsyncJobRunner(jdbi: Jdbi, env: Environment): AsyncJobRunner<SuomiFiAsyncJob> = AsyncJobRunner(
        SuomiFiAsyncJob::class,
        jdbi,
        AsyncJobRunnerConfig(
            threadPoolSize = 1,
            throttleInterval = Duration.ofSeconds(1).takeIf { env.activeProfiles.contains("production") }
        )
    )

    @Bean
    fun asyncJobRunnerStarter(asyncJobRunners: List<AsyncJobRunner<*>>, evakaEnv: EvakaEnv) =
        ApplicationListener<ApplicationReadyEvent> {
            val logger = KotlinLogging.logger { }
            if (evakaEnv.asyncJobRunnerDisabled) {
                logger.info("Async job runners disabled")
            } else {
                asyncJobRunners.forEach {
                    it.start(pollingInterval = Duration.ofMinutes(1))
                    logger.info("Async job runner AsyncJobRunner<${it.payloadType.simpleName}> started")
                }
            }
        }
}
