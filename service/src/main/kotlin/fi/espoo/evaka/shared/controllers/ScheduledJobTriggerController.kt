// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.job.ScheduledJob
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledJobTriggerController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    @GetMapping(produces = ["text/html"])
    fun form(user: AuthenticatedUser, clock: EvakaClock): String {
        accessControl.requirePermissionFor(user, clock, Action.Global.TRIGGER_SCHEDULED_JOBS)

        // language=html
        return """
<!DOCTYPE html>
<html>
    <body>
        <form id="form" method='get'>
            ${ScheduledJob.values().sorted().joinToString(separator = "\n") { jobRadio(it) }}
            <input type='submit' value='Trigger job'>
            <p id='status'></p>
        </form>
        <script>
            const [_, csrf] = document.cookie.split(';')
                .map(c => c.trim().split('=').map(decodeURIComponent))
                .find(([name, ]) => name === 'evaka.employee.xsrf')
            const status = document.getElementById('status')
            document.forms['form'].addEventListener('submit', (e) => {
                e.preventDefault()
                const data = new FormData(e.currentTarget)
                status.innerText = ''
                fetch(location.pathname, {
                    method: 'POST',
                    cache: 'no-cache',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': csrf
                    },
                    body: JSON.stringify(Object.fromEntries(data.entries()))
                })
                    .then(r => status.innerText = r.statusText)
                    .catch(e => status.innerText = e.message)
            })
        </script>
    </body>
</html>
        """.trimIndent(
        )
    }

    @PostMapping
    fun trigger(
        user: AuthenticatedUser,
        db: Database,
        clock: EvakaClock,
        @RequestBody body: TriggerBody
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Global.TRIGGER_SCHEDULED_JOBS)

        db.connect { dbc ->
            dbc.transaction { tx ->
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.RunScheduledJob(body.type)),
                    retryCount = 1,
                    runAt = clock.now()
                )
            }
        }
    }

    data class TriggerBody(val type: ScheduledJob)
}

private fun jobRadio(job: ScheduledJob): String {
    val name = job.name
    // language=html
    return """
<div>
    <input type='radio' id='$name' name='type' value='$name'>
    <label for='$name'>$name</label>
</div>
    """.trimIndent(
    )
}
