// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.FiniteDateRange

class MockLinkityClient : LinkityClient {
    private val shifts = mutableListOf<Shift>()

    override fun getShifts(period: FiniteDateRange): List<Shift> {
        return shifts.filter {
            it.startDateTime.toLocalDate() <= period.end &&
                it.endDateTime.toLocalDate() >= period.start
        }
    }

    fun setupMockShifts(shifts: List<Shift>) {
        this.shifts.clear()
        this.shifts.addAll(shifts)
    }
}
