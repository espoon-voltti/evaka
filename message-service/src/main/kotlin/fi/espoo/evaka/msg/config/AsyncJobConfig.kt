// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.async.AsyncJobRunner
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import java.time.Duration

@Configuration
class AsyncJobConfig {
    @Bean
    fun asyncJobRunner(jdbi: Jdbi) = AsyncJobRunner(jdbi)

    @Bean
    fun asyncJobRunnerSchedule(asyncJobRunner: AsyncJobRunner) = object {
        @EventListener
        fun onApplicationReady(@Suppress("UNUSED_PARAMETER") event: ApplicationReadyEvent) {
            asyncJobRunner.schedulePeriodicRun(Duration.ofMinutes(1))
        }
    }
}
