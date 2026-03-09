// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.dev

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.turku.database.resetTurkuDatabaseForE2ETests
import java.time.Duration
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api/turku")
@ExcludeCodeGen
class TurkuDevApi(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    @GetMapping fun healthCheck(): ResponseEntity<Unit> = ResponseEntity.noContent().build()

    @PostMapping("/reset-turku-db-for-e2e-tests")
    fun resetTurkuDatabaseForE2ETests(db: Database, clock: EvakaClock): ResponseEntity<Unit> {
        // Run async jobs before database reset to avoid database locks/deadlocks
        asyncJobRunner.runPendingJobsSync(clock)
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))

        db.connect { c -> c.transaction { tx -> tx.resetTurkuDatabaseForE2ETests() } }
        return ResponseEntity.noContent().build()
    }
}
