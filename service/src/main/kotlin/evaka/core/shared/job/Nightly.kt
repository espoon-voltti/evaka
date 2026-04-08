// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.Daily
import evaka.core.shared.domain.europeHelsinki
import java.time.LocalTime

class Nightly : Daily(europeHelsinki, runAt) {
    companion object {
        private var runAt: LocalTime = LocalTime.of(0, 10)

        fun configureNightlyTime(time: LocalTime) {
            runAt = time
        }
    }
}
