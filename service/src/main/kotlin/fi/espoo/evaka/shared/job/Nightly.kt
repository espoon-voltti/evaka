// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.Daily
import fi.espoo.evaka.shared.domain.europeHelsinki
import java.time.LocalTime

class Nightly() : Daily(europeHelsinki, runAt) {
    companion object {
        private var runAt: LocalTime = LocalTime.of(0, 10)

        fun configureNightlyTime(time: LocalTime) {
            runAt = time
        }
    }
}
