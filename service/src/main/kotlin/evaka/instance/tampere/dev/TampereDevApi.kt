// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.dev

import evaka.core.ExcludeCodeGen
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.instance.tampere.database.resetTampereDatabaseForE2ETests
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import java.lang.System.lineSeparator
import java.time.Duration
import java.util.stream.Collectors.joining
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api/tampere")
@ExcludeCodeGen
class TampereDevApi(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    @GetMapping fun healthCheck(): ResponseEntity<Unit> = ResponseEntity.noContent().build()

    @PostMapping("/reset-tampere-db-for-e2e-tests")
    fun resetTampereDatabaseForE2ETests(
        db: Database,
        clock: EvakaClock = RealEvakaClock(),
    ): ResponseEntity<Unit> {
        // Run async jobs before database reset to avoid database locks/deadlocks
        asyncJobRunner.runPendingJobsSync(clock)
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))

        db.connect { c -> c.transaction { tx -> tx.resetTampereDatabaseForE2ETests() } }
        MockPersonDetailsService.reset()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/salesOrder")
    fun salesOrder(request: HttpServletRequest) {
        logger.info { request.reader.lines().collect(joining(lineSeparator())) }
    }

    @PostMapping("/payableAccounting")
    fun payableAccounting(request: HttpServletRequest) {
        logger.info { request.reader.lines().collect(joining(lineSeparator())) }
    }

    @PostMapping("/archival/records/add")
    fun archive(request: HttpServletRequest) {
        logger.info {
            request.headerNames.asSequence().joinToString { "$it:${request.getHeader(it)}" }
        }
    }
}
