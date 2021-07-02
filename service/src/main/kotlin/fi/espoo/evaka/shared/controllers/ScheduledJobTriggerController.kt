// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.RunScheduledJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.job.ScheduledJob
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledJobTriggerController(private val asyncJobRunner: AsyncJobRunner) {
    @GetMapping(produces = ["text/html"])
    fun form(user: AuthenticatedUser): String {
        user.requireOneOfRoles(UserRole.ADMIN)

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
        """.trimIndent()
    }

    @PostMapping
    fun trigger(user: AuthenticatedUser, db: Database.Connection, @RequestBody body: TriggerBody) {
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx ->
            asyncJobRunner.plan(tx, listOf(RunScheduledJob(body.type)), retryCount = 1)
        }
        asyncJobRunner.scheduleImmediateRun()
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
    """.trimIndent()
}
