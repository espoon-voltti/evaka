// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class AsyncJobConfig {
    @Bean
    fun asyncJobRunner(jdbi: Jdbi): AsyncJobRunner<AsyncJob> = AsyncJobRunner(jdbi)

    @Bean
    fun asyncJobRunnerStarter(asyncJobRunner: AsyncJobRunner<AsyncJob>, evakaEnv: EvakaEnv) =
        ApplicationListener<ApplicationReadyEvent> {
            val logger = KotlinLogging.logger { }
            if (evakaEnv.asyncJobRunnerDisabled) {
                logger.info("Async job runner disabled")
            } else {
                asyncJobRunner.start(Duration.ofMinutes(1))
                logger.info("Async job runner started")
            }
        }
}
